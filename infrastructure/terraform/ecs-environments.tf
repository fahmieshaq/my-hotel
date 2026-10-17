locals {
  hotel_api_image = "${aws_ecr_repository.app.repository_url}@sha256:836b681b17a20b1bc9852f6836607b69fee5dc869036deb1e573694986ca9704"

  hotel_api_environments = {
    staging = {
      domain     = "staging-api.asmreditor.com"
      secret_arn = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/staging/database-Qqo30z"
    }

    prod = {
      domain     = "api.asmreditor.com"
      secret_arn = "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/prod/database-0yLA5U"
    }
  }
}

resource "aws_cloudwatch_log_group" "hotel_api_environment" {
  for_each = local.hotel_api_environments

  name              = "/ecs/hotel-api-${each.key}"
  retention_in_days = 14
}

resource "aws_iam_role" "hotel_api_environment_execution" {
  for_each = local.hotel_api_environments

  name = "hotel-api-${each.key}-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sts:AssumeRole"
      Condition = {
        StringEquals = {
          "aws:SourceAccount" = "343482277554"
        }
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:eu-west-2:343482277554:*"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "hotel_api_environment_execution" {
  for_each = local.hotel_api_environments

  role       = aws_iam_role.hotel_api_environment_execution[each.key].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "hotel_api_environment_secrets" {
  for_each = local.hotel_api_environments

  name = "read-database-secret"
  role = aws_iam_role.hotel_api_environment_execution[each.key].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = each.value.secret_arn
    }]
  })
}

resource "aws_ecs_task_definition" "hotel_api_environment" {
  for_each = local.hotel_api_environments

  family                   = "hotel-api-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"

  execution_role_arn = aws_iam_role.hotel_api_environment_execution[each.key].arn
  task_role_arn      = aws_iam_role.hotel_api_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = "hotel-api"
    image     = local.hotel_api_image
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    secrets = [
      {
        name      = "SPRING_DATASOURCE_URL"
        valueFrom = "${each.value.secret_arn}:SPRING_DATASOURCE_URL::"
      },
      {
        name      = "SPRING_DATASOURCE_USERNAME"
        valueFrom = "${each.value.secret_arn}:SPRING_DATASOURCE_USERNAME::"
      },
      {
        name      = "SPRING_DATASOURCE_PASSWORD"
        valueFrom = "${each.value.secret_arn}:SPRING_DATASOURCE_PASSWORD::"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.hotel_api_environment[each.key].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])
}
