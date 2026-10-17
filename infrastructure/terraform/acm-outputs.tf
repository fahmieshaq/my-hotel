# ------------------------------------------------------------
# ACM Outputs
# ------------------------------------------------------------

output "hotel_api_certificate_arn" {
  description = "ACM certificate ARN for Hotel API"
  value       = aws_acm_certificate.hotel_api.arn
}

output "hotel_api_certificate_validation_records" {
  description = "DNS CNAME records to create in Namecheap"

  value = [
    for record in aws_acm_certificate.hotel_api.domain_validation_options : {
      domain = record.domain_name
      name   = record.resource_record_name
      type   = record.resource_record_type
      value  = record.resource_record_value
    }
  ]
}
