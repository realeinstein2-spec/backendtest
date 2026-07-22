# Production 95-100 Readiness Plan

Target: raise MakersHub backend from the current evaluated state to 95-100% production readiness.

Current estimated state:

- Overall backend health: 72/100
- Security: 63/100
- Performance: 70/100
- Maintainability: 74/100
- Scalability: 68/100
- Production readiness: not ready

## Definition Of 95-100% Production Readiness

The backend can be considered 95-100% production-ready when:

- Critical and high security risks are fixed.
- Payment, OTP, auth, authorization, and order lifecycle behavior are verified with integration tests.
- Production configuration fails safely when required secrets are missing.
- Monitoring, logging, backups, rollback, and incident response are ready.
- Frontend and backend flows are tested end to end in staging using production-like settings.
- Remaining known issues are low-risk, documented, and accepted.

## Must-Fix Before Production

### 1. Remove OTP And SMS Body Logging

Target score impact: Security +8 to +12

Required work:

- Stop logging OTPs and full SMS message bodies.
- Redact phone numbers in logs.
- Keep test OTP visibility only through isolated dev/test behavior if absolutely required.
- Ensure production logs never contain OTPs.

Acceptance criteria:

- Login/register OTP flow works.
- Logs show no OTP values.
- Logs show no full SMS body.

### 2. Implement Real Production SMS Delivery

Target score impact: Security +3, production readiness +8

Required work:

- Replace the current log-only SMS service with a real provider implementation.
- Keep a dev/test stub separate from production.
- Handle provider failures with clear user-facing errors and operator logs.

Acceptance criteria:

- Production OTP arrives by SMS.
- Failed SMS delivery does not silently pass.
- Provider secrets are not logged.

### 3. Make Paystack Production Configuration Fail-Safe

Target score impact: Security +8, production readiness +10

Required work:

- Fail startup in production if `PAYSTACK_SECRET_KEY` is blank.
- Reject Paystack webhooks when secret is blank.
- Disable simulated payment behavior outside dev/test.
- Use Paystack test keys in staging and live keys only in production.

Acceptance criteria:

- Blank production Paystack secret blocks startup or payment feature initialization.
- Webhooks with invalid signatures fail.
- Payment cannot be simulated in production.

### 4. Fix Bid Listing Ownership Authorization

Target score impact: Security +5

Required work:

- In bid listing, verify authenticated SME owns the job before returning bids.
- Add test for another SME trying to list bids for a job they do not own.

Acceptance criteria:

- Job owner can list bids.
- Other SMEs receive forbidden/unauthorized response.
- Admin behavior, if needed, is explicit.

### 5. Add Rate Limiting

Target score impact: Security +8

Required work:

- Rate limit by phone and IP for:
  - register
  - login
  - OTP verify
  - token refresh
  - payment initiate
  - webhook endpoint
- Add lockout/backoff after repeated failed OTP attempts.
- Record rate-limit events for monitoring.

Acceptance criteria:

- Repeated login/OTP attempts are throttled.
- Legitimate retry behavior remains usable.
- Rate-limit response format is documented.

### 6. Strengthen OTP Design

Target score impact: Security +5

Required work:

- Move from 4-digit OTP to at least 6 digits.
- Store OTP hashes instead of plaintext OTPs.
- Keep short expiry.
- Delete or invalidate old OTPs on new request.

Acceptance criteria:

- OTP values are not recoverable from database rows.
- Expired and reused OTPs fail.
- Attempt count still works.

### 7. Fix Review Database Constraint

Target score impact: Overall +3, maintainability +3

Required work:

- Replace `reviews.order_id UNIQUE` with unique `(order_id, reviewer_id)`.
- Add migration for existing deployments.
- Add tests proving both order parties can submit one review each.

Acceptance criteria:

- SME can review factory.
- Factory can review SME on same order.
- Same reviewer cannot review same order twice.

### 8. Add Controller, Security, Payment, And Webhook Integration Tests

Target score impact: Testing +20, overall +8

Required work:

- Add controller tests for all endpoints.
- Add authorization tests by role and object ownership.
- Add JWT filter tests.
- Add Paystack webhook signature and amount verification tests.
- Add order lifecycle integration tests.
- Add dispute and message access tests.

Acceptance criteria:

- CI fails on auth/ownership regression.
- Payment webhook behavior is verified.
- Main happy path is tested end to end.

### 9. Production Swagger Policy

Target score impact: Security +2

Required work:

- Disable Swagger/OpenAPI UI in production, or restrict to admin/VPN.
- Keep Swagger enabled in dev/staging for testing.

Acceptance criteria:

- Public production users cannot access Swagger.
- Staging testers can still use Swagger.

### 10. Production Configuration Validation

Target score impact: Production readiness +8

Required work:

- Validate required env vars at startup:
  - `JWT_SECRET`
  - `DATABASE_*`
  - `PAYSTACK_SECRET_KEY`
  - SMS provider credentials
  - production CORS origins
- Default profile should not accidentally be `dev` in production.
- Validate CORS origins are explicit production domains.

Acceptance criteria:

- Missing required production config fails fast.
- No localhost CORS in production.
- No blank production secrets.

## Strongly Recommended For 95-100%

### 11. Refresh Token Rotation And Revocation

Required work:

- Persist refresh token IDs or token families.
- Rotate refresh tokens on every refresh.
- Revoke on logout/suspension/password reset.
- Detect reuse of old refresh tokens.

Why it matters:

- Reduces damage from stolen refresh tokens.

### 12. Structured Logging And Sensitive Data Controls

Required work:

- Add request correlation IDs.
- Use structured JSON logs in production.
- Mask phone numbers and payment references where appropriate.
- Prevent secrets, OTPs, and raw sensitive payloads from logs.

### 13. Observability And Alerts

Required work:

- Metrics dashboard for:
  - request latency
  - error rate
  - auth failures
  - OTP failures
  - payment webhook failures
  - SMS provider failures
  - DB pool usage
  - async queue usage
- Alerts for:
  - high 5xx rate
  - webhook verification failures
  - payment/order state mismatch
  - SMS outage
  - DB connection saturation

### 14. Backup, Restore, And Rollback Drills

Required work:

- Automated database backups.
- Restore test before launch.
- Migration rollback plan.
- Application rollback plan.
- Incident runbook for failed payment webhook and stuck orders.

### 15. Performance Hardening

Required work:

- Fix ineffective private `@Async` in `JobService`.
- Add fetch joins/entity graphs/projections for list endpoints.
- Batch scheduled auto-completion.
- Add pagination limits.
- Run basic load tests for auth, job listing, bidding, messaging, and order flows.

### 16. Dependency And Image Security

Required work:

- Add dependency vulnerability scanning.
- Add Docker image scanning.
- Add Dependabot or equivalent.
- Generate SBOM.
- Remove unused dependencies such as WebFlux if confirmed unused.

### 17. API Contract Stabilization

Required work:

- Align docs with `/api/v1`.
- Correct auth docs to show OTP-before-token flow.
- Document every enum and state transition.
- Standardize success and error response DTOs.
- Add OpenAPI examples for frontend integration.

### 18. Frontend-Backend Staging Certification

Required work:

- Frontend tests complete the full lifecycle:
  - register
  - verify OTP
  - create factory
  - admin verify
  - create job
  - submit bid
  - accept bid
  - initiate payment
  - webhook to escrow
  - production
  - quality check
  - delivery
  - review
  - dispute
  - messaging
- Test every role.
- Test wrong-role and wrong-owner access.
- Test payment pending/failure states.

## Suggested Score Path

After fixing production blockers:

- Overall backend health: 84-88
- Security: 82-88
- Performance: 74-78
- Maintainability: 78-82
- Scalability: 74-80
- Production readiness: controlled launch possible

After completing strong recommendations:

- Overall backend health: 95-100
- Security: 94-100
- Performance: 90-96
- Maintainability: 90-96
- Scalability: 90-96
- Production readiness: strong production launch readiness

## Final Production Gate Checklist

- [ ] OTPs are never logged.
- [ ] Real SMS provider works.
- [ ] Paystack secret is mandatory in production.
- [ ] Paystack webhook tests pass.
- [ ] Bid ownership authorization is fixed.
- [ ] Auth and payment rate limits are active.
- [ ] Review schema supports both party reviews.
- [ ] Swagger is disabled or restricted in production.
- [ ] Production config validation fails fast.
- [ ] Controller/security integration tests pass.
- [ ] End-to-end staging flow passes.
- [ ] Monitoring dashboards exist.
- [ ] Alerts are configured.
- [ ] Backups are enabled.
- [ ] Restore has been tested.
- [ ] Rollback plan exists.
- [ ] Frontend has implemented all required role-based flows.

