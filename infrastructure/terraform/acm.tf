# ------------------------------------------------------------
# AWS Certificate Manager
# HTTPS certificate for Hotel API staging and production
# ------------------------------------------------------------

resource "aws_acm_certificate" "hotel_api" {
  domain_name = "api.asmreditor.com"

  subject_alternative_names = [
    "staging-api.asmreditor.com"
  ]

  validation_method = "DNS"

  tags = {
    Name        = "hotel-api-https"
    Application = var.application_name
    ManagedBy   = "Terraform"
  }

  lifecycle {
    create_before_destroy = true
  }
}
