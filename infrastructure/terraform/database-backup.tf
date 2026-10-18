
# ------------------------------------------------------------
# Production PostgreSQL Backup Bucket
# ------------------------------------------------------------

resource "aws_s3_bucket" "database_backups" {
  bucket = "hotel-api-prod-db-backups-343482277554"

  tags = {
    Name        = "hotel-api-prod-db-backups"
    Environment = "production"
    Purpose     = "database-backups"
  }
}

# Prevent public access
resource "aws_s3_bucket_public_access_block" "database_backups" {
  bucket = aws_s3_bucket.database_backups.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Enable bucket versioning
resource "aws_s3_bucket_versioning" "database_backups" {
  bucket = aws_s3_bucket.database_backups.id

  versioning_configuration {
    status = "Enabled"
  }
}

# Encrypt stored backups
resource "aws_s3_bucket_server_side_encryption_configuration" "database_backups" {
  bucket = aws_s3_bucket.database_backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Retain backups for 7 days
resource "aws_s3_bucket_lifecycle_configuration" "database_backups" {
  bucket = aws_s3_bucket.database_backups.id

  rule {
    id     = "expire-old-backups"
    status = "Enabled"

    filter {
      prefix = "daily/"
    }

    expiration {
      days = 7
    }

    noncurrent_version_expiration {
      noncurrent_days = 7
    }
  }
}

# ------------------------------------------------------------
# Database Backup - ECS Task Execution Role
# ------------------------------------------------------------

resource "aws_iam_role" "database_backup_execution" {
  name = "hotel-api-database-backup-execution"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name = "hotel-api-database-backup-execution"
  }
}

resource "aws_iam_role_policy_attachment" "database_backup_execution" {
  role = aws_iam_role.database_backup_execution.name

  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ------------------------------------------------------------
# Database Backup - ECS Task Role
# ------------------------------------------------------------

resource "aws_iam_role" "database_backup_task" {
  name = "hotel-api-database-backup-task"

  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = {
    Name = "hotel-api-database-backup-task"
  }
}

# ------------------------------------------------------------
# Database Backup - S3 Write Permissions
# ------------------------------------------------------------

resource "aws_iam_role_policy" "database_backup_s3" {
  name = "write-database-backups"
  role = aws_iam_role.database_backup_task.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Sid    = "WriteDailyBackups"
        Effect = "Allow"

        Action = [
          "s3:PutObject"
        ]

        Resource = [
          "${aws_s3_bucket.database_backups.arn}/daily/*"
        ]
      }
    ]
  })
}

# ------------------------------------------------------------
# Database Backup - Production Database Secret
# ------------------------------------------------------------

resource "aws_iam_role_policy" "database_backup_secrets" {
  name = "read-production-database-secret"
  role = aws_iam_role.database_backup_execution.id

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Sid    = "ReadProductionDatabaseCredentials"
        Effect = "Allow"

        Action = [
          "secretsmanager:GetSecretValue"
        ]

        Resource = [
          "arn:aws:secretsmanager:eu-west-2:343482277554:secret:hotel-api/prod/database-0yLA5U"
        ]
      }
    ]
  })
}

