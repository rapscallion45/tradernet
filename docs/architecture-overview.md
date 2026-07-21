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

Exchange callbacks do not perform blocking recovery IO. Trade reconnects and order-book snapshot resyncs are handed to managed asynchronous EJB methods, while websocket client conversion, serialization, and sends run through bounded delivery queues outside the ingestion path.

## Build/packaging behavior

When the standard Maven build runs with web profile enabled:

- React assets are built under `web/target/sources/dist`.
- The `api` WAR includes those assets.
- Deployment modules can package/run the application in WildFly or Docker.
