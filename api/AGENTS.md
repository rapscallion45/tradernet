# API module agent guide

## Responsibility
`api/` is the client-facing Jakarta REST and websocket boundary.

## Key areas
- `src/main/java/com/tradernet/api/resources`: REST resources such as auth, orders, market, portfolio, and health.
- `MarketStreamEndpoint`: websocket stream for market bars and AI signals.
- `TradernetApplication`: JAX-RS application registration.

## Conventions
- Keep resources focused on HTTP concerns: authentication/session lookup, request validation, response shaping, and delegation.
- Do not put business calculations, aggregation, valuation, or DTO construction workflows in resources. Add or extend an EJB service in `services/*` and have the resource delegate to it.
- Put business rules in `services/*` rather than directly in resource classes.
- Use DTOs from service modules for API payloads where possible.
- For protected endpoints, preserve `tradernet_session` cookie behavior unless explicitly changing auth.
- Store only hashes of session/reset bearer tokens server-side. Cookies should remain HttpOnly, SameSite-aware, and Secure for HTTPS/proxy deployments while preserving explicit local HTTP development configuration.
- Shape HTTP failures with `ApiErrorDto`, `ApiErrors`, or registered JAX-RS `ExceptionMapper` implementations. Do not return plain string error bodies or empty 4xx responses.
- When adding or changing endpoints, update `docs/application-guide.md` and any smoke-check examples if the contract changes.
