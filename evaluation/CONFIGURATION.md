# Configuration Evaluation

Score: 68/100

Strengths:

- Environment variables are used for secrets.
- JWT secret has no default in the main config.
- Separate dev/test/prod profiles exist.
- Hikari pool and actuator settings are configured.

Main issues:

- `SPRING_PROFILES_ACTIVE` defaults to `dev`, which can expose dev behavior if not explicitly set.
- Paystack and Cloudinary secrets default to blank.
- `application-prod.yml` enables Swagger/OpenAPI.
- CORS default allows localhost.
- Dev profile enables SQL logging.

Recommendations:

- Require explicit profile in production deployments.
- Fail startup for required production secrets.
- Keep Swagger disabled or restricted in production.
- Make production CORS origins mandatory.
- Add config validation with `@ConfigurationProperties` and validation annotations.

