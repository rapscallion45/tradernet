# Architecture overview

Tradernet is organized as a multi-module Maven project where the backend and frontend can be built together and deployed as one application.

## Main module groups

- `web/`: Vite + React user interface.
- `api/`: Jakarta EE web layer exposing REST and websocket endpoints.
- `services/*`: business logic modules (orders, trades, users, signals, market-ai, etc.).
- `data-model/`: JPA entities, DAO interfaces/implementations, persistence setup.
- `deployment/*`: assembly modules for EAR packaging, WildFly modules, and Docker image.

## End-to-end flow

1. A user interacts with the React app.
2. The app calls backend endpoints in `api` (for auth, users, orders, trades, market data, and health checks).
3. API resources delegate orchestration/business operations to `services/*` modules.
4. Services read and write data through DAOs/entities from `data-model`.
5. Responses return to the web client as JSON.

## Real-time market AI flow

The market AI module (`services/market-ai-service`) adds a stream-oriented path:

1. Trade events are ingested from Binance websocket streams.
2. Trades are aggregated into bars.
3. Features are computed and scored into AI signals.
4. Events are published for API/websocket consumers.
5. Frontend charts and overlays can render bars + signals from those feeds.

## Build/packaging behavior

When the standard Maven build runs with web profile enabled:

- React assets are built under `web/target/sources/dist`.
- The `api` WAR includes those assets.
- Deployment modules can package/run the application in WildFly or Docker.
