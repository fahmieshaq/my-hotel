
output "ecs_task_execution_role_arn" {
  description = "IAM execution role used by ECS Fargate"
  value       = aws_iam_role.ecs_task_execution.arn
}

output "hotel_api_task_role_arn" {
  description = "IAM role used by the Hotel API application"
  value       = aws_iam_role.hotel_api_task.arn
}
