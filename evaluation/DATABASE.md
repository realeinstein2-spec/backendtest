# Database Evaluation

Score: 72/100

Strengths:

- Flyway migrations are present.
- Schema has useful constraints and indexes.
- PostGIS extension and spatial index support matching factories.
- Monetary values use numeric precision/scale.

Main issues:

- `reviews.order_id` is unique, but service logic permits one review per reviewer per order. This prevents both parties from reviewing the same order.
- Soft delete is modeled with `deleted_at`, but enforcement is manual and inconsistent.
- `otp_verifications.otp_code` stores plaintext OTPs.
- `escrow_transactions.order_id` is not unique, while business logic treats one active escrow per order.
- Some partial indexes duplicate earlier non-partial indexes.

Recommendations:

- Replace review unique order constraint with unique `(order_id, reviewer_id)`.
- Add global soft-delete strategy or strict repository conventions.
- Store OTP hashes with expiry and attempts.
- Enforce one active escrow per order, or model retry attempts explicitly.
- Review index overlap and keep high-value partial indexes.

