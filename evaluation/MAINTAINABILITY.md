# Maintainability Evaluation

Score: 74/100

Strengths:

- Domain concepts are separated into focused packages.
- Service tests cover several business rules.
- Flyway migrations reduce schema drift risk.
- Exception and response DTOs centralize common behavior.

Technical debt:

- Repeated current-user access logic.
- Manual soft-delete filtering.
- Mixed historical comments and stale docs.
- Service classes own too many responsibilities.
- Missing integration tests around security and payment behavior.
- Incomplete provider abstraction for SMS/push/payment failure handling.

Recommendations:

- Introduce small shared application services for auth context, notification dispatch, and external payment gateways.
- Add architectural tests or package rules.
- Keep controllers thin and move object-level authorization into tested domain services.
- Track TODO-like historical labels outside source comments.

