resource "aws_vpc_security_group_egress_rule" "ecs_postgresql" {
  security_group_id = aws_security_group.ecs.id

  description = "Allow ECS tasks to connect to Neon PostgreSQL"
  ip_protocol = "tcp"
  from_port   = 5432
  to_port     = 5432
  cidr_ipv4   = "0.0.0.0/0"
}
