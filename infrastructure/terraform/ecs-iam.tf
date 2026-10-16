
# ------------------------------------------------------------
# Trust policy: allow ECS tasks to assume these roles
# ------------------------------------------------------------

data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    effect = "Allow"

    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type = "Service"

      identifiers = [
        "ecs-tasks.amazonaws.com"
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"

      values = [
        data.aws_caller_identity.current.account_id
      ]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"

      values = [
        "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"
      ]
    }
  }
}

data "aws_caller_identity" "current" {}

# ------------------------------------------------------------
# ECS Task Execution Role
# Used by ECS to pull images and publish logs
# ------------------------------------------------------------

resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.application_name}-ecs-task-execution"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name = "${var.application_name}-ecs-task-execution"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role = aws_iam_role.ecs_task_execution.name

  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ------------------------------------------------------------
# ECS Task Role
# Used by the Spring Boot application itself
# ------------------------------------------------------------

resource "aws_iam_role" "hotel_api_task" {
  name = "${var.application_name}-ecs-task"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name = "${var.application_name}-ecs-task"
  }
}
