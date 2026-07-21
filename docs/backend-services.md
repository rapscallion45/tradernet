# Backend services

Business logic is split into focused modules under `services/`.

## Core service modules

- `order-service`: order creation, lifecycle operations, asynchronous order market-insight enrichment, market-price resolution, order response enrichment, and portfolio orchestration over separate position, valuation, and history collaborators.
- `trade-service`: trade execution and user-visible persisted fill history for order placement/closure.
- `user-service`: user profile workflows, persisted auth/session state, authorization policy lookup, admin group/role workflows, and bootstrap routines.
- `currency-conversion-service`: currency conversion support, code abstractions, quote-currency resolution, and exchange-rate lookup.
- `market-ai-service`: market bars, signal scoring, forecasting integration, order-book depth, market context, and display-currency market-data views.

## Service bean conventions

Service and DAO collaborators are container-managed Jakarta EJBs. Use `@Stateless` for business operations, persistence helpers, and external gateway/client calls that should not hold request-specific state. Use `@Singleton` only when a service intentionally owns application-wide shared state, cache, registry, lifecycle, or subscriptions; declare locking or bean-managed concurrency explicitly for those singletons. API resources inject EJB services with `@EJB`; DAO access should stay behind service-layer EJBs.

Each module reads runtime properties through one typed configuration singleton. Configuration values are parsed, bounded, and defaulted at startup; business methods consume typed getters and plain per-symbol engines receive immutable settings.

Durable Java paths follow `REST resource -> service EJB -> DAO EJB -> database`. Only DAO implementations in `data-model` may use `EntityManager`, JPQL, SQL, `DataSource`, or JDBC. Pure calculations, caches, and external-provider gateways do not require artificial DAOs. Shared persistence-neutral concepts such as canonical market symbols live in `domain-model`, avoiding dependencies on unrelated service modules.

Keep persistence-oriented services and DAOs transactional. Mark HTTP clients, in-memory caches, event publishers, websocket lifecycle helpers, and other non-database collaborators with `@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)` so external IO and cache reads do not run inside unnecessary container transactions.

Keep DTOs, JPA entities, value objects, pure scoring/domain helpers, and per-symbol runtime objects as plain Java classes unless the application server needs to manage lifecycle, transactions, injection, or concurrency for them.

## Market AI service

`MarketAiService` is the public EJB facade used by API resources and websocket endpoints. It coordinates live-symbol lifecycle, chart/signal queries, forecasting, and subscriptions while delegating exchange IO, market context hydration, order-book maintenance, in-memory history, event publishing, and bar persistence to injected collaborator beans. `MarketForecastService` provides stale-while-revalidate forecast snapshots and performs Python/Ollama work asynchronously. `MarketBarStorageService` maps closed bars and delegates durable writes to `MarketBarDao` in `data-model`. Live trade-stream startup/reconnect and order-book startup/gap resync work is requested through asynchronous EJB methods so Java websocket callbacks and API open paths do not block on external exchange connections. `MarketDataViewService` composes market data with currency conversion for display-ready bars and order-book snapshots so API resources do not perform business calculations.

`CurrencyConversionService` prefetches historical provider rates as date ranges before portfolio history is calculated. Provider-backed historical rates can remain cached, while static fallback rates have a short TTL so a temporary provider outage cannot make fallback data authoritative for the lifetime of the JVM.

`user-service` revalidates account state whenever a persisted auth session is resolved. Failed logins are serialized with a user-row lock and create a persisted, automatically expiring `lockoutUntil` deadline at the configured threshold. Cluster-wide source throttles use short-lived database buckets. Password policy, Unicode normalization, Argon2id hashing, BCrypt migration, blocklist checks, and uniform unknown-user work are centralized in `PasswordSecurityService`. Sessions have absolute and idle expiry. One reset row per user is issued and consumed with consistent lock ordering, and token consumption is atomic with the password update. Security outcomes are emitted through `AuthenticationAuditService`; see [authentication security](authentication-security.md).

Authorization rules are read from committed database state with a targeted query over only the request path and its parent prefixes. They are resolved by specificity: longest normalized path first, then exact HTTP method before the canonical `*` wildcard method. This prevents parent-resource roles from weakening a more-specific route, avoids loading the complete policy graph for every request, and avoids stale node-local permission caches after role changes.

`market-ai-service` is structured into subpackages:

- `context`: market context registration, hydration, and manual update handling.
- `stream`: inbound exchange trade stream client.
- `engine`: bar aggregation, feature generation, and event publishing.
- `forecast`: cache-first forecast service plus Python forecasting and Ollama narrative clients.
- `scoring`: pluggable scoring strategies (rule-based and linear model).
- `orderbook`: Binance connection/reconciliation state, isolated payload parsing, and snapshot assembly.
- `model`: bars, trades, features, intervals, signal side, and signal payloads.

This separation keeps exchange ingestion, market context, order-book handling, forecasting, feature engineering, and scoring loosely coupled.

## Why this split helps

- Clear ownership by domain area.
- Easier testing and future replacement of a specific service module.
- Better long-term maintainability than putting all business logic in one API module.
