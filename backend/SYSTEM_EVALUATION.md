# MakersHub Backend System Evaluation

Date: 2026-07-24

Scope: static evaluation of the backend repository only. No existing source, configuration, migration, or test files were changed. Tests/builds were not run as part of this review to avoid producing additional workspace changes.

## Executive Summary

The system is a substantial Spring Boot 3 / Java 21 marketplace backend with clear module boundaries: controllers, services, repositories, DTOs, entities, Flyway migrations, JWT/Firebase auth, Paystack payments, WebSockets, notifications, support tickets, admin tooling, analytics, reviews, disputes, and order lifecycle management.

Overall maturity is medium-to-high for a student/startup backend: the project has real production concerns represented in code, including Flyway, validation, security headers, role-based authorization, refresh token persistence, payment webhook signature verification, idempotent payment handling, Testcontainers configuration, Docker packaging, and documentation.

The main risks are production hardening and consistency rather than missing core features. Highest-priority areas are security profile hygiene, social auth verification, OTP strength/rate limiting, pagination/sort safety, database migration validation against production-like data, operational observability, and payment/refund completeness.

## Current Strengths

- Clear Spring Boot structure with controllers, services, repositories, DTOs, entities, and configuration separated.
- Java 21 and Spring Boot 3.3.x are modern choices.
- PostgreSQL/PostGIS with Flyway migrations supports repeatable deployment.
- `ddl-auto: validate` in the main profile avoids accidental schema mutation.
- JWT access/refresh token model is implemented, with refresh tokens persisted server-side and rotated on refresh.
- Role-based authorization is applied with `@PreAuthorize` and security route matching.
- Paystack payment initialization, verification, and webhook signature verification are present.
- Payment success handling uses database locking and idempotency checks around transaction status and escrow creation.
- Order lifecycle has explicit status transition validation.
- Security headers, CORS configuration, BCrypt cost 12, and request validation are present.
- Dockerfile uses a non-root runtime user and multi-stage build.
- Test suite exists across controllers, services, repositories, and utility code.
- Documentation is broad: API, deployment, developer guide, integration notes, ER diagram, and frontend guides.

## High-Priority Findings

### 1. Production profile and Swagger configuration are inconsistent

`SecurityConfig` only permits Swagger endpoints outside `prod`, but `application-prod.yml` explicitly enables Springdoc API docs and Swagger UI. In production, this likely results in generated docs being enabled at the Springdoc layer but blocked by security. That is safer than fully public docs, but the configuration intent is inconsistent and can confuse deployments.

Suggestion:

- Set `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false` in production unless there is a strict authenticated/internal docs requirement.
- If production API docs are required, expose them behind admin auth or network-level restrictions.

### 2. Default active profile is `dev`

`application.yml` defaults `SPRING_PROFILES_ACTIVE` to `dev`. This can accidentally enable dev-only behavior if an environment forgets to set the profile. Dev mode returns OTP codes in auth responses and enables verbose SQL/security logs.

Suggestion:

- Avoid defaulting deployed environments to `dev`.
- Prefer no default profile, or default to a safer base profile.
- Add deployment checks that fail startup unless `SPRING_PROFILES_ACTIVE=prod` in production.

### 3. OTP code space is only 4 digits despite accepting 4-8 digits

The auth DTO accepts OTPs of length 4 to 8, and `AuthService` generates a 4-digit OTP. Even with a 5-attempt cap, 4 digits is weak for a production payment marketplace, especially without visible endpoint-level rate limiting.

Suggestion:

- Move to 6 digits minimum.
- Add rate limiting for login, resend OTP, and verify OTP by phone number, user ID, and IP.
- Add resend cooldown and max sends per time window.
- Consider storing only a hash of OTP codes, not plaintext OTP values.

### 4. Social auth path needs hardening

Google auth uses Firebase verification, which is good if Firebase is configured correctly. Apple auth manually fetches Apple keys using a new `RestTemplate`, creates a parser, and checks issuer/audience. This path needs stricter validation and operational hardening.

Suggestion:

- Reuse the centrally configured timeout-aware HTTP client for Apple key fetching.
- Cache Apple JWKs with expiry instead of fetching on every login.
- Strictly validate token algorithm, issuer, audience, expiration, subject, and nonce if the client flow provides one.
- Fail closed if Apple client ID is blank in production.
- Add focused tests for invalid issuer, invalid audience, expired token, unknown key ID, and missing email.

### 5. Public WebSocket handshake route depends on STOMP interceptor correctness

HTTP security permits `/ws/**`, while STOMP `CONNECT` authorization happens in `WebSocketAuthInterceptor`. This is a common pattern, but it means all meaningful enforcement depends on the interceptor being applied correctly for every client path and broker behavior.

Suggestion:

- Add integration tests for unauthenticated CONNECT, invalid token CONNECT, unauthorized SUBSCRIBE to another user's order topic, and valid subscription.
- Consider explicit destination authorization rules if the message model grows beyond `/topic/orders/{id}`.

### 6. Pagination and sort sanitization are not consistently applied

`PageableUtils` exists and is used in some admin paths, but many pageable service methods pass user-supplied `Pageable` directly to repositories. Invalid sort properties are partly handled by a global exception handler, but arbitrary large page sizes and inconsistent sort properties can still cause performance or UX problems.

Suggestion:

- Apply a shared pageable sanitizer across all list endpoints.
- Enforce max page size globally, for example 50 or 100.
- Use endpoint-specific sort allowlists to prevent expensive or invalid sorts.

### 7. Payment verification endpoint needs input validation

`/api/v1/payments/verify` accepts a raw `Map<String, String>` and reads `reference` without DTO validation. Missing or blank references will flow into service logic and can produce unclear errors.

Suggestion:

- Replace raw map input with a validated DTO.
- Validate reference format and length.
- Return a clear `VALIDATION_ERROR` for missing or malformed references.

### 8. Payment/refund lifecycle appears incomplete

The backend has escrow states, disputes, refunds, and release transitions, but the observed payment code marks escrow records released/refunded internally. Actual Paystack payout/transfer/refund execution and reconciliation are not clearly represented in the inspected payment path.

Suggestion:

- Define whether escrow release/refund is only internal bookkeeping or should trigger external money movement.
- Add explicit transfer/refund transaction records and statuses.
- Add retryable background jobs for failed provider calls.
- Add reconciliation jobs that compare local transaction state with Paystack.
- Add admin-visible audit trails for every financial state transition.

## Medium-Priority Findings

### 9. External integration secrets are often optional blanks

Paystack, Cloudinary, Firebase, Apple, Google, and SMS configuration can be blank. Some services check this at call time, but production deployments can still start with important integrations silently disabled or partially broken.

Suggestion:

- Add startup validation for required integrations by profile.
- Fail startup in production when required secrets are missing.
- Keep optional integrations explicitly disabled via booleans rather than silently blank.

### 10. Logging profile needs tighter production defaults

Base config sets `com.makershub: DEBUG`; prod overrides it to INFO. Dev logs SQL and Spring Security at DEBUG. This is acceptable for local work, but risky if the wrong profile is used.

Suggestion:

- Move DEBUG logging entirely into `application-dev.yml`.
- Keep base logging conservative.
- Add log redaction tests or conventions for tokens, OTPs, signatures, phone numbers, and payment references.

### 11. JWT secret handling should be validated more clearly

`JwtUtil` decodes the configured secret as Base64 and creates an HS512 key. Startup will fail if the secret is missing or invalid, which is good. The failure mode may still be cryptic for operators.

Suggestion:

- Add explicit startup validation with a clear error message for missing, non-Base64, or too-short JWT secrets.
- Document minimum entropy and generation command in deployment docs.

### 12. Last-active update writes can become a hidden database cost

`JwtAuthenticationFilter` updates `lastActiveAt` at most once per user per minute using an in-memory map. This is a practical optimization, but in multi-instance deployments each instance has its own cache and writes can still be frequent.

Suggestion:

- Consider asynchronous
-  batching or a low-priority event queue for last-active updates.
- Add metrics for last-active update frequency and failure count.
- For multi-instance deployments, consider whether per-instance throttling is sufficient.

### 13. Migration chain should be tested against a migrated schema, not only `create-drop`

The test profile disables Flyway and uses Hibernate `create-drop`. That helps entity tests but does not prove Flyway migrations can build the schema from scratch or upgrade an existing database.

Suggestion:

- Add a Flyway migration integration test that starts from an empty PostgreSQL container and runs all migrations.
- Add a schema validation test after migrations.
- Add a production-like upgrade test if there is seeded/staging data.

### 14. Review migration history shows schema correction after initial uniqueness design

The review table originally had `order_id UNIQUE`, then later migrations added `(order_id, reviewer_id)` uniqueness and dropped the old unique constraint. This is reasonable, but it is exactly the kind of evolution that should be validated against existing production data.

Suggestion:

- Confirm V11 and V14 are ordered and applied successfully in deployed databases.
- Add migration tests around two-party reviews for the same order.

### 15. Some request DTOs validate string IDs manually instead of typed UUIDs

Several request DTOs accept IDs as strings and services call `UUID.fromString`. This works but pushes parse errors into service methods and generic exception handling.

Suggestion:

- Prefer UUID fields in request DTOs where possible.
- If string IDs are required for frontend compatibility, add `@Pattern` or custom validation.

### 16. Domain comments include bug-ticket markers in production code

The code contains many comments such as `BUG-01`, `H-10`, `C-8`, and `M-31`. They document previous fixes, but over time these can make code noisy and can expose internal process labels.

Suggestion:

- Keep comments that explain current behavior and remove historical ticket labels during cleanup.
- Move resolved security/fix history to changelog or issue tracker.

## Lower-Priority Findings

### 17. Docker build skips tests

The Dockerfile builds with `mvn -B -DskipTests clean package`. This is normal for fast image builds when CI runs tests separately, but risky if Docker build is the only gate.

Suggestion:

- Ensure CI runs `mvn verify` before building or deploying the image.
- Keep Docker build fast, but do not use it as the sole quality gate.

### 18. Docker Compose uses default Postgres credentials for local development

Local compose uses `postgres/postgres`. That is acceptable for local-only development but should never be copied into deployed environments.

Suggestion:

- Make documentation explicit that compose credentials are local-only.
- Keep production credentials entirely outside compose defaults.

### 19. Actuator exposure should be reviewed by deployment environment

The app exposes health, info, metrics, and prometheus. Health details are protected with `when_authorized`, but metrics exposure can still leak operational details if the service is public.

Suggestion:

- Confirm actuator endpoints are private or protected in production.
- Consider exposing only health publicly and putting metrics/prometheus behind internal networking.

### 20. Documentation encoding appears inconsistent

Some rendered text shows replacement artifacts for punctuation such as em dashes and arrows. This is probably encoding mismatch rather than functional risk.

Suggestion:

- Normalize Markdown and YAML files to UTF-8.
- Prefer ASCII punctuation if editors or deployment tooling continue to produce encoding artifacts.

## Testing Recommendations

Recommended next test additions:

- Flyway migration test against PostgreSQL/PostGIS.
- Security integration tests for protected routes, admin-only routes, Swagger exposure by profile, and CORS origins.
- WebSocket authentication and subscription authorization tests.
- OTP brute-force, resend, expiry, lockout, and dev/prod response behavior tests.
- Payment verification DTO validation tests.
- Paystack webhook replay/idempotency tests.
- Payment amount/currency/reference mismatch tests.
- Order status transition matrix tests.
- Dispute resolution escrow release/refund behavior tests.
- Pageable max-size and sort allowlist tests.
- Social login invalid token tests for Google/Firebase and Apple.

## Operational Recommendations

- Add CI stages: format/checkstyle or equivalent, unit tests, integration tests, Flyway migration test, package, container build.
- Add dependency vulnerability scanning for Maven dependencies and Docker base images.
- Add structured logs with request IDs and correlation IDs.
- Add metrics for auth failures, OTP sends/failures, payment init/verify/webhook outcomes, order transition failures, notification delivery failures, and external API latency.
- Add alerting for payment webhook failures, repeated invalid signatures, failed payout/refund jobs, and high auth failure rates.
- Add database backup and restore drills.
- Add production runbooks for Paystack incidents, failed migrations, SMS outage, Firebase outage, and rollback.

## Suggested Priority Plan

1. Lock production profile behavior: disable Swagger in prod config, remove dev as a safe deployment default, and add startup validation for required production secrets.
2. Strengthen auth: 6-digit OTP, rate limits, resend cooldown, OTP hashing, social auth validation tests.
3. Harden money movement: clarify escrow release/refund external behavior, add reconciliation, retries, and admin financial audit visibility.
4. Add migration and security integration tests before more feature work.
5. Standardize pageable sanitization and max page sizes across every list endpoint.
6. Improve observability around auth, payments, notifications, and order state transitions.

## Worktree Note

Before this evaluation file was created, the worktree already contained unrelated changes and untracked files, including migration changes and generated crash/build artifacts. Those files were not modified by this evaluation.

