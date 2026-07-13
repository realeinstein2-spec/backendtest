# MakersHub Backend

Production-grade Spring Boot backend for **MakersHub** — Ghana's on-demand manufacturing marketplace.

## Tech Stack

- Java 21
- Spring Boot 3.x
- PostgreSQL 15 + PostGIS
- Flyway migrations
- Spring Security + JWT
- Maven
- Docker / Docker Compose
- Railway deployment ready

## Quick Start

### Prerequisites

1. Java 21 JDK
2. Maven 3.9+
3. Docker & Docker Compose
4. PostgreSQL 15 (or use Docker)

### Run locally with Docker Compose

```bash
cd makershub-backend
cp .env.example .env
# Edit .env with real secrets
docker-compose up --build
```

### Run locally without Docker

```bash
# Start PostgreSQL/PostGIS and create database `makershub`
mvn -f makershub-backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run tests

```bash
cd makershub-backend
mvn verify
```

### Apply Flyway migrations manually

```bash
cd makershub-backend
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/makershub -Dflyway.user=postgres -Dflyway.password=postgres
```

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Environment Variables

See `.env.example` for the full list.

## Architecture

- Clean Architecture: controllers → services → repositories → entities
- DTOs for all API requests/responses
- Global exception handling
- Audit logging (async)
- Async notifications (Firebase + SMS fallback)
- Paystack webhook signature verification

## Security

- BCrypt password hashing
- JWT access (15 min) + refresh (30 days) tokens
- Role-based authorization
- CORS configured
- Input validation with Jakarta Validation
- Paystack HMAC webhook verification
- SQL injection prevention via JPA parameter binding

## Modules Implemented

- Authentication & User Management
- Factory Verification
- Job Posting
- Job Matching (PostGIS proximity)
- Bid System
- Order State Machine
- Escrow Payments (Paystack)
- Reviews
- Disputes
- Notifications
- Messaging
- Admin Dashboard
- Analytics
- Audit Logs

## Deployment

The repository includes a Dockerfile, docker-compose.yml, and GitHub Actions workflow. Railway deployment uses the same Docker image; set environment variables in the Railway dashboard.

## License

Proprietary — MakersHub Group 62
