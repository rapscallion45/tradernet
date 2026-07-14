# Backend services

Business logic is split into focused modules under `services/`.

## Core service modules

- `order-service`: order creation, lifecycle operations, order DTO mapping, and portfolio valuation/history assembly.
- `trade-service`: trade execution and user-visible persisted fill history for order placement/closure.
- `user-service`: user profile/auth-adjacent workflows and bootstrap routines.
- `currency-conversion-service`: currency conversion support and code abstractions.
- `market-ai-service`: market bars, signal scoring, forecasting integration, order-book depth, and market context.

## Service bean conventions

Service and DAO collaborators are container-managed Jakarta EJBs. Use `@Stateless` for business operations, persistence helpers, and external gateway/client calls that should not hold request-specific state. Use `@Singleton` only when a service intentionally owns application-wide shared state, cache, registry, lifecycle, or subscriptions; declare locking or bean-managed concurrency explicitly for those singletons. API resources inject EJB services/DAOs with `@EJB`.

Keep persistence-oriented services and DAOs transactional. Mark HTTP clients, in-memory caches, event publishers, websocket lifecycle helpers, and other non-database collaborators with `@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)` so external IO and cache reads do not run inside unnecessary container transactions.

Keep DTOs, JPA entities, value objects, pure scoring/domain helpers, and per-symbol runtime objects as plain Java classes unless the application server needs to manage lifecycle, transactions, injection, or concurrency for them.

## Market AI service

`MarketAiService` is the public EJB facade used by API resources and websocket endpoints. It coordinates live-symbol lifecycle, chart/signal queries, forecasting, and subscriptions while delegating exchange IO, market context hydration, order-book maintenance, in-memory history, event publishing, and bar persistence to injected collaborator beans.

`market-ai-service` is structured into subpackages:

- `context`: market context registration, hydration, and manual update handling.
- `stream`: inbound exchange trade stream client.
- `engine`: bar aggregation, feature generation, and event publishing.
- `forecast`: Python forecasting and Ollama narrative clients.
- `scoring`: pluggable scoring strategies (rule-based and linear model).
- `orderbook`: Binance aggregated L2 order-book snapshot and diff-depth handling.
- `model`: bars, trades, features, intervals, signal side, and signal payloads.

This separation keeps exchange ingestion, market context, order-book handling, forecasting, feature engineering, and scoring loosely coupled.

## Why this split helps

- Clear ownership by domain area.
- Easier testing and future replacement of a specific service module.
- Better long-term maintainability than putting all business logic in one API module.
