# Tradernet

Tradernet is a multi-module trading desk application with a Jakarta EE/WildFly backend, a Vite + React frontend, shared JPA persistence, Docker packaging, and an optional TimescaleDB-backed market forecasting stack with Ollama/Gemma narratives.

## Documentation

Use this README as the quickstart. Full details live in `/docs`:

- [Documentation index](docs/README.md) — all application docs and suggested reading order.
- [Application guide](docs/application-guide.md) — full-stack operator/developer handbook covering APIs, persistence, deployment, forecasting, configuration, and safeguards.
- [Architecture overview](docs/architecture-overview.md) — module map and request/data flow.
- [Backend services](docs/backend-services.md) — domain services and market AI package layout.
- [API layer](docs/api-layer.md) — REST resources, auth filter, and websocket boundary.
- [Authentication security](docs/authentication-security.md) — passwords, sessions, throttling, cookies, audit, and production deployment gates.
- [Frontend web app](docs/frontend-web.md) — React/Vite structure and runtime flow.
- [Data + deployment](docs/data-and-deployment.md) — persistence, packaging, and runtime infrastructure.
- [Market signal accuracy](docs/market-signal-accuracy.md) — market context scoring, forecasting, and Ollama/Gemma behavior.

## Requirements

- Java 11 (the Maven build targets Java 11).
- Maven 3.8.1+.
- Docker Desktop or Docker Engine + Compose for the containerized stack.
- Node.js 20.19.4 and Yarn 1.22.10 are bootstrapped by the Maven frontend plugin for normal builds.

## Project layout

- `web/` — Vite + React UI.
- `api/` — Jakarta REST API WAR and websocket endpoint.
- `services/` — domain services, including market AI and forecasting integration.
- `data-model/` — JPA entities, DAOs, persistence config, schema, and seed SQL.
- `deployment/` — EAR assembly, WildFly modules, Docker image, Docker Compose, and entry scripts.
- `python-services/forecasting/` — FastAPI forecasting adapter with a statistical fallback and TimesFM/Chronos hooks.

## Quickstart: build the application

```bash
mvn clean package
```

To skip the React build when frontend artifacts already exist:

```bash
mvn -DdontBuildReact clean package
```

## Quickstart: run with Docker Compose

Build the Tradernet image expected by Compose:

```bash
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package
```

Pull the local Ollama/Gemma model once:

```bash
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml --profile model-init run --rm ollama-model
```

Start the stack:

```bash
docker compose -f deployment/docker-image/src/main/docker/docker-compose.yml up
```

Compose starts Tradernet, TimescaleDB/Postgres, the Python forecasting service, and Ollama. TimescaleDB/Postgres data is stored in the named Docker volume `timescaledb_data`, so order history, trades, market bars, users, and forecasting inputs persist across normal container recreation. Do not run `docker compose down -v` unless you intentionally want to delete those volumes.

The Compose stack explicitly opts into the local application bootstrap password fallback so the smoke-test users can log in with `changeme`. Non-local environments must leave `TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD` disabled and configure separate `TRADERNET_BOOTSTRAP_SUPERUSER_PASSWORD`, `TRADERNET_BOOTSTRAP_ADMIN_PASSWORD`, and `TRADERNET_BOOTSTRAP_STANDARD_PASSWORD` secrets only for the accounts they need. See [authentication security](docs/authentication-security.md).

## Smoke checks

The health endpoints are public:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8000/health
```

Application endpoints such as `/api/market/forecast` and `/api/ws/market` require an authenticated `tradernet_session` cookie. Log in first, then reuse the session cookie.

If login returns `ACCOUNT_PASSWORD_EXPIRED`, the response sets a short-lived, HTTP-only `tradernet_password_reset` cookie instead of a full session. Reuse that temporary cookie when calling `/api/auth/forgot-password`, then log in again to receive `tradernet_session`.

PowerShell (run these as three separate commands, or keep the semicolons if you paste them as one line):

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1' -WebSession $session
```

One-line PowerShell form:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession; Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"changeme"}' -WebSession $session; Invoke-RestMethod -Uri 'http://localhost:8080/api/market/forecast?symbol=BTCUSDT&horizonDays=1' -WebSession $session
```

If login returns `ACCOUNT_PASSWORD_EXPIRED`, reset the password with the same web session and then try the login again:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/forgot-password' -Method Post -ContentType 'application/json' -Body '{"newPassword":"Local-Portfolio-Password-2026"}' -WebSession $session
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"username":"superuser","password":"Local-Portfolio-Password-2026"}' -WebSession $session
```

Bash/curl (run this in Bash, Git Bash, WSL, macOS/Linux shells, or use `curl.exe` in PowerShell because PowerShell aliases `curl` to `Invoke-WebRequest`):

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

Bash/curl password reset after an `ACCOUNT_PASSWORD_EXPIRED` login response:

```bash
curl -b /tmp/tradernet.cookies -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"newPassword":"Local-Portfolio-Password-2026"}' http://localhost:8080/api/auth/forgot-password
curl -c /tmp/tradernet.cookies -H 'Content-Type: application/json' -d '{"username":"superuser","password":"Local-Portfolio-Password-2026"}' http://localhost:8080/api/auth/login
```

Open the app at:

```text
http://localhost:8080
```

## Local development shortcuts

Build just the backend API module:

```bash
mvn -pl api -am package
```

Run the frontend dev server:

```bash
cd web/src/main/react
corepack enable
yarn install
yarn dev
```

Useful frontend scripts are defined in `web/src/main/react/package.json`:

- `yarn dev`
- `yarn build`
- `yarn lint`
- `yarn format`

## Runtime configuration highlights

Docker Compose provides working defaults for local development. The most commonly changed values are:

```text
DB_TYPE=POSTGRES
DB_HOST=postgres
DB_PORT=5432
DB_NAME=tradernet
DB_USER=tradernet
DB_PASSWORD=tradernet
TRADERNET_BOOTSTRAP_SUPERUSER_PASSWORD=<secret-manager value>
TRADERNET_BOOTSTRAP_ADMIN_PASSWORD=<optional separate secret>
TRADERNET_BOOTSTRAP_STANDARD_PASSWORD=<optional separate secret>
TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD=true
market.ai.forecasting.url=http://forecasting-service:8000
market.ai.live.maxSymbols=32
market.ai.orderBook.maxSymbols=32
market.ai.orderBook.idleTimeoutMs=300000
market.ai.orderBullScoreHorizonDays=1
market.ai.signalBullScore.enabled=true
market.ai.signalBullScoreHorizonDays=1
market.ai.signalBullScoreTtlMs=60000
market.ai.forecast.ttlMs=60000
market.ai.model.buyThreshold=0.56
market.ai.model.sellThreshold=0.44
market.ai.context.buyScoreThreshold=54
market.ai.context.sellScoreThreshold=46
market.ai.context.buyExtremeThreshold=64
market.ai.context.sellExtremeThreshold=36
market.ai.context.forecastWeight=0.45
market.ai.context.forecastNeutralBand=8
market.ai.ollama.url=http://ollama:11434
market.ai.ollama.model=gemma4:e4b
```

See the [Application guide](docs/application-guide.md#6-runtime-configuration-reference) for the full configuration reference.
