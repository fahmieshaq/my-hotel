
resource "aws_lb_target_group" "hotel_api_environment" {
  for_each = local.hotel_api_environments

  name        = "hotel-api-${each.key}-tg"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    enabled             = true
    path                = "/actuator/health/readiness"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id

  description = "Allow public HTTPS requests"
  ip_protocol = "tcp"
  from_port   = 443
  to_port     = 443
  cidr_ipv4   = "0.0.0.0/0"
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "HTTPS"

  ssl_policy      = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn = aws_acm_certificate.hotel_api.arn

  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "hotel_api_environment" {
  for_each = local.hotel_api_environments

  listener_arn = aws_lb_listener.https.arn

  priority = each.key == "prod" ? 100 : 110

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.hotel_api_environment[each.key].arn
  }

  condition {
    host_header {
      values = [each.value.domain]
    }
  }
}
