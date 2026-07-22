# Priority Actions

## Top 10 Highest-Priority Issues

1. Critical: OTP/SMS content is logged at INFO in `SmsService`.
2. Critical: Payment/webhook behavior is unsafe when Paystack secret is blank.
3. High: Any SME can list bids for any job through `BidService.listBidsForJob`.
4. High: Auth and OTP flows have no rate limiting.
5. High: Four-digit plaintext OTPs are weak for production.
6. High: Review schema blocks the second party review.
7. Medium: Private `@Async` in `JobService` does not execute asynchronously.
8. Medium: Invalid UUID/decimal input can become 500 errors.
9. Medium: Refresh tokens are long-lived, stateless, and non-revocable.
10. Medium: Missing controller/security/payment integration tests.

## Top 10 Quick Wins

1. Remove OTP/body logging from `SmsService`.
2. Add an `IllegalArgumentException` handler returning 400.
3. Add ownership check before listing bids.
4. Re-enable content type options in `SecurityConfig`.
5. Remove or gate Swagger in production.
6. Make production Paystack secret mandatory.
7. Change OTP generation to six digits.
8. Replace duplicate escrow lookup with a local optional.
9. Fix API docs for `/api/v1` and OTP login response.
10. Add tests for the bid-list authorization bug.

Estimated effort to resolve all identified issues: 4-7 engineering weeks, depending on desired depth of integration testing, token revocation design, observability setup, and production SMS/payment provider hardening.

## Release Decision Matrix

### Must Implement Before Swagger Testing

These block reliable API testing because Swagger results may be misleading or unsafe:

1. Stop OTP/SMS body logging or restrict it to a clearly isolated local-only profile.
2. Confirm the intended testing profile and secrets: `SPRING_PROFILES_ACTIVE=dev` or a dedicated staging profile, never accidental production.
3. Confirm Paystack test-mode behavior. If real Paystack test keys are not configured, testers must not treat payment results as valid escrow behavior.
4. Fix or explicitly document the current OTP flow: `register/login` returns pending auth, then `verify` returns tokens.
5. Seed or create test users for all roles: `SME_OWNER`, `FACTORY_OWNER`, `ENTERPRISE`, and `ADMIN`.

### Must Implement Before Production

These are production blockers:

1. Remove OTP/SMS body logging completely.
2. Require nonblank Paystack secret and real payment provider configuration in production.
3. Add ownership validation to bid listing.
4. Add rate limiting for register, login, OTP verify, refresh, payment initiate, and webhook endpoints.
5. Fix review uniqueness so both order parties can review.
6. Implement real SMS delivery and failure handling.
7. Add payment webhook integration tests.
8. Gate or disable Swagger in production unless admin/VPN restricted.
9. Add operational monitoring, alerts, and payment/order incident runbooks.
10. Validate production CORS, JWT, database, backup, and rollback settings.

### Can Note And Defer During Testing

These should be tracked but do not need to block Swagger testing:

1. Refresh token rotation and logout revocation.
2. N+1 query optimization after baseline performance testing.
3. Image vulnerability scanning and SBOM, unless organizational policy requires it before staging.
4. Full tracing/correlation IDs.
5. Refactoring duplicated authenticated-user lookup.
6. Cleaning historical comments.
7. Caching public/stable reads.
8. Full load testing beyond basic response-time checks.
