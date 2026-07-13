# MakersHub Developer Guide

## 1. Software to Install

- Java 21 JDK (Eclipse Temurin recommended)
- Maven 3.9+
- Docker + Docker Compose
- PostgreSQL 15 client tools (optional)
- IntelliJ IDEA / VS Code
- Postman or Insomnia

## 2. Java Version

```bash
java -version
# openjdk version "21.0.x"
```

## 3. Maven Version

```bash
mvn -version
# Apache Maven 3.9.x
```

## 4. PostgreSQL Setup

Docker Compose starts PostGIS automatically. To use a local Postgres:

```sql
CREATE DATABASE makershub;
CREATE EXTENSION postgis;
```

## 5. Environment Variables

Copy `.env.example` to `.env` and fill in values. Required: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`.

## 6. Cloudinary Setup

Create an account at cloudinary.com. Copy cloud name, API key, and API secret into `.env`.

## 7. Firebase Setup

- Create a Firebase project.
- Download `google-services.json` for Android or `GoogleService-Info.plist` for iOS.
- Add the server service account JSON path to `FIREBASE_SERVICE_ACCOUNT_PATH`.

## 8. Paystack Setup

- Create a Paystack account.
- Use test secret key for development.
- Configure webhook URL: `https://api.makershub.gh/webhooks/paystack`.

## 9. Africa's Talking Setup

- Sign up at africastalking.com.
- Copy username and API key to `.env`.

## 10. Railway Deployment

See `DEPLOYMENT.md`.

## 11. Docker Commands

```bash
docker-compose up --build       # build & start
docker-compose down -v          # stop & remove volumes
docker-compose logs -f backend  # tail backend logs
```

## 12. Flyway Commands

```bash
mvn flyway:migrate
mvn flyway:info
mvn flyway:repair
```

## 13. Running Locally

```bash
cd makershub-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Visit `http://localhost:8080/swagger-ui.html`.

## 14. Running Tests

```bash
mvn test
mvn verify        # includes integration tests
```

## 15. Production Build

```bash
mvn -B -DskipTests clean package
java -jar target/makershub-backend-*.jar --spring.profiles.active=prod
```

## 16. Connecting React Native Frontend

Use the API client pattern in `docs/FRONTEND_INTEGRATION.md`.

## 17. API Testing with Postman

Import the OpenAPI spec from `http://localhost:8080/v3/api-docs`. Set the `Authorization` header to `Bearer {{access_token}}`.

## 18. Swagger Usage

Swagger UI is available at `/swagger-ui.html`. All protected endpoints show a lock icon; click it to add a Bearer token.

## 19. Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8080 in use | `SERVER_PORT=8081 mvn spring-boot:run` |
| Flyway checksum error | `mvn flyway:repair` |
| PostGIS not found | Ensure `postgis` extension is created |
| JWT secret too short | Use a Base64-encoded 256-bit key |
| Testcontainers timeout | Increase Docker memory limit |

## 20. Production Deployment Checklist

See `DEPLOYMENT.md`.
