output "ecr_repository_url" {
  description = "ECR repository URL for Docker image publishing."
  value       = aws_ecr_repository.app.repository_url
}

output "ecr_repository_arn" {
  description = "ECR repository ARN."
  value       = aws_ecr_repository.app.arn
}

output "github_ecr_publisher_role_arn" {
  description = "IAM role assumed by GitHub Actions to publish Hotel API images"
  value       = aws_iam_role.github_ecr_publisher.arn
}