# ------------------------------------------------------------
# ECS Task Definition Outputs
# ------------------------------------------------------------

output "hotel_api_task_definition_arn" {
  description = "ARN of the Hotel API ECS task definition"
  value       = aws_ecs_task_definition.hotel_api.arn
}

output "hotel_api_task_definition_family" {
  description = "Family name of the Hotel API task definition"
  value       = aws_ecs_task_definition.hotel_api.family
}

output "hotel_api_task_definition_revision" {
  description = "Revision number of the Hotel API task definition"
  value       = aws_ecs_task_definition.hotel_api.revision
}
