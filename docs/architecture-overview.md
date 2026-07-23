# Architecture overview

Tradernet is organized as a multi-module Maven project where the backend and frontend can be built together and deployed as one application.

## Main module groups

- `web/`: Vite + React user interface.
- `domain-model/`: persistence-neutral shared domain values and canonical normalization.
- `api/`: Jakarta EE web layer exposing REST and websocket endpoints.
- `services/*`: business logic modules for orders, portfolios, trades, users/security, currency conversion, and market AI.
- `data-model/`: JPA entities, DAO interfaces/implementations, persistence setup.
- `deployment/*`: assembly modules for EAR packaging, WildFly modules, and Docker image.

## End-to-end flow

1. A user interacts with the React app.
2. The app calls backend endpoints in `api` (for auth, users, orders, trades, market data, and health checks).

Authentication follows `REST adapter -> authentication/session EJBs -> security DAOs -> database`. Password hashing, throttling, session policy, reset workflows, and audit events remain in `user-service`; API code owns cookies and HTTP status/headers only. See [authentication security](authentication-security.md).
3. API resources delegate orchestration/business operations to `services/*` modules.
4. Services read and write durable state through DAO interfaces from `data-model`.
5. DAO implementations in `data-model` own JPA, JPQL, SQL, JDBC, and datasource access.
6. Responses return to the web client as JSON.

Services that only use external providers, in-memory state, or pure calculations omit the DAO step. The separately deployed FastAPI forecasting process follows the equivalent `route -> service -> repository -> database` flow and keeps Binance access in a separate gateway.

Cross-module calls use focused persistence-neutral `@Local` EJB contracts. Portfolio calculations consume an order projection rather than `OrderEntity`; order commands and queries exchange service-owned records; API user reads consume a profile query contract while authentication uses separate session, authentication, authorization, and audit contracts. REST route policy definitions are owned and registered by the API, while the user service persists and evaluates them.

`ArchitectureBoundaryTest` enforces API-to-persistence isolation, explicit cross-module EJB interfaces, persistence-neutral local contracts, service implementation boundaries, and the prohibition on broad service `@LocalBean` views.

## Real-time market AI flow

The market AI module (`services/market-ai-service`) adds a stream-oriented path:

1. Trade events are ingested from bounded, reference-counted Binance websocket streams.
2. Trades are aggregated into bars.
3. Features are computed and scored into AI signals.
4. A symbol-scoped subscription service atomically owns pipeline references and event registrations, then delivers only matching events to lightweight subscribers that enqueue bounded per-client delivery work.
5. Frontend charts and overlays can render bars + signals from those feeds.

Exchange callbacks do not perform blocking recovery IO. Trade reconnects and order-book snapshot resyncs are handed to managed asynchronous EJB methods, while websocket client conversion and serialization run through bounded delivery queues outside the ingestion path. Queue advancement is driven by websocket send-completion callbacks, so managed workers do not wait for browsers.

The API does not access the event publisher or live pipeline implementation. It authenticates the websocket, registers bounded delivery, obtains one subscription id from `LiveMarketSubscriptionService`, and releases that id during cleanup.

Request-driven order-book websocket clients are also bounded and evicted after an idle timeout. Exhausted client pools produce retryable service responses instead of allocating indefinitely.

Market context and forecast REST reads use stale-while-revalidate application services. A request receives the latest snapshot, or a deterministic cold-cache response, while single-flight asynchronous EJB work hydrates context, calls Python forecasting, and optionally generates an Ollama narrative.

The exchange-symbol catalog follows the same pattern: reads return the current immutable snapshot immediately and single-flight a managed asynchronous Binance refresh. FX conversion uses a separate provider gateway and provider-rate cache, warms requested date ranges before collection mapping, and returns HTTP 503 when no provider-sourced rate is available.

Authorization rules are persisted as an HTTP method plus normalized path prefix and allowed roles. This lets read and mutation operations on the same resource use different policies; for example, standard users can read market context while only administrators can update the application-wide context.

## Build/packaging behavior

When the standard Maven build runs with web profile enabled:

- React assets are built under `web/target/sources/dist`.
- The `api` WAR includes those assets.
- Deployment modules can package/run the application in WildFly or Docker.
