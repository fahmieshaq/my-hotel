
# ------------------------------------------------------------
# Shared ECS Cluster
# ------------------------------------------------------------

resource "aws_ecs_cluster" "main" {
  name = "shared-apps-cluster"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }

  tags = {
    Name = "shared-apps-cluster"
  }
}
