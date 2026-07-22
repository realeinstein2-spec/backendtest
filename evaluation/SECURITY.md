# Security Evaluation

Score: 63/100

High-risk findings:

- OTPs are logged by `SmsService` because the OTP body is emitted at INFO level.
- `PaymentService` simulates payment initiation when `makershub.paystack.secret-key` is blank.
- Paystack webhook verification uses the configured secret even if blank, making blank-secret deployments unsafe.
- `BidService.listBidsForJob` does not verify that the SME owns the job.
- Auth endpoints lack rate limiting for registration, login, OTP verification, refresh, and payment initiation.
- Refresh tokens are stateless, long-lived, and not rotated/revocable.

Additional issues:

- OTP is 4 digits despite comments suggesting 6 digits.
- `SecurityConfig` disables `X-Content-Type-Options`.
- `application-prod.yml` enables Swagger generation in production.
- `SmsService` production SMS code is commented out, creating a likely production-delivery gap.
- Generic exception logging may capture sensitive request context indirectly.

Recommendations:

- Remove OTP/body logging and use a real provider abstraction by profile.
- Enforce nonblank Paystack secret in production.
- Add per-phone/IP rate limits and lockouts for auth and OTP.
- Add ownership checks to every object-listing path.
- Add refresh token persistence, rotation, and revocation.
- Re-enable content type options and gate Swagger by environment.

