# Performance Evaluation

Score: 70/100

Strengths:

- Hikari pool settings are configured.
- Database indexes exist for many common filters.
- Pagination is used for list endpoints.
- Paystack `RestTemplate` has connect/read timeouts.
- Scheduled order auto-completion catches per-order failures.

Main issues:

- Private self-invoked `@Async` in `JobService` means job creation can still block on factory matching and notification dispatch.
- DTO mapping accesses lazy associations and may trigger N+1 queries.
- Some repository methods return full lists for potentially large result sets, such as all bids for a job and expired orders.
- Duplicate escrow lookups occur in `PaymentService.initiatePayment`.
- No caching for stable lookups such as user details, factory profile, or public listings.

Recommendations:

- Move async work to a separate bean/event listener.
- Add `@EntityGraph`, query projections, or fetch joins for DTO list endpoints.
- Batch scheduled jobs with pagination.
- Avoid repeated repository calls inside one request.
- Add metrics around query latency, external API calls, async queue depth, and scheduler duration.

