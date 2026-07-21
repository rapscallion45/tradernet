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
- Keep all JPA, JPQL, SQL, `EntityManager`, `DataSource`, and JDBC access inside DAO implementations in `data-model`. Service EJBs own policy and orchestration and inject DAO interfaces; they must not double as repositories.
- Keep persistence-oriented services/DAOs transactional; mark HTTP clients, caches, event publishers, websocket lifecycle helpers, and other non-database collaborators as non-transactional.
- Keep blocking IO out of singleton locks and request/websocket hot paths where practical. Use `@Asynchronous` EJB methods, caches, queues, or snapshot-style responses for slow external calls, websocket startup/resync, forecast refreshes, and background persistence.
- Plain Java websocket listeners must hand recovery/reconnect work back to an intercepted EJB method. Do not call synchronous REST snapshot recovery or other blocking provider IO from an exchange callback thread.
- Revalidate account eligibility when resolving every persisted session, not only during login. Persist and cap failed-login counters, reset them on successful authentication/password reset, consume reset tokens atomically, and schedule expired-token cleanup.
- Fetch historical FX data in provider-supported date ranges and warm the conversion cache before portfolio history loops. Cache provider failures/static fallbacks briefly so they cannot become process-lifetime financial data.
- Keep non-critical enrichment out of the write path for user commands. For example, persist order placement/fill state first, then populate advisory forecast fields asynchronously.
- Keep DTOs, JPA entities, value objects, pure helpers, and per-symbol runtime objects as plain Java classes unless container lifecycle, injection, transactions, or concurrency are needed.
- Separate write DTOs from read DTOs when response models include derived fields; do not add no-op setters just to make read models deserialize as requests.
- Treat data availability as explicit state when zero is a valid domain value. Partial updates must preserve per-field availability instead of inferring presence from numeric magnitude.
- Own domain calculations in service/backend modules rather than duplicating formulas in API resources or frontend code.
- Put multi-step workflows such as portfolio construction, valuation, history generation, and response DTO assembly behind service-layer EJBs; keep API resources as thin delegators.
- Portfolio services must preserve signed position semantics: long holdings are positive quantities, short/open SELL positions are negative quantities, and valuation/history should include both unless short selling is explicitly disabled.
- Reuse persistence-neutral shared values and normalization from `domain-model` for concepts used by multiple services. Do not depend on an unrelated service module only to reuse a helper.
- Avoid coupling unrelated service modules directly unless there is an explicit orchestration reason.
- If a service change affects persistence, coordinate with `data-model/` schema/entity/DAO changes.
- Do not run schema creation or migration DDL from service-layer startup code. `data-model` schema SQL and migrations own database shape.
- If a service change affects UI-visible behavior, update API DTOs/types and docs.
