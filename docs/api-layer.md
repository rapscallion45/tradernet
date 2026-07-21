# API layer

The `api/` module is the HTTP boundary for clients.

## What it contains

- `TradernetApplication`: Jakarta REST bootstrap for the API.
- `AuthenticationFilter`: request-level authentication handling with fail-closed role checks for protected REST resources, delegating session and authorization policy lookup to `user-service`.
- `AuthenticationResponseFilter`: applies no-store cache policy to every auth response, including mapped validation and server errors.
- Resource classes grouped by domain (auth, users, groups, roles, orders, trades, market, health).
- `MarketStreamEndpoint`: cookie-authenticated websocket entry point for market streaming use-cases.
- `MarketStreamDeliveryService`: bounded asynchronous per-session delivery queues for market event conversion, serialization, and websocket sends.
- `ExceptionMapper` implementations that turn validation and domain failures into standard JSON errors and unexpected exceptions into a safe JSON 500 response.
- Request/response DTOs exposed as API contracts and owned by the service module that builds them.

## Typical request lifecycle

1. Request enters a resource endpoint.
2. Authentication/authorization is applied to every non-public REST resource with the `tradernet_session` cookie. Authorization selects the longest matching normalized path and prefers an exact HTTP method over a wildcard.
3. Login and password-reset calls pass only the servlet container's remote address to the service throttle. The API does not interpret client-provided forwarding headers; trusted-proxy address rewriting belongs in Undertow configuration.
3. Resource delegates to one or more service modules.
4. Service results are returned as DTOs owned by the relevant service module.
5. API returns JSON response.

The websocket boundary is not processed by the JAX-RS request filter, so `MarketStreamEndpoint` validates the request origin, resolves the same `tradernet_session` cookie, and applies the persisted `GET market` role policy before subscribing. Live connections are registered for immediate logout closure and 30-second session/account/role revalidation that does not refresh idle-session activity. Publisher callbacks only filter and enqueue events. A bounded per-session queue performs conversion and serialization on a managed asynchronous EJB boundary, then advances from asynchronous send-completion callbacks. Slow clients drop their oldest pending events rather than blocking market ingestion or managed workers, and queue state is removed when the endpoint closes.

## Service Boundaries

Workflows that span multiple domains should be pushed into the relevant service modules instead of being implemented directly in resource classes.

Resources should not inject DAOs directly. Keep resource code limited to authentication/session lookup, parameter parsing, request validation, status-code selection, and delegation to service-layer EJBs.

Auth cookies are HttpOnly, SameSite=Strict, non-persistent for login sessions, and Secure by default. Local HTTP must opt out explicitly. HTTP 429 responses include `Retry-After`; authentication responses must remain non-cacheable. See [authentication security](authentication-security.md).

WebSockets accept the exact request origin by default. Deployments behind a proxy or with multiple trusted frontend origins must configure a comma-separated allowlist through `TRADERNET_AUTH_WEBSOCKET_ALLOWED_ORIGINS` or `tradernet.auth.websocket.allowedOrigins`; missing, malformed, and untrusted browser origins are rejected.

Request DTO constraints are enforced with Jakarta Bean Validation. Common constraint failures and domain-specific access-control/market-context validation failures are converted to the standard `ApiErrorDto` response by registered JAX-RS exception mappers.

The general `WebApplicationException` mapper preserves headers from the original JAX-RS response while replacing only the body. This retains protocol requirements such as `Allow`, `WWW-Authenticate`, and `Retry-After`.

An unexpected application exception is logged server-side with a generated reference id. The client receives a generic `500` `ApiErrorDto`, the same id in `error.referenceId`, and an `X-Error-Reference` header; internal exception messages and stack traces are never returned.
