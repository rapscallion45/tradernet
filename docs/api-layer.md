# API layer

The `api/` module is the HTTP boundary for clients.

## What it contains

- `TradernetApplication`: Jakarta REST bootstrap for the API.
- `AuthenticationFilter`: request-level authentication handling.
- Resource classes grouped by domain (auth, users, groups, roles, passwords, orders, trades, signals, market, health).
- `MarketStreamEndpoint`: websocket entry point for market streaming use-cases.
- Request/response DTOs for API contracts.

## Typical request lifecycle

1. Request enters a resource endpoint.
2. Authentication/authorization is applied where required.
3. Resource delegates to one or more service modules.
4. Service results are transformed into DTOs.
5. API returns JSON response.

## Why facade-service matters here

Some workflows span multiple domain services. The API layer can use `facade-service` to avoid duplicating orchestration logic directly in resource classes.
