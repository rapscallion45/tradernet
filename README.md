# Tradernet

Tradernet is a multi-module project with a Java backend (WAR) and a Vite-based web UI. The Maven build can package the web build output into the backend WAR for deployment, and it supports running the web UI as a standalone WAR as well.

## Project layout

- `web/` — Vite + React web UI, built with Yarn and bundled into `dist/`.
- `data-model/`, `services/`, `api/` — Jakarta EE/Spring-based backend modules (the `api` module produces the WAR).
- `pom.xml` — Maven parent project that aggregates modules and aligns WildFly versions.

## Requirements

- Java 11 (the Maven build targets Java 11).
- Maven 3.8.1+.
- Node.js 20.19.4 (managed in the Maven frontend plugin).
- Yarn 1.22.10 for the Maven build (bootstrapped by the frontend plugin; the app can still use project-local Yarn via Corepack).

## Setup & build

### Build the full project (web build profile enabled)

```bash
mvn clean package
```

## Run with Docker

The Docker image is built via Maven from the `deployment/docker-image` module, which assembles the WAR and Docker build context.

### Build and run with Docker

```bash
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local package
docker run --rm -p 8080:8080 tradernet/tradernet:local
```

### Build and run the test container (Docker Desktop run configuration)

Use this configuration to build the image and run it locally with explicit env vars (including the required admin password).

**Build command**

```bash
mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test package
```

**Run configuration**

```bash
docker run --rm \
  --name tradernet-test \
  -p 8080:8080 \
  -e ADMIN_PASSWORD=local-admin-password \
  -e DB_TYPE=POSTGRES \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=tradernet \
  -e DB_USER=tradernet \
  -e DB_PASSWORD=tradernet \
  tradernet/tradernet:local-test
```

Then open `http://localhost:8080` or check the health endpoint at `http://localhost:8080/api/health`.

### Run with Docker Compose

```bash
docker compose -f deployment/docker-image/docker-compose.yml up
```

These commands work with Docker Desktop (which includes Docker Engine and Compose).

The backend WAR includes the web `dist/` output (wired via the `maven-war-plugin`).

### Database configuration (Docker)

The container configures a WildFly datasource on startup. By default it uses PostgreSQL.
Set environment variables to override connection details:

```
DB_TYPE=POSTGRES
DB_HOST=postgres
DB_PORT=5432
DB_NAME=tradernet
DB_USER=tradernet
DB_PASSWORD=tradernet
```

The provided Docker Compose file includes a PostgreSQL service with matching defaults.

### Admin user password

On startup the container creates a WildFly admin user. You must set `ADMIN_PASSWORD` to a non-default value or the container will refuse to start (set `ALLOW_DEFAULT_ADMIN_PASSWORD=true` only for local development). 

### Build without running the web Maven profile

```bash
mvn -DdontBuildFrontend clean package
```

This skips the web Maven profile (useful in CI when web artifacts are prebuilt).

### Build just the web UI

```bash
cd web
corepack enable
yarn install
yarn build
```

### Run the web UI locally (development)

```bash
cd web
corepack enable
yarn install
yarn dev
```

The `dev` script starts the Vite dev server.

### Build just the backend API module

```bash
mvn -pl api -am package
```

The backend API module produces a WAR file that can be deployed to your application server (for example, WildFly).

### Health check endpoint

When deployed, the backend exposes a JAX-RS health check at:

```
GET /api/health
```

It returns `{"status":"ok"}` for a basic smoke check.

## Notes

- The web module uses a Maven build profile (`build-react`) to install Node/Yarn and to run `yarn install` and `yarn build` during the Maven lifecycle.
- The backend API WAR packaging pulls the web build output from `web/target/sources/dist` into the WAR.
- The parent POM imports the WildFly BOM to align Jakarta EE / RESTEasy versions for WildFly deployments.
- If Maven reports cached resolution failures for the WildFly BOM, re-run the build/import with `-U` to force dependency updates (for example: `mvn -U -pl api -am package`).

## How the application works

- The web UI is a Vite + React app that builds static assets into `web/target/sources/dist`.
- The backend API module is a Jakarta EE WAR that exposes JAX-RS endpoints (including `GET /api/health`) and can be deployed on WildFly.
- During a full Maven build (with the web profile enabled), the web assets are packaged into the backend WAR so a single deployment serves both API and UI.

## Useful scripts

From `web/src/main/react/package.json`:

- `yarn dev` — run the dev server.
- `yarn build` — build production assets.
- `yarn lint` — lint the web UI.
- `yarn format` — check formatting.
