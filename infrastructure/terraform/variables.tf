variable "aws_region" {
  description = "AWS region where infrastructure is deployed."
  type        = string
}

variable "application_name" {
  description = "Name of the backend application whose AWS resources are being provisioned."
  type        = string
  default     = "hotel-api"
}
