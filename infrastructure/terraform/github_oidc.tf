# Allow AWS to verify identity tokens issued by GitHub Actions.
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com"
  ]

  tags = {
    Name      = "github-actions-oidc"
    ManagedBy = "Terraform"
  }
}

# Allow only this repository's main branch to assume the role.
data "aws_iam_policy_document" "github_ecr_trust" {
  statement {
    effect = "Allow"

    actions = [
      "sts:AssumeRoleWithWebIdentity"
    ]

    principals {
      type = "Federated"

      identifiers = [
        aws_iam_openid_connect_provider.github.arn
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"

      values = [
        "sts.amazonaws.com"
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"

      values = [
        "repo:fahmieshaq@22756125/my-hotel@1411763878:ref:refs/heads/main"
      ]
    }
  }
}

# IAM role assumed by GitHub Actions.
resource "aws_iam_role" "github_ecr_publisher" {
  name = "hotel-api-github-ecr-publisher"

  assume_role_policy = data.aws_iam_policy_document.github_ecr_trust.json

  tags = {
    Application = var.application_name
    ManagedBy   = "Terraform"
  }
}

# Permissions required to publish Docker images to ECR.
data "aws_iam_policy_document" "github_ecr_publish" {
  statement {
    sid = "GetECRAuthorizationToken"

    effect = "Allow"

    actions = [
      "ecr:GetAuthorizationToken"
    ]

    resources = ["*"]
  }

  statement {
    sid = "PublishHotelAPIImages"

    effect = "Allow"

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart"
    ]

    resources = [
      aws_ecr_repository.app.arn
    ]
  }
}

resource "aws_iam_role_policy" "github_ecr_publish" {
  name = "hotel-api-ecr-publish"
  role = aws_iam_role.github_ecr_publisher.id

  policy = data.aws_iam_policy_document.github_ecr_publish.json
}
