
output "hotel_api_environment_services" {
  value = {
    for environment, service in aws_ecs_service.hotel_api_environment :
    environment => service.name
  }
}

output "hotel_api_environment_domains" {
  value = {
    for environment, config in local.hotel_api_environments :
    environment => "https://${config.domain}"
  }
}

output "hotel_api_environment_target_groups" {
  value = {
    for environment, target_group in aws_lb_target_group.hotel_api_environment :
    environment => target_group.arn
  }
}
