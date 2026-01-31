# Tradernet

Tradernet is a multi-module project with a Java backend (WAR) and a Vite-based frontend. The Maven build wires them together so the frontend build output is packaged into the backend WAR for deployment.

## Project layout

- `frontend/` — Vite + React frontend, built with Yarn and bundled into `dist/`.
- `backend/` — Jakarta EE/Spring-based backend packaged as a WAR.
- `pom.xml` — Maven parent project that aggregates both modules.

## Requirements

- Java 11 (the Maven build targets Java 11).
- Maven 3.x.
- Node.js 20.11.1 (managed in the Maven frontend plugin).
- Yarn (project-local, see the `packageManager` field in `frontend/package.json`).

## Setup & build

### Build the full project

```bash
mvn clean package
```

The backend WAR includes the frontend `dist/` output (wired via the `maven-war-plugin`).

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

## Notes

- The frontend module uses the Maven `frontend-maven-plugin` to install Node/Yarn and to run `yarn install` and `yarn build` during the Maven lifecycle.
- The backend WAR packaging pulls the frontend build output from `frontend/dist` into the WAR.

## Useful scripts

From `frontend/package.json`:

- `yarn dev` — run the dev server.
- `yarn build` — build production assets.
- `yarn lint` — lint the frontend.
- `yarn format` — check formatting.
