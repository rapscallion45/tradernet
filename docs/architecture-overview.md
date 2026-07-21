# Architecture overview

Tradernet is organized as a multi-module Maven project where the backend and frontend can be built together and deployed as one application.

## Main module groups

- `web/`: Vite + React user interface.
- `domain-model/`: persistence-neutral shared domain values and canonical normalization.
- `api/`: Jakarta EE web layer exposing REST and websocket endpoints.
- `services/*`: business logic modules for orders, trades, users/security, currency conversion, and market AI.
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

## Real-time market AI flow

The market AI module (`services/market-ai-service`) adds a stream-oriented path:

1. Trade events are ingested from Binance websocket streams.
2. Trades are aggregated into bars.
3. Features are computed and scored into AI signals.
4. Events are published to lightweight subscribers that enqueue bounded per-client delivery work.
5. Frontend charts and overlays can render bars + signals from those feeds.

Exchange callbacks do not perform blocking recovery IO. Trade reconnects and order-book snapshot resyncs are handed to managed asynchronous EJB methods, while websocket client conversion and serialization run through bounded delivery queues outside the ingestion path. Queue advancement is driven by websocket send-completion callbacks, so managed workers do not wait for browsers.

Market context and forecast REST reads use stale-while-revalidate application services. A request receives the latest snapshot, or a deterministic cold-cache response, while single-flight asynchronous EJB work hydrates context, calls Python forecasting, and optionally generates an Ollama narrative.

Authorization rules are persisted as an HTTP method plus normalized path prefix and allowed roles. This lets read and mutation operations on the same resource use different policies; for example, standard users can read market context while only administrators can update the application-wide context.

## Build/packaging behavior

When the standard Maven build runs with web profile enabled:

- React assets are built under `web/target/sources/dist`.
- The `api` WAR includes those assets.
- Deployment modules can package/run the application in WildFly or Docker.
