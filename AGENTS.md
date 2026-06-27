# Tradernet agent guide

## Scope
Applies to the whole repository unless a deeper `AGENTS.md` overrides it.

## Project map
- `web/`: React/Vite frontend served in development or packaged into the backend WAR.
- `api/`: Jakarta REST and websocket boundary. Keep request/session handling here and delegate business logic to services.
- `services/`: business modules for orders, trades, users, signals, currency conversion, facade orchestration, and market AI.
- `data-model/`: JPA entities, DAOs, persistence configuration, schema SQL, seed SQL, and migrations.
- `deployment/`: EAR assembly, WildFly module packaging, Docker image, Docker Compose stack, and operational scripts.
- `python-services/forecasting/`: FastAPI forecasting adapter used by the Java market AI service.
- `docs/`: centralized operator/developer documentation. Keep docs updated when behavior, deployment, schema, or API contracts change.

## General conventions
- Prefer small, focused changes that keep API/resource layers thin and push business logic into the appropriate service module.
- Keep business calculations on the backend. Frontend code should request calculated values from APIs and limit itself to presentation, formatting, and user interaction state.
- Keep generated or environment-specific artifacts out of version control.
- Do not use recursive `ls -R` or `grep -R`; use `find` and `rg`.
- Never add try/catch blocks around imports.
- When adding database columns for persisted data, update the JPA entity/DTO/API response, `schema.sql`, and add a migration under `data-model/src/main/resources/META-INF/db/migrations` when existing databases need it.
- When changing user-visible behavior or deployment steps, update `README.md` or the relevant `/docs` page.

## Useful checks
- Backend/module build: `mvn -DdontBuildReact clean package` or a narrower `mvn -pl <module> -am ...` command.
- Docker image build: `mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package`.
- Frontend build: `cd web/src/main/react && yarn build`.
- Python syntax check: `python3 -m py_compile <file>`.
