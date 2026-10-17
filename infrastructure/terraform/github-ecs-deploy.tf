locals {
  github_repository = "fahmieshaq/my-hotel"

  github_deployment_roles = {
    staging = {
      environment = "staging"
      service     = "hotel-api-staging"
    }
    prod = {
      environment = "production"
      service     = "hotel-api-prod"
    }
  }
}

data "aws_iam_policy_document" "github_deployment_trust" {
  for_each = local.github_deployment_roles

  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type = "Federated"
      identifiers = [
        aws_iam_openid_connect_provider.github.arn
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:${local.github_repository}:environment:${each.value.environment}"
      ]
    }
  }
}

resource "aws_iam_role" "github_deployer" {
  for_each = local.github_deployment_roles

  name = each.key == "prod" ? (
    "hotel-api-github-production-deployer"
    ) : (
    "hotel-api-github-staging-deployer"
  )

  assume_role_policy = data.aws_iam_policy_document.github_deployment_trust[
    each.key
  ].json
}

resource "aws_iam_role_policy" "github_deployer" {
  for_each = local.github_deployment_roles

  name = "hotel-api-ecs-deploy"
  role = aws_iam_role.github_deployer[each.key].id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Sid      = "DescribeTaskDefinitions"
        Effect   = "Allow"
        Action   = ["ecs:DescribeTaskDefinition"]
        Resource = "*"
      },
      {
        Sid    = "RegisterEnvironmentTaskDefinitions"
        Effect = "Allow"
        Action = ["ecs:RegisterTaskDefinition"]
        Resource = [
          "arn:aws:ecs:eu-west-2:343482277554:task-definition/hotel-api-${each.key}:*"
        ]
      },
      {
        Sid    = "ManageEnvironmentService"
        Effect = "Allow"
        Action = [
          "ecs:DescribeServices",
          "ecs:UpdateService"
        ]
        Resource = [
          "arn:aws:ecs:eu-west-2:343482277554:service/shared-apps-cluster/${each.value.service}"
        ]
      },
      {
        Sid    = "PassEnvironmentTaskRoles"
        Effect = "Allow"
        Action = ["iam:PassRole"]

        Resource = [
          aws_iam_role.hotel_api_task.arn,
          aws_iam_role.hotel_api_environment_execution[
            each.key
          ].arn
        ]

        Condition = {
          StringEquals = {
            "iam:PassedToService" = "ecs-tasks.amazonaws.com"
          }
        }
      }
    ]
  })
}
