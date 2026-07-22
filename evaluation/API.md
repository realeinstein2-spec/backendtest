# API Evaluation

Score: 71/100

Strengths:

- REST resource layout is generally understandable.
- Most request bodies use validation.
- Pagination is used on major list endpoints.
- Error responses are structured.

Main issues:

- Documentation says base URL `/v1`, while controllers use `/api/v1`.
- Docs show login returning tokens, but code returns pending OTP auth.
- Some route-level role checks are not enough without object ownership checks.
- Query parameters such as `minBudget` and `maxBudget` are strings parsed in services.
- Invalid UUIDs and invalid decimal filters can become generic 500 responses.
- Success responses sometimes use error response DTOs.

Recommendations:

- Align API docs with actual routes and auth flow.
- Validate IDs and filter parameters at the boundary.
- Add ownership tests for all object access.
- Standardize response wrappers and status codes.
- Add OpenAPI examples for error cases and pagination.

