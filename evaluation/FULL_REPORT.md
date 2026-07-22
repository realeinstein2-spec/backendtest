# Full Backend System Evaluation

Evaluation date: 2026-07-16

Scope: read-only inspection of source, config, migrations, tests, Docker/CI, and docs. Existing files were not modified. Tests were not run because Maven would write to `target/`.

## Final Scores

- Overall backend health: 72/100
- Security: 63/100
- Performance: 70/100
- Maintainability: 74/100
- Scalability: 68/100
- Test coverage assessment: Moderate service-unit coverage; weak integration, security, payment, WebSocket, and migration coverage.
- Production readiness: Not production-ready until critical payment, OTP, authorization, rate limiting, and observability gaps are addressed.
- Risk assessment: Medium-high. Core architecture is viable, but several production controls are incomplete or inconsistent.
- Estimated total remediation effort: 4-7 engineering weeks.

## Issue Register

### 1. OTP and SMS content logged at INFO

Severity: Critical
Category: Security
Files affected: `src/main/java/com/makershub/notification/SmsService.java`, `src/main/java/com/makershub/service/AuthService.java`
Relevant locations: `SmsService.java:10-14`, `AuthService.java:231-240`
Description: AuthService sends OTP text through `SmsService`, and the current `SmsService.send` implementation logs phone number and full SMS body at INFO.
Why it matters: OTPs are credentials. Logs are commonly centralized, retained, searched, and visible to operators.
Potential impact: Account takeover via log access; compliance and privacy exposure.
Recommended solution: Never log OTPs or full SMS bodies. Redact phone numbers. Use a real provider implementation in production and a test-only stub in test/dev.
Estimated effort: Small.
Related issues: 4, 5.

### 2. Paystack blank-secret behavior is unsafe

Severity: Critical
Category: Security / Payments
Files affected: `src/main/java/com/makershub/service/PaymentService.java`, `src/main/java/com/makershub/util/PaystackSignatureVerifier.java`, `src/main/resources/application.yml`
Relevant locations: `PaymentService.java:109-112`, `PaymentService.java:121-124`, `PaystackSignatureVerifier.java:12-21`, `application.yml:44`
Description: Payment initiation returns a simulated authorization URL when the Paystack secret is blank, and webhook signature verification still computes HMAC using the provided secret even when it is blank.
Why it matters: Production misconfiguration can silently create fake payment flows or make webhook verification rely on an empty key.
Potential impact: Fraudulent order state changes, false escrow states, financial loss.
Recommended solution: Fail startup in production when Paystack secret is blank. Disable simulation outside explicit local/test profiles. Reject webhooks if secret is blank.
Estimated effort: Small to medium.
Related issues: 10, 17.

### 3. Bid listing lacks object ownership authorization

Severity: High
Category: Authorization / API
Files affected: `src/main/java/com/makershub/controller/BidController.java`, `src/main/java/com/makershub/service/BidService.java`
Relevant locations: `BidController.java:37-39`, `BidService.java:93-97`
Description: Controller requires `SME_OWNER`, but service lists all bids for any job ID without confirming the authenticated SME owns that job.
Why it matters: Bids can include pricing and supplier strategy.
Potential impact: Data leakage between SMEs and competitive manipulation.
Recommended solution: Load the job, require `job.sme.id == currentUser.id`, then list bids. Add controller/security tests.
Estimated effort: Small.
Related issues: 13.

### 4. Auth and OTP endpoints lack rate limiting

Severity: High
Category: Security
Files affected: `src/main/java/com/makershub/controller/AuthController.java`, `src/main/java/com/makershub/service/AuthService.java`
Relevant locations: `AuthController.java:20-38`, `AuthService.java:114-190`
Description: Login, register, refresh, and OTP verification have attempt counting only per current OTP record, not per IP/phone/time window.
Why it matters: Attackers can brute force credentials, enumerate phone numbers, spam OTP generation, and pressure SMS costs.
Potential impact: Account takeover attempts, denial of service, SMS cost abuse.
Recommended solution: Add distributed rate limiting by IP and phone, exponential backoff, lockout, and monitoring.
Estimated effort: Medium.
Related issues: 1, 5.

### 5. OTP entropy and storage are weak

Severity: High
Category: Security
Files affected: `src/main/java/com/makershub/service/AuthService.java`, `src/main/java/com/makershub/entity/OtpVerification.java`, `src/main/resources/db/migration/V3__create_otp_table.sql`
Relevant locations: `AuthService.java:220`, `OtpVerification.java:31`, `V3__create_otp_table.sql:4`
Description: OTP generation uses `%04d`, creating 10,000 possibilities, and OTPs are stored in plaintext.
Why it matters: A 4-digit OTP is weak for internet-facing auth, and plaintext OTPs are recoverable from database access.
Potential impact: Account takeover if rate limits fail or DB/log access is compromised.
Recommended solution: Use 6+ digit OTPs or stronger challenge codes, hash OTPs, and enforce short expiry plus rate limits.
Estimated effort: Medium.
Related issues: 1, 4.

### 6. Review schema conflicts with two-party review logic

Severity: High
Category: Database / Business Logic
Files affected: `src/main/resources/db/migration/V1__init_schema.sql`, `src/main/java/com/makershub/service/ReviewService.java`, `src/main/java/com/makershub/repository/ReviewRepository.java`
Relevant locations: `V1__init_schema.sql:132-145`, `ReviewService.java:56-58`, `ReviewRepository.java:15`
Description: Database has `order_id UUID NOT NULL UNIQUE`, but service checks uniqueness by `(orderId, reviewerId)`.
Why it matters: Once one party reviews an order, the other party cannot insert a review.
Potential impact: Broken review workflow and production insert failures.
Recommended solution: Replace unique `order_id` constraint with unique `(order_id, reviewer_id)`.
Estimated effort: Small to medium migration.
Related issues: 20.

### 7. Private `@Async` method is ineffective

Severity: Medium
Category: Performance / Architecture
Files affected: `src/main/java/com/makershub/service/JobService.java`
Relevant locations: `JobService.java:68-70`, `JobService.java:98-110`
Description: `notifyMatchingFactories` is private and called from the same bean, so Spring proxy-based async execution will not apply.
Why it matters: Job creation can block on matching and notifications.
Potential impact: Slower job creation, request timeouts under large factory sets or slow notification paths.
Recommended solution: Move notification dispatch to a separate `@Service` method, application event listener, or queue.
Estimated effort: Small.
Related issues: 12.

### 8. Invalid IDs and filters can produce 500 errors

Severity: Medium
Category: API / Error Handling
Files affected: multiple DTOs and services
Relevant locations: `BidService.java:59`, `PaymentService.java:64`, `ReviewService.java:42`, `MessageService.java:47`, `JobService.java:78-79`, `GlobalExceptionHandler.java:70-74`
Description: String IDs and numeric filters are parsed inside services. Invalid input throws `IllegalArgumentException` or `NumberFormatException`, which falls to generic 500.
Why it matters: Client mistakes should be reported as 400 with actionable errors.
Potential impact: Poor API UX, noisy error logs, misleading monitoring.
Recommended solution: Use `UUID` controller parameters/body fields where possible, validate string fields with patterns, and add specific exception handlers.
Estimated effort: Small to medium.
Related issues: 13.

### 9. Refresh tokens are not revocable or rotated

Severity: Medium
Category: Authentication
Files affected: `src/main/java/com/makershub/security/JwtUtil.java`, `src/main/java/com/makershub/service/AuthService.java`
Relevant locations: `JwtUtil.java:35-47`, `AuthService.java:196-211`
Description: Refresh tokens are stateless JWTs with 30-day lifetime and no persisted token family, rotation, reuse detection, or logout revocation.
Why it matters: Compromised refresh tokens stay valid until expiry unless user is suspended or unverified.
Potential impact: Long-lived unauthorized access.
Recommended solution: Persist refresh token identifiers, rotate on use, support revocation and logout, detect reuse.
Estimated effort: Medium.
Related issues: 4.

### 10. Payment initiation performs external call inside DB transaction

Severity: Medium
Category: Reliability / Performance
Files affected: `src/main/java/com/makershub/service/PaymentService.java`
Relevant locations: `PaymentService.java:61-119`
Description: Escrow creation and Paystack API call occur in one transactional method.
Why it matters: Network latency or failure holds DB resources and creates ambiguous partial states.
Potential impact: Connection pool pressure and difficult retry behavior.
Recommended solution: Persist pending payment, commit, then call Paystack through an outbox/job or explicit state machine with retries.
Estimated effort: Medium.
Related issues: 2.

### 11. Security header disables content type options

Severity: Medium
Category: Security Headers
Files affected: `src/main/java/com/makershub/config/SecurityConfig.java`
Relevant locations: `SecurityConfig.java:60-66`
Description: `contentTypeOptions` is disabled.
Why it matters: `X-Content-Type-Options: nosniff` helps reduce MIME-sniffing attacks.
Potential impact: Increased browser-side attack surface.
Recommended solution: Remove the disable call and rely on Spring Security defaults or explicitly enable.
Estimated effort: Small.
Related issues: none.

### 12. DTO mapping can trigger N+1 queries

Severity: Medium
Category: Performance / Database
Files affected: `src/main/java/com/makershub/mapper/DtoMapper.java`, repositories returning pages/lists
Relevant locations: `DtoMapper.java:13-39`, `OrderService.java:186-194`, `MessageService.java:90`
Description: Mappers dereference lazy relations such as user, factory, job, order, and factory profile fields during page/list mapping.
Why it matters: Each item can trigger additional SQL.
Potential impact: Latency and DB load grow with page size.
Recommended solution: Add fetch joins, `@EntityGraph`, DTO projections, or tailored queries for list endpoints.
Estimated effort: Medium.
Related issues: 7.

### 13. API docs are stale/inconsistent

Severity: Medium
Category: Documentation / API
Files affected: `docs/API.md`, `README.md`, controllers/services
Relevant locations: `docs/API.md:3`, `docs/API.md:27-52`, `docs/API.md:100-107`, `AuthController.java:29-38`, `OrderService.java:228-235`
Description: Docs use `/v1`, describe login returning tokens, and list a payment transition that service code intentionally blocks.
Why it matters: Frontend and integrators will implement incorrect flows.
Potential impact: Integration delays and production client failures.
Recommended solution: Update docs from OpenAPI and add tested examples.
Estimated effort: Small.
Related issues: 8.

### 14. Swagger production posture is inconsistent

Severity: Medium
Category: Configuration / Security
Files affected: `src/main/resources/application-prod.yml`, `src/main/java/com/makershub/config/SecurityConfig.java`
Relevant locations: `application-prod.yml:12-17`, `SecurityConfig.java:70-74`
Description: Production config enables springdoc, while security only permits Swagger anonymously outside prod.
Why it matters: Authenticated users may still access production docs, and policy is unclear.
Potential impact: Increased information disclosure surface.
Recommended solution: Disable Swagger generation in production or restrict to admin/VPN.
Estimated effort: Small.
Related issues: 13.

### 15. SMS production provider is not implemented

Severity: High
Category: Availability / Authentication
Files affected: `src/main/java/com/makershub/notification/SmsService.java`
Relevant locations: `SmsService.java:8-15`, `SmsService.java:17-78`
Description: Active SMS service only logs messages; real Africa's Talking implementation is commented out.
Why it matters: OTP login depends on SMS delivery.
Potential impact: Users cannot authenticate in production unless dev responses expose OTPs or operators read logs.
Recommended solution: Implement provider-backed SMS with profile-specific stubs and delivery failure handling.
Estimated effort: Medium.
Related issues: 1.

### 16. Soft delete is manual and inconsistent

Severity: Medium
Category: Database / Maintainability
Files affected: repositories and services
Relevant locations: `UserRepository.java:11-17`, `FactoryRepository.java:15-19`, `UserService.java:48`, `V1__init_schema.sql:21,42,66,89,110`
Description: Many tables have `deleted_at`, but repositories use a mix of soft-delete-aware and normal methods.
Why it matters: Deleted data may leak or block re-creation unintentionally.
Potential impact: Data integrity bugs and privacy issues.
Recommended solution: Adopt Hibernate soft-delete annotations/filters or enforce repository naming conventions with tests.
Estimated effort: Medium.
Related issues: 6.

### 17. Duplicate escrow lookup and missing uniqueness semantics

Severity: Medium
Category: Database / Payments
Files affected: `PaymentService.java`, `V1__init_schema.sql`
Relevant locations: `PaymentService.java:74-80`, `V1__init_schema.sql:114-130`
Description: Service treats escrow by order as unique but schema does not make `order_id` unique. Service also calls `findByOrderId` twice.
Why it matters: Race conditions or retries can create ambiguous escrow records.
Potential impact: Payment confusion and incorrect status updates.
Recommended solution: Add unique active escrow constraint or explicit payment-attempt model. Use one optional lookup.
Estimated effort: Small to medium.
Related issues: 2, 10.

### 18. CI lacks security and image scanning

Severity: Medium
Category: DevOps / Dependencies
Files affected: `.github/workflows/ci-cd.yml`, `Dockerfile`, `pom.xml`
Relevant locations: `.github/workflows/ci-cd.yml:18-32`, `Dockerfile:1-17`
Description: CI runs Maven verify and deploys, but no dependency vulnerability scan, license scan, image scan, or SBOM generation is configured.
Why it matters: Supply-chain issues can reach production unnoticed.
Potential impact: Known vulnerable dependencies or base images in production.
Recommended solution: Add Dependabot, dependency review, Trivy/Grype image scanning, and SBOM generation.
Estimated effort: Small to medium.
Related issues: none.

### 19. Observability is incomplete

Severity: Medium
Category: DevOps / Operations
Files affected: `application.yml`, docs
Relevant locations: `application.yml:64-70`, `docs/DEPLOYMENT.md:33-49`
Description: Actuator and Prometheus endpoint are enabled, but no dashboards, alerts, correlation IDs, tracing, or runbooks are documented.
Why it matters: Production incidents require fast diagnosis.
Potential impact: Longer outages and unclear payment/order incident handling.
Recommended solution: Add structured logs, request IDs, metrics dashboards, alerts, traces, and operational runbooks.
Estimated effort: Medium.
Related issues: 10.

### 20. Review rating constraints are incomplete

Severity: Low
Category: Database / Data Integrity
Files affected: `V1__init_schema.sql`, `ReviewRequest.java`
Relevant locations: `V1__init_schema.sql:142-145`, `ReviewRequest.java:15-33`
Description: DTO constrains all rating fields, but DB only constrains `overall_rating`.
Why it matters: Future imports or alternate writers can persist invalid quality/timeliness/communication ratings.
Potential impact: Invalid analytics and display values.
Recommended solution: Add DB check constraints for all rating columns.
Estimated effort: Small.
Related issues: 6.

## Top 10 Quick Wins

1. Remove OTP and SMS body logging.
2. Require Paystack secret in production.
3. Add bid-list ownership check.
4. Add `IllegalArgumentException` and `NumberFormatException` handlers.
5. Change OTP to six digits.
6. Re-enable content type options.
7. Disable or admin-gate Swagger in production.
8. Fix stale API docs.
9. Add one test for bid-list authorization.
10. Remove duplicate escrow lookup.

## Technical Debt Summary

The largest debt areas are security hardening, production provider readiness, object-level authorization tests, manual soft-delete discipline, and missing integration coverage. The codebase is modular enough to improve incrementally without a rewrite.

## Production Readiness Assessment

Current state: not production-ready for a payment-enabled marketplace. It is suitable for controlled development/staging after secrets are configured, but production launch should wait for remediation of critical/high issues, monitoring, and integration tests.

