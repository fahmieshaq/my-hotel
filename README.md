# Hotel API

A Spring Boot REST API for managing hotels, rooms, and bookings. The project demonstrates layered application design, database migrations, automated testing, containerization, observability, and deployment to AWS ECS Fargate.

## Stack

- Java 21; Spring Boot 4.1.1; Maven Wrapper
- Spring Web MVC, Jakarta Bean Validation, Spring Data JPA/Hibernate
- PostgreSQL with Flyway migrations
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator, Micrometer, Prometheus
- SLF4J/Logback with request-ID correlation
- Docker; Terraform; GitHub Actions; AWS ECS Fargate, ECR, ALB, ACM, CloudWatch, IAM, Secrets Manager, S3, EventBridge Scheduler
- Local monitoring: Prometheus, Grafana, Loki, Grafana Alloy

## Application structure

`src/main/java/com/maz/my_hotel/`:

- `controller/`: HTTP endpoints and DTO validation
- `dto/`: request and response data contracts
- `service/`: business logic
- `repository/`: JPA persistence
- `entity/`: domain entities (`Hotel`, `Room`, `Booking`)
- `exception/`: centralized error handling
- `filter/RequestIdFilter.java`: request tracing / MDC

Database migrations are in `src/main/resources/db/migration/`.

## API endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/hotels` | Create hotel |
| GET | `/hotels` | List hotels |
| GET | `/hotels/{id}` | Retrieve hotel |
| PUT | `/hotels/{id}` | Update hotel |
| DELETE | `/hotels/{id}` | Delete hotel |
| POST | `/rooms` | Create room |
| GET | `/rooms` | List rooms |
| GET | `/rooms/{id}` | Retrieve room |
| POST | `/bookings` | Create booking |
| GET | `/bookings` | List bookings |

OpenAPI: `/api-docs`; Swagger UI: `/swagger-ui.html` (availability may depend on deployment/network access).

## Local development

Prerequisites: Java 21, Docker, PostgreSQL connection details, and a shell. On Windows use `mvnw.cmd`; on Linux/macOS use `./mvnw`.

The default Spring profile is `dev`. Supply these environment variables (for example in an untracked `.env` used by Docker Compose):

```dotenv
DB_URL_DEV=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE?sslmode=require
DB_USER_DEV=YOUR_USER
DB_PASSWORD_DEV=YOUR_PASSWORD
```

Check `src/main/resources/application-dev.properties` for the exact datasource variable names. **Do not commit credentials.**

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Build the image and run the local monitoring stack:

```powershell
docker build -t hotel-api:local .
docker compose up -d
```

Docker Compose expects the dev DB variables and uses a prebuilt `hotel-api:local` image. Local ports: API `8080`, Prometheus `9090`, Grafana `3000`, Loki `3100` (bound to loopback).

## Testing

```powershell
.\mvnw.cmd clean verify
```

Tests cover controllers, services, repositories, request-ID filter, and integration scenarios. The Java 21 local verification previously passed 30 tests. Testcontainers tests may require a running Docker daemon.

## Health and observability

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness` — includes database health
- `/actuator/prometheus` — metrics endpoint

Local Grafana uses provisioned Prometheus and Loki data sources. Alloy collects Docker container logs. Production alerting and CloudWatch infrastructure are defined in Terraform; the local Docker monitoring stack should not be assumed to be deployed to AWS.

## CI/CD and environments

The GitHub Actions workflow `.github/workflows/ci.yml` runs Maven verification and Docker image build. On merges/pushes to `main`, it publishes an immutable image to ECR and deploys **staging before production**, using GitHub OIDC roles and ECS rollout/readiness checks. Review GitHub environment protection rules separately if approval gates are required.

| Environment | URL |
| --- | --- |
| Staging | https://staging-api.asmreditor.com |
| Production | https://api.asmreditor.com |

AWS region: `eu-west-2`; ECS cluster: `shared-apps-cluster`; ECS services: `hotel-api-staging`, `hotel-api-prod`.

Infrastructure source: `infrastructure/terraform/`. Application configuration uses separate `dev`, `staging`, and `prod` Spring profiles; staging and production database credentials are injected from managed secrets.

## Backups and disaster recovery

Production PostgreSQL backups are generated daily by an EventBridge-scheduled ECS task and uploaded to a private S3 bucket. A separate ECS restore task is constrained to the isolated `hotel_restore_test` database. See [Disaster Recovery Runbook](docs/DISASTER_RECOVERY.md).

## Security and repository hygiene

Never commit `.env`, `*.tfvars`, `*.tfstate*`, `*.tfplan`, `.terraform/`, secrets, private keys, or local Docker image archives. `.gitignore` covers these patterns, but ignored files can still be included in manually created ZIP archives or remain tracked if previously committed. Review `git ls-files` and rotate any exposed credentials.

## Repository layout

```text
.github/workflows/       CI/CD workflows
backup/                  pg_dump image and script
restore/                 isolated pg_restore image and script
infrastructure/terraform/ AWS infrastructure
observability/           Prometheus, Grafana, Loki, Alloy
src/main/                API and database migrations
src/test/                Automated tests
Dockerfile               Multi-stage Java 21 image
docker-compose.yml       Local application and observability
pom.xml                  Maven project
```
