# Tradernet

Tradernet is a multi-module project with a Java backend (WAR) and a Vite-based frontend. The Maven build can package the frontend build output into the backend WAR for deployment, and it supports running the frontend as a standalone WAR as well.

## Project layout

- `frontend/` — Vite + React frontend, built with Yarn and bundled into `dist/`.
- `backend/` — Jakarta EE/Spring-based backend packaged as a WAR.
- `pom.xml` — Maven parent project that aggregates both modules and aligns WildFly versions.

## Requirements

- Java 11 (the Maven build targets Java 11).
- Maven 3.8.1+.
- Node.js 20.19.4 (managed in the Maven frontend plugin).
- Yarn 1.22.10 for the Maven build (bootstrapped by the frontend plugin; the app can still use project-local Yarn via Corepack).

## Setup & build

### Build the full project (frontend build profile enabled)

```bash
mvn clean package
```

## Run with Docker

The Docker image builds the full Maven project (including the frontend assets) and deploys the WAR on WildFly.

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

The backend WAR includes the frontend `dist/` output (wired via the `maven-war-plugin`).

### Build without running the frontend Maven profile

```bash
mvn -DdontBuildFrontend clean package
```

This skips the frontend Maven profile (useful in CI when frontend artifacts are prebuilt).

### Build just the frontend

```bash
cd frontend
corepack enable
yarn install
yarn build
```

### Run the frontend locally (development)

```bash
cd frontend
corepack enable
yarn install
yarn dev
```

The `dev` script starts the Vite dev server.

### Build just the backend

```bash
mvn -pl backend -am package
```

The backend module produces a WAR file that can be deployed to your application server (for example, WildFly).

### Health check endpoint

When deployed, the backend exposes a JAX-RS health check at:

```
GET /api/health
```

It returns `{"status":"ok"}` for a basic smoke check.

## Notes

- The frontend module uses a Maven build profile (`build-frontend`) to install Node/Yarn and to run `yarn install` and `yarn build` during the Maven lifecycle.
- The backend WAR packaging pulls the frontend build output from `frontend/dist` into the WAR.
- The parent POM imports the WildFly BOM to align Jakarta EE / RESTEasy versions for WildFly deployments.

## How the application works

- The frontend is a Vite + React UI that builds static assets into `frontend/dist`.
- The backend is a Jakarta EE WAR that exposes JAX-RS endpoints (including `GET /api/health`) and can be deployed on WildFly.
- During a full Maven build (with the frontend profile enabled), the frontend assets are packaged into the backend WAR so a single deployment serves both API and UI.

## Useful scripts

From `frontend/package.json`:

- `yarn dev` — run the dev server.
- `yarn build` — build production assets.
- `yarn lint` — lint the frontend.
- `yarn format` — check formatting.
