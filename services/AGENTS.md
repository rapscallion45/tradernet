# Services agent guide

## Responsibility
`services/` contains business logic modules. API resources should delegate here rather than implementing domain behavior directly.

## Module map
- `order-service`: order lifecycle, order DTO mapping, and order history support.
- `trade-service`: trade execution domain logic.
- `user-service`: users, bootstrap data, security-related user state.
- `signal-service`: trading signal validation/processing.
- `currency-conversion-service`: currency conversion and quote-currency resolution.
- `facade-service`: cross-service orchestration helpers.
- `market-ai-service`: market bars, signal scoring, forecasting integration, and market context.

## Conventions
- Keep service APIs stable for `api/` callers.
- Own domain calculations in service/backend modules rather than duplicating formulas in API resources or frontend code.
- Avoid coupling unrelated service modules directly unless there is an explicit orchestration reason.
- If a service change affects persistence, coordinate with `data-model/` schema/entity/DAO changes.
- If a service change affects UI-visible behavior, update API DTOs/types and docs.
