# Tradernet application guide

This guide is the operator/developer handbook for the full Tradernet application. It complements the focused documents in this folder by listing the runtime surfaces, persistence behavior, deployment topology, and extension points in one place.

## 1. Application at a glance

Tradernet is a Maven multi-module trading desk application with a Jakarta EE/WildFly backend, a React/Vite frontend, a shared JPA data model, Docker packaging, and an optional market forecasting stack.

| Area | Primary modules/files | Responsibility |
| --- | --- | --- |
| Frontend | `web/` | React/Vite UI, chart panels, session-aware screens, static build assets. |
| API boundary | `api/` | JAX-RS REST resources, websocket endpoint, auth filter, JSON request/response contracts. |
| Domain services | `services/*` | Business logic for users, orders, trades, signals, currency conversion, facade orchestration, and market AI. |
| Persistence | `data-model/` | JPA entities, DAO interfaces/implementations, persistence unit, schema and seed SQL. |
| Deployment | `deployment/*` | EAR assembly, WildFly modules, Docker image, Docker Compose stack, entry script. |
| Forecasting runtime | `python-services/forecasting` | FastAPI service for TimesFM/Chronos adapter-shaped forecasts with a statistical fallback. |
| Local LLM runtime | Docker Compose `ollama` service | Ollama-hosted Gemma model used to turn structured forecasts into concise narrative summaries. |

## 2. Request and data flow

1. A browser loads the React app from the backend WAR/EAR or from the Vite dev server during frontend development.
2. The React app calls REST endpoints under `/api/*` or subscribes to the market websocket.
3. API resources validate query/body data and delegate to service modules.
4. Service modules use DAOs/entities from `data-model` for durable application state.
5. Market AI also consumes Binance trade streams, maintains Binance order books, builds bars, computes features, emits signals, and stores closed bars in `market_bars`.
6. Forecast requests call the Python forecasting service, enrich the forecast with current market context, and optionally ask Ollama/Gemma for a narrative.

## 3. Main API surfaces

Except for `/api/health` and authentication routes, REST endpoints require a valid `tradernet_session` cookie. Command-line smoke tests should call `/api/auth/login` first and then reuse the returned cookie for protected endpoints such as `/api/market/forecast`.

| API | Resource | Purpose |
| --- | --- | --- |
| `GET /api/health` | `HealthResource` | Basic application smoke check. |
| `/api/auth` | `AuthResource` | Login/session-related operations. |
| `/api/users` | `UserResource` | User management and profile operations. |
| `/api/groups` | `GroupResource` | Group management. |
| `/api/roles` | `RoleResource` | Role/resource management. |
| `/api/passwords` | `PasswordResource` | Password workflows. |
| `/api/orders` | `OrderResource` | Order creation, listing, and lifecycle operations. |
| `/api/trades` | `TradeResource` | Trade execution/history operations. |
| `/api/signals` | `SignalResource` | Trading signal operations. |
| `/api/portfolio` | `PortfolioResource` | Portfolio summary/history views. |
| `/api/market/bars` | `MarketResource` | Historical/recent market bars for charts. |
| `/api/market/signals` | `MarketResource` | Recent market AI signals. |
| `/api/market/context` | `MarketResource` | Get/update normalized market context, including backend-calculated bullish-percent display fields and per-input availability flags. |
| `/api/market/forecast` | `MarketResource` | Longer-horizon forecast with bull score, probability, drivers, and narrative. |
| `/api/market/order-book` | `MarketResource` | Backend-maintained Binance aggregated L2 order book with spread, depth, and synchronization status. |
| `/api/ws/market` | `MarketStreamEndpoint` | Websocket stream of market bars and signals. |

### Portfolio history contract

`GET /api/portfolio` returns the current summary plus a daily `history` series. Each history point contains the account value for that day in the selected display currency and an `events` array for order activity on that day. The portfolio page uses those backend-calculated values directly for the chart hover tooltip and BUY/SELL markers.

### Forecast API contract

Request:

```http
GET /api/market/forecast?symbol=BTCUSDT&horizonDays=1
```

Response fields:

| Field | Meaning |
| --- | --- |
| `symbol` | Normalized market symbol. |
| `horizonDays` | Forecast horizon, bounded by the Java client. Defaults to 1 day for the daily-trading UI and API default. |
| `probabilityPositiveReturn` | Probability estimate that return over the horizon is positive. |
| `expectedReturn` | Expected return over the horizon as a decimal. |
| `bullScore` | 0-100 bullishness score derived from forecast/model output. |
| `model` | Forecast backend name, for example `statistical-fallback`, `timesfm`, `chronos`, or `context-fallback`. |
| `drivers` | Human-readable forecast/context drivers. |
| `narrative` | Concise Ollama/Gemma or deterministic fallback summary. |

Example response shape:

```json
{
  "symbol": "BTCUSDT",
  "horizonDays": 1,
  "probabilityPositiveReturn": 0.53,
  "expectedReturn": 0.012,
  "bullScore": 54.2,
  "model": "statistical-fallback",
  "drivers": ["price history source: timescaledb", "recent price momentum positive", "realized volatility contained"],
  "narrative": "Today's BTCUSDT Bull Score is 54. price history source: timescaledb, recent price momentum positive, realized volatility contained. Probability of a positive 1-day return: 53%."
}
```

### Order book API contract

Request:

```http
GET /api/market/order-book?symbol=BTCUSDT&levels=12&currency=USD
```

The backend opens Binance's diff-depth stream, fetches a REST snapshot, applies updates in update-ID order, and resyncs from REST if a gap is detected. The response is aggregated L2 market depth, not individual order/queue-level data.

Response fields include:

| Field | Meaning |
| --- | --- |
| `status` | `LIVE`, `SYNCING`, `SNAPSHOT_ONLY`, `STALE`, or `UNAVAILABLE`. |
| `aggregation` | `AGGREGATED_L2`, reflecting Binance price-level depth rather than individual orders. |
| `bids` / `asks` | Top requested levels with backend-calculated notional, cumulative notional, and depth percentage. |
| `bestBid`, `bestAsk`, `midPrice`, `spread`, `spreadPercent` | Backend-calculated top-of-book values in the requested display currency. |
| `depthImbalancePercent` | Percentage tilt between displayed bid and ask notional. Positive values indicate more bid depth. |
| `exchangeSnapshotLimit` | REST snapshot depth requested from Binance, capped by Binance's supported limits. |
| `resyncCount` | Number of detected stream gaps that forced a snapshot resync. |

## 4. Persistence and database ownership

| Data | Storage | Notes |
| --- | --- | --- |
| Users, roles, groups, passwords, resources | JPA tables in `data-model` schema | Bootstrapped by `SystemBootstrapService` and seed SQL. |
| Orders | `tblOrders` | Used for order lifecycle and investment/performance history. |
| Trades | `tblTrades` | Used for trade execution/history. |
| Signals | `tblSignals` | Stores application trading signals. |
| User properties | `tblUserProperties` | Per-user preferences/properties. |
| Market bars | `market_bars` | Written by `MarketAiService` from closed live bars and read by the Python forecasting service. |

Docker Compose uses TimescaleDB/Postgres for durable local development. The named Docker volume `timescaledb_data` is mounted at `/var/lib/postgresql/data`, so orders, trades, users, market bars, and forecast history inputs survive normal container recreation. Do not run `docker compose down -v` unless deleting the database is intentional.

The `timescaledb-init.sql` script enables the TimescaleDB extension, creates `market_bars`, converts it into a hypertable, and creates an index for symbol/time lookups.

## 5. Docker Compose runtime topology

| Service | Port | Purpose | Persistent state |
| --- | --- | --- | --- |
| `tradernet` | `8080` | WildFly-hosted Tradernet API/UI. | Application data is in Postgres. |
| `postgres` | internal `5432` | TimescaleDB/Postgres database. | `timescaledb_data`. |
| `forecasting-service` | `8000` | FastAPI model adapter/fallback forecast service. | Reads `market_bars` from Postgres. |
| `ollama` | `11434` | Local LLM runtime for Gemma narratives. | `ollama_data`. |
| `ollama-model` | one-shot profile | Pulls the configured Gemma model into Ollama. | `ollama_data`. |

Typical local deployment:

```bash
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml --profile model-init run --rm ollama-model
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml up
```

### Rebuild the test image while leaving the database running

When `postgres` is already running and you only want to rebuild/redeploy the Tradernet `local-test` image, do not run `docker compose down` and do not run `docker compose down -v`. Rebuild the image, then recreate only the `tradernet` service without restarting dependencies:

```bash
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml up -d --no-deps --force-recreate tradernet
```

The `--no-deps` flag prevents Compose from recreating dependent services such as `postgres`, `forecasting-service`, and `ollama`; `timescaledb_data` remains attached to the running database container.

If you changed only the Python forecasting service, rebuild and recreate that service instead, again without restarting Postgres:

```bash
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml build forecasting-service
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml up -d --no-deps --force-recreate forecasting-service
```

### Apply database migrations without losing data

Schema changes that affect an existing named volume are shipped as SQL files under `data-model/src/main/resources/META-INF/db/migrations`. Apply them by piping the migration into the already-running `postgres` service, then recreate only the app container:

```bash
cat data-model/src/main/resources/META-INF/db/migrations/20260624-add-order-bull-score.sql | docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml exec -T postgres psql -U tradernet -d tradernet
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml up -d --no-deps --force-recreate tradernet
```

PowerShell equivalent for applying the migration:

```powershell
Get-Content .\data-model\src\main\resources\META-INF\db\migrations\20260624-add-order-bull-score.sql | docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml exec -T postgres psql -U tradernet -d tradernet
```

This keeps the `timescaledb_data` volume intact. Avoid `docker compose down -v` unless intentionally wiping local orders, trades, users, market bars, and Ollama model data.

Smoke checks:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8000/health
```

Authenticated forecast smoke check with Bash/curl. Run this in Bash, Git Bash, WSL, macOS/Linux shells, or use `curl.exe` in PowerShell because PowerShell aliases `curl` to `Invoke-WebRequest`:

```bash
curl -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"username":"superuser","password":"changeme"}' http://localhost:8080/api/auth/login
curl -b /tmp/tradernet.cookies 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1'
```

PowerShell with real curl executable, if you prefer curl syntax. Put JSON in a variable so PowerShell does not strip the JSON quotes before `curl.exe` receives the body:

```powershell
$loginBody = '{"username":"superuser","password":"changeme"}'
curl.exe -c "$env:TEMP\tradernet.cookies" -H "Content-Type: application/json" --data-raw $loginBody http://localhost:8080/api/auth/login
curl.exe -b "$env:TEMP\tradernet.cookies" 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1'
```

Authenticated forecast smoke check with PowerShell. Run these as three separate commands, or keep the semicolons if you paste the one-line form:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1' -WebSession $session
```

One-line PowerShell form:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession; Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session; Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1' -WebSession $session
```

If a persistent local database returns `INCORRECT_CREDENTIALS`, reset the bootstrap application user's password and retry login:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/forgot-password' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","newPassword":"changeme"}'
```

```bash
curl -H 'Content-Type: application/json' -d '{"username":"superuser","newPassword":"changeme"}' http://localhost:8080/api/auth/forgot-password
```

## 6. Runtime configuration reference

### Database/container variables

| Variable | Default/use | Description |
| --- | --- | --- |
| `DB_TYPE` | `H2` or `POSTGRES` | Selects datasource type in the Docker entry script. Compose uses `POSTGRES`. |
| `DB_HOST` | `postgres` | Database host visible from the Tradernet container. |
| `DB_PORT` | `5432` | Database port. |
| `DB_NAME` | `tradernet` | Database name. |
| `DB_USER` | `tradernet`/`sa` | Database username. |
| `DB_PASSWORD` | environment-specific | Database password. |
| `ADMIN_USERNAME` | `superuser` | WildFly admin username created on startup. |
| `ADMIN_PASSWORD` | `changeme` | WildFly admin password created on startup. |
| `SCHEMA_AUTO_CREATE_DEV` | `true` | Allows H2 schema auto-generation in local dev mode. |

### Market AI Java system properties

| Property | Default | Description |
| --- | --- | --- |
| `market.ai.symbol` | `btcusdt` | Default Binance stream symbol started at application boot. Additional chart symbols become live dynamically when a user opens a chart websocket for that symbol. |
| `market.ai.binance.restBaseUrl` | `https://api.binance.com` | Binance REST base URL used for symbols, klines, and order book snapshots. Use `https://api.binance.us` for Binance.US deployments. |
| `market.ai.binance.wsBaseUrl` | `wss://stream.binance.com:9443/ws` | Binance websocket base URL used for trade and order book streams. Use `wss://stream.binance.us:9443/ws` for Binance.US deployments. |
| `market.ai.orderBook.snapshotLimit` | `5000` | Binance REST order book snapshot depth used before applying websocket deltas. Normalized to Binance-supported limits up to 5000. |
| `market.ai.orderBook.staleAfterMs` | `30000` | Age after which a synchronized order book is marked `STALE` if no update has been applied. |
| `market.ai.context.symbols` | active symbol | Comma-separated symbols for scheduled context hydration. |
| `market.ai.context.ingestion.enabled` | `true` | Enables/disables scheduled no-key market context ingestion. |
| `market.ai.scorer` | `context` | Selects `context`, `linear`, or `rules` signal scorer. |
| `market.ai.model.buyThreshold` | `0.56` | Linear scorer buy threshold. Lower values emit more short-term BUY signals. |
| `market.ai.model.sellThreshold` | `0.44` | Linear scorer sell threshold. Higher values emit more short-term SELL signals. |
| `market.ai.context.buyScoreThreshold` | `54` | Effective context score at or above which a technical BUY is context-confirmed. |
| `market.ai.context.sellScoreThreshold` | `46` | Effective context score at or below which a technical SELL is context-confirmed. |
| `market.ai.context.buyExtremeThreshold` | `64` | Effective context score that can promote a technical HOLD into BUY. |
| `market.ai.context.sellExtremeThreshold` | `36` | Effective context score that can promote a technical HOLD into SELL. |
| `market.ai.context.forecastWeight` | `0.45` | Weight given to the forecast bull score when blending it into the chart signal context score. |
| `market.ai.context.forecastNeutralBand` | `8` | Treats forecast bull scores within `50 +/- this value` as neutral, which biases directional technical signals back to HOLD. |
| `market.ai.signalBullScore.enabled` | `true` | Enables forecast bull-score enrichment for chart BUY/HOLD/SELL signals. |
| `market.ai.signalBullScoreHorizonDays` | `1` | Forecast horizon used when feeding bull score into chart signal generation. |
| `market.ai.signalBullScoreTtlMs` | `60000` | Cache TTL for signal bull-score lookups so every closed bar does not call the Python forecasting service. |
| `market.ai.orderBullScoreHorizonDays` | `1` | Forecast horizon captured as `bullScore` when an order is created. |
| `market.ai.forecasting.url` | `http://forecasting-service:8000` | Python forecasting service base URL. |
| `market.ai.ollama.enabled` | `true` | Enables LLM-generated forecast narratives. |
| `market.ai.ollama.url` | `http://ollama:11434` | Ollama base URL. |
| `market.ai.ollama.model` | `gemma4:e4b` | Ollama model tag used for narratives. |

### Python forecasting variables

| Variable | Default | Description |
| --- | --- | --- |
| `DATABASE_URL` | `postgresql://tradernet:tradernet@postgres:5432/tradernet` | Postgres/TimescaleDB connection string. |
| `FORECAST_BACKEND` | `statistical-fallback` | Forecast backend selector; accepted adapter values are `timesfm`, `chronos`, or fallback. |

## 7. Forecasting and LLM behavior

The real-time chart BUY/HOLD/SELL signal and the forecast card are related but separate:

- Chart signals are generated from short-term technical features, market context, and the cached forecast bull score.
- The technical model still creates the first BUY/SELL/HOLD vote from EMA/RSI features. Market context and forecast bull score then form an effective context score that can confirm the technical vote, block it into HOLD when contradictory, or promote HOLD only when the effective score reaches an extreme.
- Forecast cards and order-history `Bull Score` use the forecast endpoint. The default UI/order/signal horizon is 1 day for daily trading, and the forecast card lets the user select supported horizons such as 1, 3, 7, 14, or 30 days without changing the selected symbol.
- A high forecast bull score pulls the effective context score toward BUY, a low bull score pulls it toward SELL, and a score near 50 falls inside the neutral band and biases directional technical votes back to HOLD. The chart signal caches this score for `market.ai.signalBullScoreTtlMs` milliseconds to avoid calling the forecasting service on every closed bar.

The forecasting path is designed to degrade gracefully:

1. Java requests a forecast from the Python service.
2. If Python is unavailable or returns an error, Java returns a context-based fallback forecast.
3. Java sends structured forecast data to Ollama/Gemma.
4. If Ollama is disabled, unavailable, or returns an empty/error response, Java returns deterministic narrative text. If Ollama returns hardcoded Bitcoin wording, Java normalizes the narrative back to the selected forecast symbol before returning it.

The default Python service is intentionally lightweight. It first reads recent closes from `market_bars`; if a selected symbol has insufficient TimescaleDB history, it falls back to recent Binance 1-minute klines for that symbol. If neither source has enough data, it returns a neutral 50 bull score instead of a hardcoded bullish forecast. It then computes a momentum/volatility fallback forecast and exposes stable hooks for production images that install TimesFM or Chronos.

## 8. Frontend notes

The React app is built with Vite and can run in two modes:

- Packaged mode: Maven builds static frontend assets and includes them in the backend WAR.
- Development mode: run the Vite dev server from `web/src/main/react` with Yarn.

Market UI features can consume:

- REST bars via `/api/market/bars`.
- REST signals via `/api/market/signals`.
- REST order books via `/api/market/order-book`.
- Websocket bars/signals via `/api/ws/market`.
- Forecast cards/summaries via `/api/market/forecast`.

### Chart signal display

The chart signal badges intentionally distinguish a real backend `HOLD` from the absence of a received signal:

- `No signal` means the chart has not received a signal payload for the selected symbol yet; no-signal badges are muted so they do not look equivalent to a real `HOLD`.
- `BUY`, `SELL`, or `HOLD` means the backend emitted an `AiSignal` over `/api/ws/market`.
- The adjacent confidence badge shows strength labels (`No signal`, `Weak`, `Medium`, or `Strong`) derived from the latest signal confidence.
- The chart legend appends the latest signal model version and up to five prioritized signal notes next to the stream status/error text, so messages such as `no market data for 20 seconds` still show the most recent model/driver context when available. Forecast/context notes such as `forecast_bull_score`, `effective_context_score`, and `context_filter` are shown before lower-level technical notes such as EMA delta and RSI.
- The chart interval selector stores the user's last selected interval in browser local storage and falls back to `1S` when no saved or valid interval exists.
- Opening a chart websocket dynamically starts a dedicated Binance trade stream for the selected symbol, so the user-selected symbol becomes live without a redeploy or static configuration change.
- Multiple selected symbols can be live at the same time in one backend process; each symbol has its own bar aggregator, feature engine, and signal engine so rolling indicators and cooldowns do not bleed across symbols.
- Until the first live signal arrives for a newly selected symbol, the initial chart signal can still be generated on demand from recent Binance klines via `GET /api/market/signals`.
- The Market Score Inputs card shows backend-calculated bullish-tilt percentages, not raw z-scores. `50% bull` is neutral only when backend input data is present; missing inputs show muted `No data` badges instead of a fallback percentage. The card-level explanation is available from the info icon next to the title. Values above 50% are supportive context for BUY, and values below 50% are bearish context for SELL. Hovering a badge shows the raw normalized input when data exists. These context inputs feed the backend market score used by `context-v2`; they do not directly place orders and they are blended with the short-term technical signal and forecast bull score before producing BUY/SELL/HOLD.
- The charts sidebar shows the selected symbol order book between the summary card and `TradernetAI Forecast`. The browser polls Tradernet, not Binance directly; the backend maintains the Binance L2 book, exposes `LIVE`/sync status, and resyncs from REST snapshots when diff-depth update IDs gap.

### Forecast and order history display

- The charts sidebar shows the current selected symbol forecast in the `TradernetAI Forecast` card, including bull score, positive-return probability, narrative text, a backend-generated plain-language current condition summary, and a forecast horizon dropdown that refetches the backend forecast for the selected number of days.
- The order history table includes a `Bull Score` column. This value is the forecast-derived bull score captured at order creation time; older rows created before the `bullScore` migration display a muted dash until they have a stored value.

## 9. Operational safeguards

- Keep `timescaledb_data` backups if local order/trade history matters.
- Avoid `docker compose down -v` unless intentionally wiping database and Ollama volumes.
- Treat generated forecasts as informational analytics, not financial advice.
- For production, use managed Postgres/TimescaleDB or a hardened database deployment instead of relying only on a local Docker volume.
- Pin production image tags instead of `latest` for TimescaleDB/Ollama.
- Configure network egress and API/provider allowances for Binance, context providers, model pulls, and any future TimesFM/Chronos model downloads.
