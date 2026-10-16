# ------------------------------------------------------------
# ECS Task Definition - Hotel API
# ------------------------------------------------------------
#
# Defines how AWS Fargate runs the Hotel API container.
#
# This resource does not start the application.
# An ECS Service will launch tasks from this definition.
#
# ------------------------------------------------------------

resource "aws_ecs_task_definition" "hotel_api" {

  # Name of the task definition family
  family = var.application_name

  # Use AWS Fargate instead of managing EC2 servers
  requires_compatibilities = ["FARGATE"]

  # Each Fargate task gets its own network interface
  network_mode = "awsvpc"

  # 512 CPU units = 0.5 vCPU
  cpu = "512"

  # 1024 MiB = 1 GiB RAM
  memory = "1024"

  # AWS permissions for pulling images and sending logs
  execution_role_arn = aws_iam_role.ecs_task_execution.arn

  # AWS permissions available to Spring Boot application code
  task_role_arn = aws_iam_role.hotel_api_task.arn

  # Linux container on x86-64 architecture
  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  # ----------------------------------------------------------
  # Container configuration
  # ----------------------------------------------------------

  container_definitions = jsonencode([
    {
      name = var.application_name

      # Pin to the exact Docker image already published to ECR.
      # The digest identifies immutable image content.
      image = "${aws_ecr_repository.app.repository_url}@sha256:836b681b17a20b1bc9852f6836607b69fee5dc869036deb1e573694986ca9704"

      essential = true

      # Spring Boot listens on port 8080
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]

      # ------------------------------------------------------
      # CloudWatch logging
      # ------------------------------------------------------

      logConfiguration = {
        logDriver = "awslogs"

        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = {
    Name        = "${var.application_name}-task"
    Application = var.application_name
    ManagedBy   = "Terraform"
  }
}
