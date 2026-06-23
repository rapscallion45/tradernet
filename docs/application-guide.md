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
5. Market AI also consumes Binance trade streams, builds bars, computes features, emits signals, and stores closed bars in `market_bars`.
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
| `/api/market/context` | `MarketResource` | Get/update normalized market context. |
| `/api/market/forecast` | `MarketResource` | Longer-horizon forecast with bull score, probability, drivers, and narrative. |
| `/api/ws/market` | `MarketStreamEndpoint` | Websocket stream of market bars and signals. |

### Forecast API contract

Request:

```http
GET /api/market/forecast?symbol=BTCUSDT&horizonDays=30
```

Response fields:

| Field | Meaning |
| --- | --- |
| `symbol` | Normalized market symbol. |
| `horizonDays` | Forecast horizon, bounded by the Java client. |
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
  "horizonDays": 30,
  "probabilityPositiveReturn": 0.64,
  "expectedReturn": 0.035,
  "bullScore": 67.5,
  "model": "statistical-fallback",
  "drivers": ["limited TimescaleDB history", "context priors active", "funding rates neutral"],
  "narrative": "Today's Bitcoin Bull Score is 68. limited TimescaleDB history, context priors active, funding rates neutral. Probability of a positive 30-day return: 64%."
}
```

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

Smoke checks:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8000/health
```

Authenticated forecast smoke check with Bash/curl. Run this in Bash, Git Bash, WSL, macOS/Linux shells, or use `curl.exe` in PowerShell because PowerShell aliases `curl` to `Invoke-WebRequest`:

```bash
curl -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"username":"superuser","password":"changeme"}' http://localhost:8080/api/auth/login
curl -b /tmp/tradernet.cookies 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=30'
```

PowerShell with real curl executable, if you prefer curl syntax:

```powershell
curl.exe -c "$env:TEMP\tradernet.cookies" -H "Content-Type: application/json" -d '{"username":"superuser","password":"changeme"}' http://localhost:8080/api/auth/login
curl.exe -b "$env:TEMP\tradernet.cookies" 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=30'
```

Authenticated forecast smoke check with PowerShell. Run these as three separate commands, or keep the semicolons if you paste the one-line form:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=30' -WebSession $session
```

One-line PowerShell form:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession; Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session; Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=30' -WebSession $session
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
| `market.ai.symbol` | `btcusdt` | Binance stream symbol for live ingestion. |
| `market.ai.context.symbols` | active symbol | Comma-separated symbols for scheduled context hydration. |
| `market.ai.context.ingestion.enabled` | `true` | Enables/disables scheduled no-key market context ingestion. |
| `market.ai.scorer` | `context` | Selects `context`, `linear`, or `rules` signal scorer. |
| `market.ai.model.buyThreshold` | `0.62` | Linear scorer buy threshold. |
| `market.ai.model.sellThreshold` | `0.38` | Linear scorer sell threshold. |
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

The forecasting path is designed to degrade gracefully:

1. Java requests a forecast from the Python service.
2. If Python is unavailable or returns an error, Java returns a context-based fallback forecast.
3. Java sends structured forecast data to Ollama/Gemma.
4. If Ollama is disabled, unavailable, or returns an empty/error response, Java returns deterministic narrative text.

The default Python service is intentionally lightweight. It reads recent closes from `market_bars`, computes a momentum/volatility fallback forecast, and exposes stable hooks for production images that install TimesFM or Chronos.

## 8. Frontend notes

The React app is built with Vite and can run in two modes:

- Packaged mode: Maven builds static frontend assets and includes them in the backend WAR.
- Development mode: run the Vite dev server from `web/src/main/react` with Yarn.

Market UI features can consume:

- REST bars via `/api/market/bars`.
- REST signals via `/api/market/signals`.
- Websocket bars/signals via `/api/ws/market`.
- Forecast cards/summaries via `/api/market/forecast`.

## 9. Operational safeguards

- Keep `timescaledb_data` backups if local order/trade history matters.
- Avoid `docker compose down -v` unless intentionally wiping database and Ollama volumes.
- Treat generated forecasts as informational analytics, not financial advice.
- For production, use managed Postgres/TimescaleDB or a hardened database deployment instead of relying only on a local Docker volume.
- Pin production image tags instead of `latest` for TimescaleDB/Ollama.
- Configure network egress and API/provider allowances for Binance, context providers, model pulls, and any future TimesFM/Chronos model downloads.
