#!/usr/bin/env bash
set -Eeuo pipefail

fail() {
  status=$?
  echo "RESTORE_FAILED: PostgreSQL restore exited with code ${status}" >&2
  exit "$status"
}
trap fail ERR

: "${SPRING_DATASOURCE_URL:?Missing restore database URL}"
: "${SPRING_DATASOURCE_USERNAME:?Missing restore username}"
: "${SPRING_DATASOURCE_PASSWORD:?Missing restore password}"
: "${BACKUP_BUCKET:?Missing backup bucket}"
: "${BACKUP_KEY:?Missing backup object key}"
: "${AWS_REGION:?Missing AWS region}"

connection_uri="${SPRING_DATASOURCE_URL#jdbc:}"
connection_uri="${connection_uri%%\?*}"

# Only allow restores into the designated temporary test database.
if [[ "$connection_uri" != */hotel_restore_test ]]; then
  echo "RESTORE_FAILED: Target must be hotel_restore_test" >&2
  exit 1
fi

export PGUSER="$SPRING_DATASOURCE_USERNAME"
export PGPASSWORD="$SPRING_DATASOURCE_PASSWORD"
export PGSSLMODE=require
export PGCONNECT_TIMEOUT=20

backup_file="$(mktemp /tmp/hotel-restore-XXXXXXXX.dump)"
trap 'rm -f -- "$backup_file"' EXIT

echo "Downloading database backup from S3"

aws s3 cp \
  "s3://${BACKUP_BUCKET}/${BACKUP_KEY}" \
  "$backup_file" \
  --region "$AWS_REGION" \
  --only-show-errors

echo "Restoring PostgreSQL database"

pg_restore \
  --dbname="$connection_uri" \
  --no-owner \
  --no-acl \
  --exit-on-error \
  "$backup_file"

echo "RESTORE_SUCCESS"