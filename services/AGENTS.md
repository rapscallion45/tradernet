# Services agent guide

## Responsibility
`services/` contains business logic modules. API resources should delegate here rather than implementing domain behavior directly.

## Module map
- `order-service`: order lifecycle, order DTO mapping, portfolio valuation, and order history support.
- `trade-service`: trade execution domain logic.
- `user-service`: users, bootstrap data, security-related user state.
- `currency-conversion-service`: currency conversion, quote-currency resolution, and exchange-rate lookup. Do not couple this module to market model classes.
- `market-ai-service`: market bars, display-currency market data views, signal scoring, forecasting integration, order-book depth, and market context.

## Conventions
- Keep service APIs stable for `api/` callers.
- Prefer container-managed EJBs for service and DAO collaborators. Use `@Stateless` for operation-oriented services and `@Singleton` only for application-wide shared state, cache, registry, lifecycle, or subscriptions.
- Keep persistence-oriented services/DAOs transactional; mark HTTP clients, caches, event publishers, websocket lifecycle helpers, and other non-database collaborators as non-transactional.
- Keep DTOs, JPA entities, value objects, pure helpers, and per-symbol runtime objects as plain Java classes unless container lifecycle, injection, transactions, or concurrency are needed.
- Own domain calculations in service/backend modules rather than duplicating formulas in API resources or frontend code.
- Put multi-step workflows such as portfolio construction, valuation, history generation, and response DTO assembly behind service-layer EJBs; keep API resources as thin delegators.
- Reuse existing service-owned normalization helpers for canonical domain values such as market symbols instead of adding endpoint-local normalization.
- Avoid coupling unrelated service modules directly unless there is an explicit orchestration reason.
- If a service change affects persistence, coordinate with `data-model/` schema/entity/DAO changes.
- If a service change affects UI-visible behavior, update API DTOs/types and docs.
