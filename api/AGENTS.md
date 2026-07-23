# API module agent guide

## Responsibility
`api/` is the client-facing Jakarta REST and websocket boundary.

## Key areas
- `src/main/java/com/tradernet/api/resources`: REST resources such as auth, orders, market, portfolio, and health.
- `MarketStreamEndpoint`: websocket stream for market bars and AI signals.
- `TradernetApplication`: JAX-RS application registration.

## Conventions
- Keep resources focused on HTTP concerns: authentication/session lookup, request validation, response shaping, and delegation.
- Inject other modules only through focused `@Local` interfaces. Do not import concrete service EJB implementations, JPA types, DAOs, or market engine/stream internals.
- Do not put business calculations, aggregation, valuation, or DTO construction workflows in resources. Add or extend an EJB service in `services/*` and have the resource delegate to it.
- Put business rules in `services/*` rather than directly in resource classes.
- Use DTOs from service modules for API payloads where possible.
- For protected endpoints, preserve `tradernet_session` cookie behavior unless explicitly changing auth.
- Store only hashes of session/reset bearer tokens server-side. Login cookies must remain non-persistent, HttpOnly, SameSite=Strict, and Secure by default; local HTTP requires explicit configuration. Apply no-store headers to all authentication responses.
- Pass `HttpServletRequest.getRemoteAddr()` to authentication throttles and leave forwarded-address interpretation to explicitly trusted Undertow proxy configuration. Never trust `X-Forwarded-*` directly. Return HTTP 429 with `Retry-After` when service throttles reject a request.
- Shape HTTP failures with `ApiErrorDto`, `ApiErrors`, or registered JAX-RS `ExceptionMapper` implementations. Do not return plain string error bodies or empty 4xx responses.
- Keep a final `ExceptionMapper<Exception>` registered after domain-specific mappers. Unexpected failures must log a generated reference id and return only a generic JSON 500 response carrying the same reference in its body/header.
- Apply Jakarta Bean Validation to request bodies and centralize its 400 response in an `ExceptionMapper`. Keep manual validation only for rules that require authenticated identity, service state, or cross-field domain decisions.
- Reuse validation constants owned by shared domain types for symbols, currencies, intervals, and similar values. Apply them consistently across equivalent query parameters and DTO fields; do not silently coerce invalid client input to a default.
- Pass both HTTP method and normalized path to authorization policy lookup. Policy resolution must use the longest matching path and prefer an exact method over a wildcard so parent permissions cannot weaken a specific route.
- Declare REST route names, path prefixes, methods, and default role assignments in the API-owned authorization policy catalog. Register them through the user-service policy contract; do not move endpoint knowledge into identity bootstrap code.
- WebSocket endpoints sit outside JAX-RS filters: validate the configured or same origin, resolve the current session user, apply the matching service-owned role policy before subscribing, register the connection for logout and periodic revocation, and never let validation refresh session idle activity.
- Build the JAX-RS `SecurityContext` from canonical application roles only. Do not fall back to container roles, which would create a second authorization authority.
- Preserve status-specific headers when standardizing `WebApplicationException` bodies; build from the original response rather than recreating it from the status alone.
- Websocket event callbacks may enqueue only. Use a bounded, registered per-session delivery queue for conversion, serialization, and sends; never recreate a queue after endpoint cleanup because a late callback arrived.
- Subscribe through `LiveMarketSubscriptionService` and retain only its subscription id. The market module owns bounded live-symbol capacity, symbol routing, publisher registration, and reference release, including partial-open failures.
- Advance websocket queues from asynchronous send-completion callbacks. Do not occupy managed EJB workers with `Future#get` while waiting for browser sends, and account for bounded-queue drops.
- Keep response contracts canonical and semantic. Do not expose duplicate identifier aliases or backend-formatted display strings.
- When adding or changing endpoints, update `docs/application-guide.md` and any smoke-check examples if the contract changes.
