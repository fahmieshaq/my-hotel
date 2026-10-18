#!/usr/bin/env bash
set -Eeuo pipefail

backup_file=""

cleanup() {
  if [[ -n "$backup_file" ]]; then
    rm -f -- "$backup_file"
  fi
}

fail() {
  status=$?
  echo "BACKUP_FAILED: PostgreSQL backup exited with code ${status}" >&2
  exit "$status"
}

trap cleanup EXIT
trap fail ERR

: "${SPRING_DATASOURCE_URL:?Missing database URL}"
: "${SPRING_DATASOURCE_USERNAME:?Missing database username}"
: "${SPRING_DATASOURCE_PASSWORD:?Missing database password}"
: "${BACKUP_BUCKET:?Missing backup bucket}"
: "${AWS_REGION:?Missing AWS region}"

# Convert JDBC URL to PostgreSQL URI.
connection_uri="${SPRING_DATASOURCE_URL#jdbc:}"

if [[ "$connection_uri" != postgresql://* ]]; then
  echo "BACKUP_FAILED: Invalid PostgreSQL URL" >&2
  exit 1
fi

# Remove JDBC query parameters.
connection_uri="${connection_uri%%\?*}"

export PGUSER="$SPRING_DATASOURCE_USERNAME"
export PGPASSWORD="$SPRING_DATASOURCE_PASSWORD"
export PGSSLMODE=require
export PGCONNECT_TIMEOUT=20

umask 077

backup_file="$(mktemp /tmp/hotel-prod-XXXXXXXX.dump)"

backup_date="$(date -u +%Y-%m-%dT%H-%M-%SZ)"
object_key="daily/hotel-api-prod-${backup_date}.dump"

echo "Starting PostgreSQL backup: ${backup_date}"

pg_dump \
  --dbname="$connection_uri" \
  --format=custom \
  --no-owner \
  --no-acl \
  --file="$backup_file"

if [[ ! -s "$backup_file" ]]; then
  echo "BACKUP_FAILED: Empty backup file" >&2
  exit 1
fi

aws s3 cp \
  "$backup_file" \
  "s3://${BACKUP_BUCKET}/${object_key}" \
  --region "$AWS_REGION" \
  --only-show-errors

echo "BACKUP_SUCCESS: s3://${BACKUP_BUCKET}/${object_key}"
