# ------------------------------------------------------------
# CloudWatch Outputs - Hotel API
# ------------------------------------------------------------

output "hotel_api_log_group_name" {
  description = "CloudWatch log group name for Hotel API"
  value       = aws_cloudwatch_log_group.app.name
}

output "hotel_api_log_group_arn" {
  description = "CloudWatch log group ARN for Hotel API"
  value       = aws_cloudwatch_log_group.app.arn
}
