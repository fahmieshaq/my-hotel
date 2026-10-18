resource "aws_ecr_repository" "database_restore" {
  name                 = "hotel-api-database-restore"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_iam_role" "database_restore_execution" {
  name = "hotel-api-database-restore-execution"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

resource "aws_iam_role_policy_attachment" "database_restore_execution" {
  role       = aws_iam_role.database_restore_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "database_restore_secrets" {
  name = "read-restore-test-database-secret"
  role = aws_iam_role.database_restore_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["secretsmanager:GetSecretValue"]
        Resource = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/restore-test/database-lUXSvl"
      }
    ]
  })
}

resource "aws_iam_role" "database_restore_task" {
  name = "hotel-api-database-restore-task"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json
}

resource "aws_iam_role_policy" "database_restore_s3" {
  name = "read-database-backups"
  role = aws_iam_role.database_restore_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject"
        ]
        Resource = "${aws_s3_bucket.database_backups.arn}/daily/*"
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "database_restore" {
  name              = "/ecs/hotel-api-database-restore"
  retention_in_days = 14
}

resource "aws_ecs_task_definition" "database_restore" {
  family                   = "hotel-api-database-restore"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"

  execution_role_arn = aws_iam_role.database_restore_execution.arn
  task_role_arn      = aws_iam_role.database_restore_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "database-restore"
      image     = "${aws_ecr_repository.database_restore.repository_url}:v1"
      essential = true

      environment = [
        {
          name  = "AWS_REGION"
          value = "eu-west-2"
        },
        {
          name  = "BACKUP_BUCKET"
          value = aws_s3_bucket.database_backups.bucket
        },
        {
          name  = "BACKUP_KEY"
          value = "daily/hotel-api-prod-2026-10-18T04-54-37Z.dump"
        }
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_URL"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/restore-test/database-lUXSvl:SPRING_DATASOURCE_URL::"
        },
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/restore-test/database-lUXSvl:SPRING_DATASOURCE_USERNAME::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/restore-test/database-lUXSvl:SPRING_DATASOURCE_PASSWORD::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.database_restore.name
          "awslogs-region"        = "eu-west-2"
          "awslogs-stream-prefix" = "restore"
        }
      }
    }
  ])
}