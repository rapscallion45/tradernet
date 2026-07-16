# API layer

The `api/` module is the HTTP boundary for clients.

## What it contains

- `TradernetApplication`: Jakarta REST bootstrap for the API.
- `AuthenticationFilter`: request-level authentication handling with fail-closed role checks for protected REST resources, delegating session and authorization policy lookup to `user-service`.
- Resource classes grouped by domain (auth, users, groups, roles, orders, trades, market, health).
- `MarketStreamEndpoint`: cookie-authenticated websocket entry point for market streaming use-cases.
- Request/response DTOs exposed as API contracts and owned by the service module that builds them.

## Typical request lifecycle

1. Request enters a resource endpoint.
2. Authentication/authorization is applied to every non-public REST resource with the `tradernet_session` cookie.
3. Resource delegates to one or more service modules.
4. Service results are returned as DTOs owned by the relevant service module.
5. API returns JSON response.

The websocket boundary is not processed by the JAX-RS request filter, so `MarketStreamEndpoint` validates the same `tradernet_session` cookie during the websocket handshake.

## Service Boundaries

Workflows that span multiple domains should be pushed into the relevant service modules instead of being implemented directly in resource classes.

Resources should not inject DAOs directly. Keep resource code limited to authentication/session lookup, parameter parsing, request validation, status-code selection, and delegation to service-layer EJBs.
