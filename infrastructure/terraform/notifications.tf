# --------------------------------------------------
# Hotel API notifications
# --------------------------------------------------
variable "alert_email" {
  description = "Email address receiving Hotel API alerts"
  type        = string
  sensitive   = true
}

data "aws_caller_identity" "notifications" {}

data "aws_partition" "notifications" {}

resource "aws_sns_topic" "hotel_api_alerts" {
  name = "hotel-api-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.hotel_api_alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# --------------------------------------------------
# ECS deployment failures
# --------------------------------------------------
resource "aws_cloudwatch_event_rule" "ecs_deployment_failed" {
  name        = "hotel-api-deployment-failed"
  description = "Detect failed Hotel API ECS deployments"

  event_pattern = jsonencode({
    source        = ["aws.ecs"]
    "detail-type" = ["ECS Deployment State Change"]
    detail = {
      eventName  = ["SERVICE_DEPLOYMENT_FAILED"]
      clusterArn = [aws_ecs_cluster.main.arn]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_deployment_failed" {
  rule      = aws_cloudwatch_event_rule.ecs_deployment_failed.name
  target_id = "HotelApiDeploymentFailureEmail"
  arn       = aws_sns_topic.hotel_api_alerts.arn
}

# --------------------------------------------------
# ECS task failures (not routine scheduler stops)
# --------------------------------------------------
resource "aws_cloudwatch_event_rule" "ecs_tasks_stopped" {
  name        = "hotel-api-tasks-stopped"
  description = "Detect failed Hotel API ECS tasks"

  event_pattern = jsonencode({
    source        = ["aws.ecs"]
    "detail-type" = ["ECS Task State Change"]
    detail = {
      clusterArn = [aws_ecs_cluster.main.arn]
      lastStatus = ["STOPPED"]
      group = [
        "service:hotel-api-staging",
        "service:hotel-api-prod"
      ]
      stopCode = [
        "EssentialContainerExited",
        "TaskFailedToStart"
      ]
    }
  })
}

resource "aws_cloudwatch_event_target" "ecs_tasks_stopped" {
  rule      = aws_cloudwatch_event_rule.ecs_tasks_stopped.name
  target_id = "HotelApiStoppedTaskEmail"
  arn       = aws_sns_topic.hotel_api_alerts.arn
}

# --------------------------------------------------
# Production has no healthy ALB targets
# --------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "production_no_healthy_targets" {
  alarm_name          = "hotel-api-production-no-healthy-targets"
  alarm_description   = "Production ALB has no healthy Hotel API targets"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 2
  period              = 60
  statistic           = "Minimum"
  threshold           = 1

  namespace   = "AWS/ApplicationELB"
  metric_name = "HealthyHostCount"

  dimensions = {
    LoadBalancer = aws_lb.main.arn_suffix
    TargetGroup  = aws_lb_target_group.hotel_api_environment["prod"].arn_suffix
  }

  treat_missing_data = "breaching"
  alarm_actions      = [aws_sns_topic.hotel_api_alerts.arn]
  ok_actions         = [aws_sns_topic.hotel_api_alerts.arn]
}

# --------------------------------------------------
# SNS topic policy
# --------------------------------------------------
data "aws_iam_policy_document" "sns_combined" {
  # Preserve management permissions for the owning AWS account.
  statement {
    sid    = "AllowAccountOwnerManagement"
    effect = "Allow"
    principals {
      type        = "AWS"
      identifiers = ["arn:${data.aws_partition.notifications.partition}:iam::${data.aws_caller_identity.notifications.account_id}:root"]
    }
    actions = [
      "sns:GetTopicAttributes",
      "sns:SetTopicAttributes",
      "sns:AddPermission",
      "sns:RemovePermission",
      "sns:DeleteTopic",
      "sns:Subscribe",
      "sns:ListSubscriptionsByTopic",
      "sns:Publish"
    ]
    resources = [aws_sns_topic.hotel_api_alerts.arn]
  }

  statement {
    sid     = "AllowHotelApiEventBridgeRules"
    effect  = "Allow"
    actions = ["sns:Publish"]
    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com"]
    }
    resources = [aws_sns_topic.hotel_api_alerts.arn]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = [
        aws_cloudwatch_event_rule.ecs_deployment_failed.arn,
        aws_cloudwatch_event_rule.ecs_tasks_stopped.arn
      ]
    }
  }

  statement {
    sid     = "AllowHotelApiCloudWatchAlarm"
    effect  = "Allow"
    actions = ["sns:Publish"]
    principals {
      type        = "Service"
      identifiers = ["cloudwatch.amazonaws.com"]
    }
    resources = [aws_sns_topic.hotel_api_alerts.arn]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_cloudwatch_metric_alarm.production_no_healthy_targets.arn]
    }
  }
}

resource "aws_sns_topic_policy" "hotel_api_alerts" {
  arn    = aws_sns_topic.hotel_api_alerts.arn
  policy = data.aws_iam_policy_document.sns_combined.json
}
