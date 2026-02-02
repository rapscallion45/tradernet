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

The Docker image builds the full Maven project (including the web assets) and deploys the WAR on WildFly.

### Build and run with Docker

```bash
docker build -t tradernet .
docker run --rm -p 8080:8080 tradernet
```

Then open `http://localhost:8080` or check the health endpoint at `http://localhost:8080/api/health`.

### Run with Docker Compose

```bash
docker compose up --build
```

These commands work with Docker Desktop (which includes Docker Engine and Compose).

The backend WAR includes the web `dist/` output (wired via the `maven-war-plugin`).

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

- The web module uses a Maven build profile (`build-frontend`) to install Node/Yarn and to run `yarn install` and `yarn build` during the Maven lifecycle.
- The backend API WAR packaging pulls the web build output from `web/dist` into the WAR.
- The parent POM imports the WildFly BOM to align Jakarta EE / RESTEasy versions for WildFly deployments.
- If Maven reports cached resolution failures for the WildFly BOM, re-run the build/import with `-U` to force dependency updates (for example: `mvn -U -pl api -am package`).

## How the application works

- The web UI is a Vite + React app that builds static assets into `web/dist`.
- The backend API module is a Jakarta EE WAR that exposes JAX-RS endpoints (including `GET /api/health`) and can be deployed on WildFly.
- During a full Maven build (with the web profile enabled), the web assets are packaged into the backend WAR so a single deployment serves both API and UI.

## Useful scripts

From `web/package.json`:

- `yarn dev` — run the dev server.
- `yarn build` — build production assets.
- `yarn lint` — lint the web UI.
- `yarn format` — check formatting.
