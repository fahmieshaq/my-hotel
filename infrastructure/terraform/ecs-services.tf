
resource "aws_ecs_service" "hotel_api_environment" {
  for_each = local.hotel_api_environments

  name            = "hotel-api-${each.key}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.hotel_api_environment[each.key].arn

  launch_type      = "FARGATE"
  platform_version = "LATEST"

  desired_count = 1

  health_check_grace_period_seconds = 120

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets = [
      aws_subnet.public_a.id,
      aws_subnet.public_b.id
    ]

    security_groups = [
      aws_security_group.ecs.id
    ]

    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.hotel_api_environment[each.key].arn
    container_name   = "hotel-api"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener_rule.hotel_api_environment,
    aws_iam_role_policy_attachment.hotel_api_environment_execution,
    aws_iam_role_policy.hotel_api_environment_secrets,
    aws_vpc_security_group_egress_rule.ecs_postgresql
  ]

  lifecycle {
    ignore_changes = [
      task_definition
    ]
  }
}
