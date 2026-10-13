resource "aws_ecr_repository" "app" {
  name                 = var.application_name
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Name        = var.application_name
    Application = var.application_name
    ManagedBy   = "Terraform"
  }
}