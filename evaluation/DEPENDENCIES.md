# Dependencies Evaluation

Score: 70/100

Strengths:

- Uses Spring Boot dependency management.
- Modern Java 21 baseline.
- Relevant dependencies for security, validation, JPA, Flyway, WebSocket, OpenAPI, metrics, and Testcontainers.

Risks:

- No dependency vulnerability scan configuration was found.
- No license scanning was found.
- Both Spring MVC and WebFlux starters are included; WebFlux appears unused and increases footprint.
- Cloudinary dependency version should be monitored for maintenance/security.
- Spring Boot 3.3.4 may be behind current patch releases as of this evaluation date.

Recommendations:

- Add OWASP Dependency-Check, Snyk, Dependabot, or GitHub dependency review.
- Add license policy checks.
- Remove unused WebFlux if not needed.
- Keep Spring Boot and springdoc on supported patch releases after compatibility testing.

