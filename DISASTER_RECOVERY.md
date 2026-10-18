# Disaster Recovery Runbook — Hotel API

## Scope and safety

This runbook covers **PostgreSQL logical backup and isolated restore verification** for the Hotel API running in AWS `eu-west-2`. It is not an automated production failover plan.

**Important:** The supplied `restore/restore.sh` explicitly rejects targets other than a database whose URL ends with `/hotel_restore_test`. Do not bypass this safeguard or attempt to run the restore task against production. Production recovery requires a separately reviewed and approved procedure.

## Infrastructure

| Component | Configuration |
| --- | --- |
| Production database | Neon PostgreSQL (connection details via AWS Secrets Manager) |
| Backup runner | ECS Fargate task `hotel-api-database-backup` |
| Schedule | EventBridge Scheduler `cron(0 2 * * ? *)` — 02:00 UTC daily |
| Backup image | PostgreSQL 18 + AWS CLI, built from `backup/Dockerfile` |
| Backup destination | `s3://hotel-api-prod-db-backups-343482277554/daily/` |
| Backup format | PostgreSQL custom format (`pg_dump --format=custom`) |
| S3 lifecycle | Current and noncurrent objects expire after 7 days per Terraform |
| Restore runner | ECS Fargate task `hotel-api-database-restore` |
| Restore image | PostgreSQL 18 + AWS CLI, built from `restore/Dockerfile` |
| Restore target | Isolated Neon database `hotel_restore_test` |
| Logs | CloudWatch log groups configured in Terraform |
| Failure monitoring | CloudWatch log metric filter and alarm for `BACKUP_FAILED` |

Source of truth: `infrastructure/terraform/database-backup.tf`, `database-backup-runtime.tf`, and `database-restore.tf`.

## Backup flow

1. EventBridge Scheduler starts the ECS backup task.
2. The task receives production database connection variables from AWS Secrets Manager.
3. `backup/backup.sh` removes JDBC URL query parameters, requires SSL, and executes `pg_dump` using PostgreSQL custom format.
4. The script rejects an empty backup and uploads the dump to S3 with key `daily/hotel-api-prod-YYYY-MM-DDTHH-MM-SSZ.dump`.
5. CloudWatch logs `BACKUP_SUCCESS` including the S3 object URI, or `BACKUP_FAILED` if a command fails.

The script uses `umask 077` and removes the temporary local dump when finished. S3 public access blocking, versioning, and server-side encryption are defined in Terraform.

## Routine daily verification

1. In AWS Console, open **EventBridge Scheduler** and verify the backup schedule is enabled and has executed as expected.
2. In **CloudWatch Logs**, inspect the backup task log stream for the most recent `BACKUP_SUCCESS`.
3. In **S3**, verify a recent nonempty object exists under `daily/`.
4. In **CloudWatch Alarms**, check `hotel-api-database-backup-failed` and investigate any `ALARM` or missing expected backup.
5. Record the backup object key, UTC timestamp, object size, and task execution ID in the operations log.

A `BACKUP_SUCCESS` message confirms a dump was uploaded, **not** that it can be restored. Run an isolated restore drill periodically.

## Isolated restore drill

**Preconditions:** Appropriate AWS access; existing isolated Neon branch/database `hotel_restore_test`; restore-test secret in AWS Secrets Manager; network access from the ECS task to Neon; valid S3 object within retention.

1. Identify the desired backup object key in S3.
2. Confirm the restore-test secret points to the **isolated** database, not production.
3. Review the ECS task definition `hotel-api-database-restore`. Its `BACKUP_KEY` is configured in Terraform and may refer to a historical dump. Update it to the chosen existing S3 object key before a new drill, following infrastructure change review.
4. Start **one standalone ECS Fargate task** from the restore task definition in the configured VPC/subnets/security group. Do not change either running Hotel API ECS service.
5. Watch the task in ECS and the restore CloudWatch log group.
6. Require task exit code `0` and `RESTORE_SUCCESS` in logs.
7. Connect to the isolated Neon database and verify schema, Flyway history, and row counts against a production snapshot where appropriate.
8. Record the S3 key, target database, task definition revision, execution ID, UTC time, and validation results.

The restore script downloads the object from S3 and executes:

```bash
pg_restore --dbname="$connection_uri" --no-owner --no-acl --exit-on-error "$backup_file"
```

It requires SSL and verifies the database URL ends in `/hotel_restore_test`. **It does not automatically create a fresh empty target or drop existing tables**; use a clean isolated target to avoid conflicts.

## Last verified recovery drill

A prior drill restored `daily/hotel-api-prod-2026-10-18T04-54-37Z.dump` to an isolated Neon test database. ECS task exited with code `0` and CloudWatch recorded `RESTORE_SUCCESS`.

Verified tables: `booking`, `flyway_schema_history`, `hotel`, `revchanges`, `revinfo`, `room`. Row counts matched the production snapshot for `hotel=0`, `room=0`, `booking=0`, and `flyway_schema_history=2`.

**Limitation:** Business tables were empty during that drill. Restoration of nonempty booking/hotel/room records and application-level consistency still needs validation. This old S3 key is an example, not a guaranteed available object after retention expiry.

## Incident response

**Missing backup:** Check Scheduler state/history, ECS stopped-task reason, CloudWatch logs, database reachability, Secrets Manager permissions, S3 IAM access, and backup container image availability. Re-run an authorized backup task after fixing the cause, then confirm the object exists.

**Backup task failed:** Search logs for `BACKUP_FAILED`; determine whether the failure occurred during `pg_dump`, S3 upload, or environment validation. Do not treat a partial/empty object as valid.

**Restore task failed:** Search for `RESTORE_FAILED`, inspect task exit code and database connectivity, ensure the target is `hotel_restore_test`, confirm `BACKUP_KEY` exists, and check whether the target contains conflicting objects. Never test against production.

**Production database outage/data loss:** Escalate and assess Neon-native recovery options, business impact, and the last usable backup. Define an approved recovery target and cutover plan before modifying production. The existing restore image intentionally cannot restore directly into production.

## Recovery objectives and limitations

No formal RPO/RTO has been established in the repository. A daily logical backup cadence implies that, **if backups run successfully**, up to approximately 24 hours of changes may be absent from the latest dump, plus any schedule delays. This is **not** a guaranteed RPO. RTO has not been measured.

The Terraform S3 lifecycle retains current and noncurrent object versions for 7 days. Confirm the desired backup is still present before starting recovery.

## Periodic improvements

- Validate restores with nonempty business data and relational consistency checks.
- Schedule and record recurring restore drills.
- Define approved RPO/RTO, production recovery ownership, and cutover/rollback procedures.
- Consider independent backup retention or point-in-time recovery based on business requirements.
- Keep restore task `BACKUP_KEY` updated for each drill; do not rely on a fixed dated key.
- Verify alert delivery, not just CloudWatch alarm state.

## Security

Use least-privilege IAM, Secrets Manager, private S3 storage, and isolated restore targets. Never print database passwords, store secrets in documentation, commit Terraform state/plan files, or share full project ZIPs containing `.env` and `terraform.tfvars`.
