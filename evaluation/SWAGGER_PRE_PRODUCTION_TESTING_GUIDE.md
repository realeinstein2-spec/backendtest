# Swagger Pre-Production Testing Guide

Purpose: guide backend and frontend testers from the current state through Swagger validation before pushing for broader testing, production readiness, and post-production smoke testing.

Current Swagger URL locally: `http://localhost:8080/swagger-ui.html`

Actual API prefix in code: `/api/v1`

## Testing Phases

### Phase 0: Before Pushing For Swagger Testing

Must implement or confirm:

- Use `SPRING_PROFILES_ACTIVE=dev` or a dedicated staging/test profile.
- Use non-production database and non-production external service keys.
- Confirm whether OTP is returned in the response for testing. In dev, `register` and `login` can include `otpCode`; in production it should be null.
- Do not test with real customer phone numbers, payment cards, Ghana Card numbers, or production funds.
- Confirm that test logs are isolated because the current `SmsService` logs SMS body content.

Recommended before Swagger testing:

- Add or manually create one user for each role: SME, factory owner, enterprise, admin.
- Create a verified factory profile for factory bidding tests.
- Keep a shared test data sheet with user phone numbers, OTPs, job IDs, bid IDs, order IDs, dispute IDs, and payment references.

Can defer during Swagger testing:

- Refresh token rotation.
- Full observability stack.
- N+1 query optimization.
- Full production SMS provider rollout, if testers understand OTP delivery is test-only.

### Phase 1: Swagger Authentication Setup

1. Open Swagger UI.
2. Register an SME user:

```json
{
  "phoneNumber": "+233240000001",
  "password": "SecurePass123",
  "fullName": "Swagger SME",
  "role": "SME_OWNER",
  "ghanaCardNumber": "GHA-TEST-001",
  "region": "Ashanti",
  "town": "Kumasi"
}
```

3. Verify the SME:

```json
{
  "phoneNumber": "+233240000001",
  "otp": "<otpCode from response or test log>"
}
```

4. Copy `accessToken`.
5. Click Swagger `Authorize`.
6. Enter:

```text
Bearer <accessToken>
```

Repeat for:

- `FACTORY_OWNER`
- `ENTERPRISE`
- `ADMIN`

### Phase 2: Happy Path End-To-End Flow

Use the actual backend lifecycle:

1. SME registers and verifies OTP.
2. Factory owner registers and verifies OTP.
3. Factory owner creates factory profile.
4. Admin verifies factory profile.
5. SME creates a job.
6. Factory lists open jobs.
7. Factory submits bid.
8. SME lists bids for the job.
9. SME accepts bid, creating order.
10. SME initiates payment.
11. Paystack webhook marks escrow held and order `IN_ESCROW`.
12. Factory updates order to `IN_PRODUCTION`.
13. Factory updates order to `QUALITY_CHECK`.
14. Factory updates order to `DELIVERED`.
15. SME confirms delivery.
16. SME and factory submit reviews. Current database schema may block the second review.
17. Parties send and list messages.
18. Admin checks analytics.

Important: the backend does not let the factory manually change `PAYMENT_PENDING` to `IN_ESCROW`. That state is controlled by payment webhook handling.

### Phase 3: Negative Testing

Test every endpoint with:

- No token: expect `401`.
- Wrong role token: expect `403`.
- Valid role but wrong object owner: expect `403` or business error. Known issue: bid listing currently lacks ownership validation.
- Missing required field: expect `400 VALIDATION_ERROR`.
- Invalid enum value: expect `400`.
- Invalid UUID path value: currently may produce inconsistent errors; should be tracked.
- Expired/invalid access token: expect `401`.

### Phase 4: Production Readiness Gate

Do not mark production-ready until these are fixed:

- OTP/SMS content is not logged.
- Paystack production secret is mandatory and webhooks reject blank-secret configuration.
- Bid-list ownership check is implemented.
- Auth/payment rate limiting exists.
- Real SMS provider works.
- Review schema supports intended review behavior.
- Swagger production access policy is explicit.
- Backups, rollback, logs, metrics, and alerting are configured.

### Phase 5: After Production Deployment Smoke Tests

Use production-safe test accounts and test payment mode only if supported:

1. Health endpoint returns healthy.
2. Login flow completes for a test account.
3. Token refresh works.
4. `/api/v1/users/me` returns the expected test account.
5. Role-protected endpoints deny the wrong role.
6. Create a small test job if production policy allows it.
7. Confirm payment webhook verification with test-mode Paystack only.
8. Confirm logs do not contain OTPs, full SMS messages, secrets, or raw payment payload secrets.
9. Confirm alerts fire for failed external services.

