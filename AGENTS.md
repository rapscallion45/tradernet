# Tradernet agent guide

## Scope
Applies to the whole repository unless a deeper `AGENTS.md` overrides it.

## Project map
- `web/`: React/Vite frontend served in development or packaged into the backend WAR.
- `api/`: Jakarta REST and websocket boundary. Keep request/session handling here and delegate business logic to services.
- `services/`: business modules for orders, trades, users, currency conversion, and market AI.
- `data-model/`: JPA entities, DAOs, persistence configuration, schema SQL, seed SQL, and migrations.
- `deployment/`: EAR assembly, WildFly module packaging, Docker image, Docker Compose stack, and operational scripts.
- `python-services/forecasting/`: FastAPI forecasting adapter used by the Java market AI service.
- `docs/`: centralized operator/developer documentation. Keep docs updated when behavior, deployment, schema, or API contracts change.

## General conventions
- Prefer small, focused changes that keep API/resource layers thin and push business logic into the appropriate service module.
- Keep REST resources as transport adapters only: authentication/session lookup, request validation, parameter parsing, response status shaping, and delegation. Move portfolio/order/market calculations, aggregation, valuation, and DTO construction into service-layer EJBs.
- Keep business calculations on the backend. Frontend code should request calculated values from APIs and limit itself to presentation, formatting, and user interaction state.
- Scope user-owned resources to the authenticated user. Do not trust client-supplied `userId` for protected portfolio, order, trade, or account data unless an explicit admin policy exists; reject mismatches instead of returning another user's data.
- Use container-managed Jakarta EJBs for service-layer collaborators. Prefer `@Stateless` for business operations, persistence workflows, external gateways, and mapper/orchestration services. Use `@Singleton` only for intentional application-wide shared state, caches, registries, subscriptions, or lifecycle owners, and make concurrency/locking decisions explicit.
- Keep blocking IO out of singleton locks, websocket callbacks, and request hot paths where practical. Use container-managed asynchronous EJB boundaries plus cached/snapshot responses for market streams, order-book startup/resync, forecast enrichment, and background persistence.
- API resources should inject service-layer EJBs with `@EJB`. Do not inject DAOs directly into resources; keep persistence access behind the owning service module.
- Keep repeated JAX-RS response patterns centralized. Prefer small request helpers and `ExceptionMapper` implementations for common auth/error handling instead of duplicating optional-user/401/403 response construction across resources.
- Return standard JSON HTTP errors with `ApiErrorDto`/`ApiErrors` or registered JAX-RS `ExceptionMapper` implementations. Keep success/message DTOs for successful auth workflows, but do not return plain strings or empty bodies for API errors.
- Do not expose JPA entities, JPA enums, or persistence implementation types in API or cross-service contracts. Define service DTOs, command objects, and service-owned enums, then map to/from entities inside the owning service module.
- Keep DTO construction and DTO/entity mapping in service-layer mappers or focused service collaborators. Avoid placing DTO assembly in REST resources, JPA entities, or unrelated services.
- API DTOs should return raw semantic values: numbers, timestamps, currency codes, ids, enums, and structured objects. Do not add preformatted display strings for dates, money, percentages, or key/value diagnostics; frontend code owns locale-specific formatting.
- When returning model drivers, signal notes, diagnostics, or explanations, use structured fields such as `key`, `label`, `value`, and `numericValue` rather than concatenated display strings like `foo=1.23`.
- Persist password hashes canonically on `tblUsers.password_hash`. Do not reintroduce password-list tables, password-history APIs, or `/api/passwords`-style endpoints unless a future security design explicitly requires audited password history with hashes only.
- Store only hashes of auth-session and password-reset bearer tokens server-side. Auth cookies must be HttpOnly, SameSite-aware, and Secure in HTTPS deployments, with local HTTP development handled by explicit configuration rather than weakening production defaults.
- Preserve signed portfolio semantics. BUY/open long positions are positive quantities, open SELL/short positions are negative quantities, and backend portfolio valuation/history must include both unless a future product decision explicitly disables short selling.
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
