# Tradernet application guide

This guide is the operator/developer handbook for the full Tradernet application. It complements the focused documents in this folder by listing the runtime surfaces, persistence behavior, deployment topology, and extension points in one place.

## 1. Application at a glance

Tradernet is a Maven multi-module trading desk application with a Jakarta EE/WildFly backend, a React/Vite frontend, a shared JPA data model, Docker packaging, and an optional market forecasting stack.

| Area | Primary modules/files | Responsibility |
| --- | --- | --- |
| Frontend | `web/` | React/Vite UI, chart panels, session-aware screens, static build assets. |
| Shared domain | `domain-model/` | Persistence-neutral shared values and canonical market-symbol normalization. |
| API boundary | `api/` | JAX-RS REST resources, websocket endpoint, auth filter, JSON request/response contracts. |
| Domain services | `services/*` | Business logic for users, orders, trades, currency conversion, and market AI. |
| Persistence | `data-model/` | JPA entities, DAO interfaces/implementations, persistence unit, schema and seed SQL. |
| Deployment | `deployment/*` | EAR assembly, WildFly modules, Docker image, Docker Compose stack, entry script. |
| Forecasting runtime | `python-services/forecasting` | FastAPI service for TimesFM/Chronos adapter-shaped forecasts with a statistical fallback. |
| Local LLM runtime | Docker Compose `ollama` service | Ollama-hosted Gemma model used to turn structured forecasts into concise narrative summaries. |

## 2. Request and data flow

1. A browser loads the React app from the backend WAR/EAR or from the Vite dev server during frontend development.
2. The React app calls REST endpoints under `/api/*` or subscribes to the market websocket.
3. API resources validate query/body data and delegate to service modules.
4. Service modules use DAO interfaces from `data-model` for durable application state; DAO implementations exclusively own JPA/JDBC database access.
5. Market AI also consumes Binance trade streams, maintains Binance order books, builds bars, computes features, emits signals, and stores closed bars in `market_bars`.
6. Forecast requests call the Python forecasting service, enrich the forecast with current market context, and optionally ask Ollama/Gemma for a narrative.

## 3. Main API surfaces

Except for `/api/health` and authentication routes, REST endpoints require a valid `tradernet_session` cookie. Session state is persisted in the database and role-policy lookup is owned by `user-service`, while the API filter and websocket handshake only enforce those service decisions. The market websocket at `/api/ws/market` requires the same cookie during the websocket handshake. Command-line smoke tests should call `/api/auth/login` first and then reuse the returned cookie for protected endpoints such as `/api/market/forecast`.

Every session lookup reloads the current user policy. Disabling, deleting, expiring, or locking an account, reaching the failed-login limit, or requiring a password change therefore invalidates existing sessions as well as blocking new logins. Invalid sessions are removed when encountered, and expired auth/reset rows are deleted by a scheduled cleanup every 15 minutes.

Clean bootstrap role defaults are additive: `ALL Rights` receives every protected resource, `Admin Rights` receives the standard trading resources plus users/groups, and `Standard Rights` receives orders, portfolio, trades, and market. Existing custom assignments are preserved.

An expired-password login returns `ACCOUNT_PASSWORD_EXPIRED` and sets a short-lived, HTTP-only `tradernet_password_reset` cookie rather than a full session. Reuse that temporary cookie only for `/api/auth/forgot-password`, then log in again to receive `tradernet_session`.

Auth/session cookies are HttpOnly and SameSite=Lax. The Secure attribute is enabled automatically for HTTPS or `X-Forwarded-Proto: https` requests, and can be forced with `tradernet.auth.cookie.secure`. Raw session/reset bearer tokens are never stored server-side; `tblAuthSessions.token` and `tblPasswordResetSessions.token` contain token hashes.

HTTP-level API failures use a standard JSON error body. Jakarta Bean Validation failures on request DTOs and invalid market-context updates are mapped to this contract with JAX-RS exception mappers:

```json
{
  "error": {
    "status": 400,
    "code": "Bad Request",
    "errorMessage": "Request body is required",
    "timestamp": 1784476800000
  }
}
```

| API | Resource | Purpose |
| --- | --- | --- |
| `GET /api/health` | `HealthResource` | Basic application smoke check. |
| `/api/auth` | `AuthResource` | Login/session-related operations. |
| `/api/users` | `UserResource` | User management and profile operations. |
| `/api/groups` | `GroupResource` | Group management. |
| `/api/roles` | `RoleResource` | Role/resource management. |
| `/api/orders` | `OrderResource` | Order creation, listing, and lifecycle operations. |
| `/api/trades` | `TradeResource` | Authenticated user's trade history, optionally filtered by `symbol`. |
| `/api/portfolio` | `PortfolioResource` | Portfolio summary/history views. |
| `/api/market/bars` | `MarketResource` | Historical/recent market bars for charts. |
| `/api/market/signals` | `MarketResource` | Recent market AI signals. |
| `/api/market/context` | `MarketResource` | Gets normalized market context with backend-calculated bullish-percent display fields and per-input availability flags. Updates accept mutable z-score inputs only. |
| `/api/market/forecast` | `MarketResource` | Longer-horizon forecast with bull score, probability, drivers, and narrative. |
| `/api/market/order-book` | `MarketResource` | Backend-maintained Binance aggregated L2 order book with spread, depth, and synchronization status. |
| `/api/ws/market` | `MarketStreamEndpoint` | Authenticated websocket stream of market bars and signals. |

### Portfolio history contract

`GET /api/portfolio` returns the current summary plus a daily `history` series. Each history point contains the account value for that day in the selected display currency and an `events` array for order activity on that day. The portfolio page uses those backend-calculated values directly for the chart hover tooltip and BUY/SELL markers.

For non-USD display currencies, the backend prefetches the required historical FX date range before calculating the daily series. Provider-backed quotes fill weekends from the most recent prior quote; static fallback rates are cached briefly and retried after provider recovery rather than becoming process-lifetime historical data.

Open BUY/long positions are represented as positive quantities. Open SELL/short positions are represented as negative quantities and are included in holdings, totals, profit/loss, and historical account valuation.

### Order history contract

`GET /api/orders` returns the authenticated user's orders. Supplying a different `userId` is rejected with `403` rather than exposing another user's history. Each order uses `id` as its single canonical identifier; the former duplicate `orderId` response alias is not emitted. Monetary metrics such as `price`, `currentPrice`, `pnl`, `closePrice`, and `netValue` are returned as numeric values in the response `currency`; clients are responsible for locale-specific date, currency, and percent formatting.

`POST /api/orders` persists the order and opening fill before any non-critical forecast enrichment. Advisory fields such as `aiPrediction` and `bullScore` are populated asynchronously when market/forecast data is available, so the immediate create response can contain null advisory fields while later `GET /api/orders` responses include the stored enrichment.

### Trade history contract

`GET /api/trades` returns only the authenticated user's persisted fills, ordered newest first. Each item includes the related `orderId`, normalized `symbol`, execution `side`, `executionType` (`OPEN` or `CLOSE`), signed `quantity`, `price`, and `timestamp`. Pass `symbol=BTCUSDT` to filter the history to one symbol.

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
| `drivers` | Structured forecast/context driver objects with `key`, `label`, and optional `value` or `numericValue`. |
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
  "drivers": [
    { "key": "history_source", "label": "price history source", "value": "timescaledb" },
    { "key": "momentum", "label": "recent price momentum positive" },
    { "key": "volatility", "label": "realized volatility contained" }
  ],
  "narrative": "Today's BTCUSDT Bull Score is 54. price history source: timescaledb, recent price momentum positive, realized volatility contained. Probability of a positive 1-day return: 53%."
}
```

### Market context update contract

`POST /api/market/context?symbol=BTCUSDT` accepts one or more mutable normalized input fields:

```json
{
  "etfFlowZScore": 0.4,
  "exchangeOutflowZScore": 0.1,
  "fundingRateZScore": -0.2,
  "openInterestChangeZScore": 0.3,
  "mvrvZScore": 0.0,
  "liquidityGrowthZScore": 0.2,
  "sentimentZScore": 0.5
}
```

The response is the hydrated `MarketContextSnapshot`, including derived bullish-percent and explicit per-input availability fields. A z-score of `0` is available neutral data, not missing data, and partial updates preserve availability for inputs they do not replace. Derived response fields are read-only; if an older client sends them in a POST body they are ignored. Empty updates and non-finite numeric inputs return the standard HTTP 400 error body.

### Order book API contract

Request:

```http
GET /api/market/order-book?symbol=BTCUSDT&levels=12&currency=USD
```

The backend opens Binance's diff-depth stream, fetches a REST snapshot, applies updates in update-ID order, and requests an asynchronous EJB-managed REST resync if a gap is detected. The Java websocket callback only records the gap and schedules recovery, so provider IO does not block the stream callback. The response is aggregated L2 market depth, not individual order/queue-level data.

Order-book websocket startup and retry snapshot sync are scheduled in the background. A first request for a symbol can therefore return `SYNCING` with the latest cached snapshot while the backend connects and hydrates the book.

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
| Users, roles, groups, resources | JPA tables in `data-model` schema | Identity data is seeded by `SystemBootstrapService` through DAOs. User IDs are database-generated and password hashes are stored canonically on `tblUsers.password_hash`. |
| Auth sessions | `tblAuthSessions`, `tblPasswordResetSessions` | Accessed through dedicated DAOs. The `token` columns store hashes of full login-session and short-lived reset tokens only. |
| Orders | `tblOrders` | Used for order lifecycle and investment/performance history. |
| Trades | `tblTrades` | User-scoped fills created by `TradeExecutionService` when orders are placed or closed, with `orderId`, `side`, and `executionType` metadata. SELL executions are stored as negative quantities. |
| Market bars | `market_bars` | Closed bars flow through `MarketAiService -> MarketBarStorageService -> MarketBarDao`; the Python forecasting repository reads them. |

Docker Compose uses TimescaleDB/Postgres for durable local development. The named Docker volume `timescaledb_data` is mounted at `/var/lib/postgresql/data`, so orders, trades, users, market bars, and forecast history inputs survive normal container recreation. Do not run `docker compose down -v` unless deleting the database is intentional.

Schema SQL and versioned migration files in `data-model` are the source of truth for tables, columns, and indexes. Service-layer bootstrap code does not run DDL at application startup; apply migrations to long-lived databases before redeploying schema-dependent code.

Before deploying the database-generated user ID change to an existing Postgres database, apply `20260720-generate-user-ids.sql`. It converts `tblUsers.id` to an identity column and advances its sequence beyond the highest existing user ID.

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

If login returns `ACCOUNT_PASSWORD_EXPIRED`, reset the password with the same cookie jar/session and retry login:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/forgot-password' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","newPassword":"changeme"}' -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session
```

```bash
curl -b /tmp/tradernet.cookies -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"username":"superuser","newPassword":"changeme"}' http://localhost:8080/api/auth/forgot-password
curl -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"username":"superuser","password":"changeme"}' http://localhost:8080/api/auth/login
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
| `TRADERNET_BOOTSTRAP_DEFAULT_PASSWORD` | unset | Application bootstrap password for creating or repairing `superuser`, `admin`, and `standard` credentials. Set this explicitly outside local development. |
| `TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD` | `false` | Allows the insecure `changeme` application bootstrap fallback. Docker Compose opts into this for local development; production deployments should leave it disabled and set `TRADERNET_BOOTSTRAP_DEFAULT_PASSWORD`. |

### Auth Java system properties

| Property | Default | Description |
| --- | --- | --- |
| `tradernet.auth.cookie.secure` | auto | Forces auth cookies Secure when `true` or explicitly disables Secure for local HTTP when `false`. If unset, HTTPS and `X-Forwarded-Proto: https` requests receive Secure cookies. |
| `tradernet.auth.maxFailedLoginAttempts` | `5` | Number of persisted failed password checks allowed before account/session access is blocked. Successful authentication or password reset clears the counter; `bypassLockout` accounts ignore this threshold. |
| `tradernet.bootstrap.defaultPassword` | unset | Java property equivalent of `TRADERNET_BOOTSTRAP_DEFAULT_PASSWORD`. |
| `tradernet.bootstrap.allowDefaultPassword` | `false` | Java property equivalent of `TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD`; intended for local/dev only. |

### Market AI Java system properties

| Property | Default | Description |
| --- | --- | --- |
| `market.ai.symbol` | `btcusdt` | Default Binance stream symbol started at application boot. Additional chart symbols become live dynamically when a user opens a chart websocket for that symbol. |
| `market.ai.binance.restBaseUrl` | `https://api.binance.com` | Binance REST base URL used for symbols, klines, and order book snapshots. Use `https://api.binance.us` for Binance.US deployments. |
| `market.ai.binance.wsBaseUrl` | `wss://stream.binance.com:9443/ws` | Binance websocket base URL used for trade and order book streams. Use `wss://stream.binance.us:9443/ws` for Binance.US deployments. |
| `market.ai.websocket.maxPendingEvents` | `128` | Maximum queued bar/signal events per API websocket session. Slow clients drop the oldest pending event so they cannot block market ingestion. |
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
| `market.ai.orderBullScoreHorizonDays` | `1` | Forecast horizon captured asynchronously as stored order `bullScore` after an order is created. |
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
- A high forecast bull score pulls the effective context score toward BUY, a low bull score pulls it toward SELL, and a score near 50 falls inside the neutral band and biases directional technical votes back to HOLD. The chart signal uses an async cache for this score, refreshed according to `market.ai.signalBullScoreTtlMs`, so live websocket trade handling does not call the forecasting service on every closed bar.

The forecasting path is designed to degrade gracefully:

1. Java requests a forecast from the Python service.
2. If Python is unavailable or returns an error, Java returns a context-based fallback forecast.
3. Java sends structured forecast data to Ollama/Gemma.
4. If Ollama is disabled, unavailable, or returns an empty/error response, Java returns deterministic narrative text. If Ollama returns hardcoded Bitcoin wording, Java normalizes the narrative back to the selected forecast symbol before returning it.

The default Python service is intentionally lightweight and follows `FastAPI route -> ForecastService -> TimescalePriceHistoryRepository -> database`. Binance fallback access is isolated in `BinanceMarketDataClient`. The service first reads recent closes from `market_bars`; if a selected symbol has insufficient TimescaleDB history, it falls back to recent Binance 1-minute klines for that symbol. If neither source has enough data, it returns a neutral 50 bull score instead of a hardcoded bullish forecast. It then computes a momentum/volatility fallback forecast and exposes stable hooks for production images that install TimesFM or Chronos.

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
- The chart legend appends the latest signal model version and up to five prioritized structured signal notes next to the stream status/error text, so messages such as `no market data for 20 seconds` still show the most recent model/driver context when available. Forecast/context note keys such as `forecast_bull_score`, `effective_context_score`, and `context_filter` are shown before lower-level technical notes such as EMA delta and RSI.
- The chart interval selector stores the user's last selected interval in browser local storage and falls back to `1S` when no saved or valid interval exists.
- Opening a chart websocket dynamically starts a dedicated Binance trade stream for the selected symbol, so the user-selected symbol becomes live without a redeploy or static configuration change. Closed/error streams are detected by a managed scheduler and requested for reconnection.
- Market publisher callbacks only enqueue websocket work. Currency conversion, JSON serialization, and network sends use bounded per-session queues on a managed asynchronous EJB boundary, isolating ingestion from slow or disconnected browsers.
- Multiple selected symbols can be live at the same time in one backend process; each symbol has its own bar aggregator, feature engine, and signal engine so rolling indicators and cooldowns do not bleed across symbols.
- Closed live bars are published to chart subscribers immediately and persisted asynchronously for downstream forecasting history.
- Until the first live signal arrives for a newly selected symbol, the initial chart signal can still be generated on demand from recent Binance klines via `GET /api/market/signals`.
- The Market Score Inputs card shows backend-calculated bullish-tilt percentages, not raw z-scores. `50% bull` is neutral only when backend input data is present; missing inputs show muted `No data` badges instead of a fallback percentage. The card-level explanation is available from the info icon next to the title. Values above 50% are supportive context for BUY, and values below 50% are bearish context for SELL. Hovering a badge shows the raw normalized input when data exists. These context inputs feed the backend market score used by `context-v2`; they do not directly place orders and they are blended with the short-term technical signal and forecast bull score before producing BUY/SELL/HOLD.
- The charts sidebar shows the selected symbol order book between the summary card and `TradernetAI Forecast`. The browser polls Tradernet, not Binance directly; the backend maintains the Binance L2 book, exposes `LIVE`/sync status, and resyncs from REST snapshots when diff-depth update IDs gap.

### Forecast and order history display

- The charts sidebar shows the current selected symbol forecast in the `TradernetAI Forecast` card, including bull score, positive-return probability, narrative text, a frontend-generated current condition summary derived from backend numeric fields, and a forecast horizon dropdown that refetches the backend forecast for the selected number of days.
- The order history table includes a `Bull Score` column. This value is the forecast-derived bull score captured asynchronously after order creation; newly-created or older rows display a muted dash until they have a stored value.

## 9. Operational safeguards

- Keep `timescaledb_data` backups if local order/trade history matters.
- Avoid `docker compose down -v` unless intentionally wiping database and Ollama volumes.
- Treat generated forecasts as informational analytics, not financial advice.
- For production, use managed Postgres/TimescaleDB or a hardened database deployment instead of relying only on a local Docker volume.
- Pin production image tags instead of `latest` for TimescaleDB/Ollama.
- Configure network egress and API/provider allowances for Binance, context providers, model pulls, and any future TimesFM/Chronos model downloads.
