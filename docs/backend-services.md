# Backend services

Business logic is split into focused modules under `services/`.

## Core service modules

- `order-service`: order creation, lifecycle operations, asynchronous market-insight enrichment, order response enrichment, and persistence-neutral portfolio projections.
- `portfolio-service`: user-scoped position replay, current valuation, portfolio history, and portfolio response contracts.
- `trade-service`: focused trade command and user-scoped query contracts implemented by `TradeService`.
- `user-service`: separate profile and credential/session workflows, authorization policy persistence/evaluation, admin group/role workflows, and identity bootstrap routines.
- `currency-conversion-service`: currency conversion support, code abstractions, quote-currency resolution, and exchange-rate lookup.
- `market-ai-service`: market bars, signal scoring, forecasting integration, order-book depth, market context, and display-currency market-data views.

## Service bean conventions

Service and DAO collaborators are container-managed Jakarta EJBs. Use `@Stateless` for business operations, persistence helpers, and external gateway/client calls that should not hold request-specific state. Use `@Singleton` only when a service intentionally owns application-wide shared state, cache, registry, lifecycle, or subscriptions; declare locking or bean-managed concurrency explicitly for those singletons. API resources and cross-module collaborators inject focused `@Local` interfaces rather than concrete bean classes. Service beans do not expose `@LocalBean` views; DAO access stays behind service-layer EJBs.

Each module reads runtime properties through one typed configuration singleton. Configuration values are parsed, bounded, and defaulted at startup; business methods consume typed getters and plain per-symbol engines receive immutable settings.

Durable Java paths follow `REST resource -> service EJB -> DAO EJB -> database`. Only DAO implementations in `data-model` may use `EntityManager`, JPQL, SQL, `DataSource`, or JDBC. Pure calculations, caches, and external-provider gateways do not require artificial DAOs. Shared persistence-neutral concepts such as canonical market symbols live in `domain-model`, avoiding dependencies on unrelated service modules.

Keep persistence-oriented services and DAOs transactional. Mark HTTP clients, in-memory caches, event publishers, websocket lifecycle helpers, and other non-database collaborators with `@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)` so external IO and cache reads do not run inside unnecessary container transactions.

Keep DTOs, JPA entities, value objects, pure scoring/domain helpers, and per-symbol runtime objects as plain Java classes unless the application server needs to manage lifecycle, transactions, injection, or concurrency for them.

## Market AI service

The API consumes focused market EJB contracts instead of a catch-all facade: `MarketBarProvider`, `MarketSignalProvider`, `MarketSymbolProvider`, `MarketForecastProvider`, `MarketContextOperations`, `MarketDataViewProvider`, and `LiveMarketSubscriptionService`. `MarketForecastService` provides stale-while-revalidate forecast snapshots and performs Python/Ollama work asynchronously. `MarketBarStorageService` maps closed bars and delegates durable writes to `MarketBarDao` in `data-model`. Live trade-stream startup/reconnect and order-book startup/gap resync work is requested through asynchronous EJB methods so Java websocket callbacks and API open paths do not block on external exchange connections. Trade-stream runtimes are bounded and reference-counted behind the subscription manager; order-book runtimes have a separate capacity limit and idle eviction. `MarketDataViewService` composes market data with currency conversion for bars and order-book snapshots.

`CurrencyConversionService` applies conversion policy over `FxRateGateway` and `FxRateCache`. Historical rates are prefetched as provider-supported date ranges before bars, orders, or portfolio history are mapped. The cache stores only provider-sourced values; absent authoritative data raises `CurrencyConversionUnavailableException`, which the API maps to HTTP 503.

`user-service` revalidates account state whenever a persisted auth session is resolved. Failed logins are serialized with a user-row lock and create a persisted, automatically expiring `lockoutUntil` deadline at the configured threshold. Cluster-wide source throttles use short-lived database buckets. Password policy, Unicode normalization, Argon2id hashing, BCrypt migration, blocklist checks, and uniform unknown-user work are centralized in `PasswordSecurityService`. Sessions have absolute and idle expiry. One reset row per user is issued and consumed with consistent lock ordering, and token consumption is atomic with the password update. Security outcomes are emitted through `AuthenticationAuditService`; see [authentication security](authentication-security.md).

Authorization rules are read from committed database state with a targeted query over only the request path and its parent prefixes. They are resolved by specificity: longest normalized path first, then exact HTTP method before the canonical `*` wildcard method. This prevents parent-resource roles from weakening a more-specific route, avoids loading the complete policy graph for every request, and avoids stale node-local permission caches after role changes.

The API owns its route policy catalog and registers definitions through `AuthorizationPolicyRegistrationService`. `SystemBootstrapService` owns only identity primitives such as built-in roles, groups, and optional bootstrap accounts, so the user module does not hardcode REST endpoint paths.

`market-ai-service` is structured into subpackages:

- `context`: market context registration, hydration, and manual update handling.
- `stream`: inbound exchange trade stream client.
- `engine`: bar aggregation, feature generation, and event publishing.
- `forecast`: cache-first forecast service plus Python forecasting and Ollama narrative clients.
- `scoring`: pluggable scoring strategies (rule-based and linear model).
- `orderbook`: Binance connection/reconciliation state, isolated payload parsing, and snapshot assembly.
- `model`: bars, trades, features, intervals, signal side, and signal payloads.

This separation keeps exchange ingestion, market context, order-book handling, forecasting, feature engineering, and scoring loosely coupled.

The API test suite includes `ArchitectureBoundaryTest`, which scans compiled modules with ArchUnit. New cross-module dependencies must preserve explicit local contracts and the established API/service/DAO boundaries.

## Why this split helps

- Clear ownership by domain area.
- Easier testing and future replacement of a specific service module.
- Better long-term maintainability than putting all business logic in one API module.
