# ------------------------------------------------------------
# CloudWatch Logs - Hotel API
# ------------------------------------------------------------
#
# Creates a dedicated CloudWatch log group for Hotel API.
#
# Each startup (Hotel API, HOORUZ, Xetoor) will manage
# its own CloudWatch log group in its own Terraform project.
#
# Spring Boot -> SLF4J/Logback -> Container stdout/stderr
# -> ECS awslogs driver -> CloudWatch Logs
#
# ------------------------------------------------------------

resource "aws_cloudwatch_log_group" "app" {

  # Example: /ecs/hotel-api
  name = "/ecs/${var.application_name}"

  # Automatically delete logs older than 14 days
  # to help control storage costs.
  retention_in_days = 14

  # AWS encrypts CloudWatch Logs at rest by default.

  tags = {
    Name        = "${var.application_name}-logs"
    Application = var.application_name
    ManagedBy   = "Terraform"
  }
}
