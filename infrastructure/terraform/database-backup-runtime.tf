
# ============================================================
# ECR - Database Backup Image
# ============================================================

resource "aws_ecr_repository" "database_backup" {
  name                 = "hotel-api-database-backup"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "hotel-api-database-backup"
  }
}

# ============================================================
# CloudWatch Logs
# ============================================================

resource "aws_cloudwatch_log_group" "database_backup" {
  name              = "/ecs/hotel-api-database-backup"
  retention_in_days = 14
}

# ============================================================
# Security Group - Outbound Only
# ============================================================

resource "aws_security_group" "database_backup" {
  name        = "hotel-api-database-backup"
  description = "Outbound-only access for database backups"
  vpc_id      = aws_vpc.main.id

  tags = {
    Name = "hotel-api-database-backup"
  }
}

resource "aws_vpc_security_group_egress_rule" "database_backup_postgres" {
  security_group_id = aws_security_group.database_backup.id

  ip_protocol = "tcp"
  from_port   = 5432
  to_port     = 5432

  cidr_ipv4   = "0.0.0.0/0"
  description = "Neon PostgreSQL"
}

resource "aws_vpc_security_group_egress_rule" "database_backup_https" {
  security_group_id = aws_security_group.database_backup.id

  ip_protocol = "tcp"
  from_port   = 443
  to_port     = 443

  cidr_ipv4   = "0.0.0.0/0"
  description = "AWS APIs and ECR"
}

# ============================================================
# ECS Fargate Task Definition
# ============================================================

resource "aws_ecs_task_definition" "database_backup" {
  family                   = "hotel-api-database-backup"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu    = "512"
  memory = "1024"

  execution_role_arn = aws_iam_role.database_backup_execution.arn
  task_role_arn      = aws_iam_role.database_backup_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "database-backup"
      image     = "${aws_ecr_repository.database_backup.repository_url}:v3"
      essential = true

      readonlyRootFilesystem = false

      environment = [
        {
          name  = "BACKUP_BUCKET"
          value = aws_s3_bucket.database_backups.bucket
        },
        {
          name  = "AWS_REGION"
          value = var.aws_region
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_URL"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/prod/database-0yLA5U:SPRING_DATASOURCE_URL::"
        },
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/prod/database-0yLA5U:SPRING_DATASOURCE_USERNAME::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/prod/database-0yLA5U:SPRING_DATASOURCE_PASSWORD::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          awslogs-group         = aws_cloudwatch_log_group.database_backup.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "backup"
        }
      }
    }
  ])
}

# ============================================================
# EventBridge Scheduler IAM Role
# ============================================================

data "aws_iam_policy_document" "database_backup_scheduler_trust" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["scheduler.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"

      values = [
        data.aws_caller_identity.current.account_id
      ]
    }
  }
}

resource "aws_iam_role" "database_backup_scheduler" {
  name = "hotel-api-database-backup-scheduler"

  assume_role_policy = data.aws_iam_policy_document.database_backup_scheduler_trust.json
}

resource "aws_iam_role_policy" "database_backup_scheduler" {
  name = "run-database-backup-task"
  role = aws_iam_role.database_backup_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        Resource = [aws_ecs_task_definition.database_backup.arn]

        Condition = {
          ArnEquals = {
            "ecs:cluster" = aws_ecs_cluster.main.arn
          }
        }
      },
      {
        Effect = "Allow"
        Action = ["iam:PassRole"]

        Resource = [
          aws_iam_role.database_backup_execution.arn,
          aws_iam_role.database_backup_task.arn
        ]

        Condition = {
          StringEquals = {
            "iam:PassedToService" = "ecs-tasks.amazonaws.com"
          }
        }
      }
    ]
  })
}

# ============================================================
# EventBridge - Daily Backup Schedule
# ============================================================

resource "aws_scheduler_schedule" "database_backup" {
  name = "hotel-api-production-database-backup-daily"

  schedule_expression          = "cron(0 2 * * ? *)"
  schedule_expression_timezone = "Etc/UTC"

  state = "ENABLED"

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = aws_ecs_cluster.main.arn
    role_arn = aws_iam_role.database_backup_scheduler.arn

    ecs_parameters {
      task_definition_arn = aws_ecs_task_definition.database_backup.arn

      launch_type      = "FARGATE"
      task_count       = 1
      platform_version = "LATEST"

      network_configuration {
        subnets = [
          aws_subnet.public_a.id,
          aws_subnet.public_b.id
        ]

        security_groups = [
          aws_security_group.database_backup.id
        ]

        assign_public_ip = true
      }
    }

    retry_policy {
      maximum_event_age_in_seconds = 3600
      maximum_retry_attempts       = 2
    }
  }
}

# ============================================================
# CloudWatch - Backup Failure Detection
# ============================================================

resource "aws_cloudwatch_log_metric_filter" "database_backup_failed" {
  name           = "hotel-api-database-backup-failed"
  pattern        = "BACKUP_FAILED"
  log_group_name = aws_cloudwatch_log_group.database_backup.name

  metric_transformation {
    name      = "DatabaseBackupFailureCount"
    namespace = "HotelAPI/Production"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "database_backup_failed" {
  alarm_name = "hotel-api-database-backup-failed"

  comparison_operator = "GreaterThanOrEqualToThreshold"

  evaluation_periods = 1
  period             = 300
  statistic          = "Sum"

  metric_name = "DatabaseBackupFailureCount"
  namespace   = "HotelAPI/Production"

  threshold          = 1
  treat_missing_data = "notBreaching"

  alarm_actions = [
    aws_sns_topic.hotel_api_alerts.arn
  ]
}
