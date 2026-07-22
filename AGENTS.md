# Tradernet agent guide

## Scope
Applies to the whole repository unless a deeper `AGENTS.md` overrides it.

## Project map
- `web/`: React/Vite frontend served in development or packaged into the backend WAR.
- `domain-model/`: persistence-neutral shared domain values and canonical normalization used across service boundaries.
- `api/`: Jakarta REST and websocket boundary. Keep request/session handling here and delegate business logic to services.
- `services/`: business modules for orders, portfolios, trades, users, currency conversion, and market AI.
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
- Require focused `@Local` EJB contracts at API and cross-module boundaries. API code and one service module must not inject another module's concrete EJB class. Do not use `@LocalBean` on service beans or add a broad facade that merely forwards unrelated operations.
- REST route definitions and their default authorization policies are owned by `api`; the user service owns policy persistence and evaluation but must not hardcode another module's endpoint catalog.
- Authorization policies must distinguish operations that have different risk. Persist and evaluate the HTTP method/action with the normalized resource path, select the longest matching path and then the exact method over a wildcard, and do not union broader parent permissions into a more-specific route. Authorization changes must be visible after commit across application nodes; do not add an indefinite node-local policy cache without commit-safe distributed invalidation.
- Enforce `REST/resource -> service -> DAO -> database` for durable Java state. Service modules must not inject `EntityManager` or `DataSource`, issue JPQL/SQL, or own JDBC code; put DAO contracts and their JPA/JDBC implementations in `data-model` and inject them into services with `@EJB`.
- Services that do not access durable state, such as external market gateways, caches, and pure calculations, do not need artificial DAOs. Keep those collaborators behind focused service/gateway interfaces instead.
- Keep repeated JAX-RS response patterns centralized. Prefer small request helpers and `ExceptionMapper` implementations for common auth/error handling instead of duplicating optional-user/401/403 response construction across resources.
- Return standard JSON HTTP errors with `ApiErrorDto`/`ApiErrors` or registered JAX-RS `ExceptionMapper` implementations. Keep success/message DTOs for successful auth workflows, but do not return plain strings or empty bodies for API errors.
- Do not expose JPA entities, JPA enums, or persistence implementation types in API or cross-service contracts. Define service DTOs, command objects, and service-owned enums, then map to/from entities inside the owning service module.
- Keep entity-bearing collaborator methods inside the owning persistence module. API-facing and cross-module EJB methods must return service-owned DTOs/results or accept service-owned commands, including authentication and session workflows.
- Keep DTO construction and DTO/entity mapping in service-layer mappers or focused service collaborators. Avoid placing DTO assembly in REST resources, JPA entities, or unrelated services.
- API DTOs should return raw semantic values: numbers, timestamps, currency codes, ids, enums, and structured objects. Do not add preformatted display strings for dates, money, percentages, or key/value diagnostics; frontend code owns locale-specific formatting.
- When returning model drivers, signal notes, diagnostics, or explanations, use structured fields such as `key`, `label`, `value`, and `numericValue` rather than concatenated display strings like `foo=1.23`.
- Persist password hashes canonically on `tblUsers.password_hash`. Do not reintroduce password-list tables, password-history APIs, or `/api/passwords`-style endpoints unless a future security design explicitly requires audited password history with hashes only.
- Store only hashes of auth-session and password-reset bearer tokens server-side. Auth cookies must be non-persistent for login sessions, HttpOnly, SameSite=Strict, Secure by default, and explicitly downgraded only for local HTTP. Every auth response, including mapped failures, must be `no-store`.
- Enforce privilege ceilings inside security-administration services, not only in REST path policy. An administrator must not grant or modify `ALL Rights` unless the authenticated actor already holds `ALL Rights`; pass the actor into role/group mutation EJBs and audit denials and successful privilege changes.
- Apply the corresponding persisted role policy to WebSocket handshakes. Validate an explicit or same-origin policy, revalidate long-lived connections without extending idle sessions, and close matching connections on logout or revoked access.
- Treat bootstrap access-control assignments as initial seed data. Do not restore removed grants on every restart, and fail startup when a persisted bootstrap account still matches a known development password while the local fallback is disabled.
- Revalidate persisted user status, expiry, temporary `lockoutUntil`, and password-change requirements whenever an auth session is resolved. Enforce both idle and absolute session expiry. Failed logins must be serialized with a user-row lock, temporary lockouts must expire automatically, and login/reset endpoints must use persisted cluster-wide source throttles with HTTP 429/`Retry-After` handling.
- Keep at most one password-reset row per user. Issue and consume reset tokens with a consistent `user -> reset row` lock order, consume them atomically with password updates, revoke user sessions after reset, and clean expired auth/reset/throttle rows on scheduled service paths.
- Centralize password validation and hashing in the user service. Normalize new passwords with NFC, count Unicode code points, support at least 64 characters, apply UTF-8 resource limits and common/breached/context blocklists, use Argon2id for new hashes, and transparently upgrade compatible BCrypt hashes. Do not enforce arbitrary composition rules or duplicate password policy in REST resources.
- Persist a unique canonical username (NFKC, trimmed, lowercase) and use it for every identity lookup. Local password authentication must reject external-identity accounts after performing equivalent password-verification work.
- Emit sanitized structured authentication audit events without passwords or bearer tokens. Use the servlet container remote address for source throttling; never trust `X-Forwarded-*` directly in application code. Configure trusted-proxy rewriting in the container.
- Register a final JAX-RS exception mapper for unexpected `Exception` failures. Return a generic JSON 500 body with an operator reference id, log the full exception against that id, and never expose internal exception messages or stack traces to clients.
- Use Jakarta Bean Validation on JAX-RS request DTOs and central `ExceptionMapper` implementations for common validation failures. Do not duplicate the same null/range checks manually in resources unless the check depends on authenticated or persisted state.
- Define reusable symbol, currency, interval, and similar validation contracts next to the shared domain type that owns them, then apply the same contract at every REST/query/DTO entry point instead of silently defaulting invalid input.
- Keep websocket publisher callbacks non-blocking: callbacks may filter and enqueue only. Perform currency conversion, serialization, and network sends through bounded per-client queues on managed asynchronous service boundaries, and unregister queues when sessions close.
- Order-book websocket callbacks must request snapshot recovery through an asynchronous EJB proxy; never perform synchronous REST resync directly on a Java websocket callback thread. Live exchange clients must have a managed reconnect policy after close or error.
- Batch historical exchange-rate requests over the required date range before mapping bars, orders, or portfolio history. Cache only provider-sourced FX rates; when no authoritative rate is available, return a typed service-unavailable failure rather than inventing a static financial value.
- Track market-context input availability explicitly. A valid z-score of zero is present neutral data, not missing data; partial updates must preserve availability for fields they do not replace.
- Keep one canonical field for each API concept. Do not publish compatibility aliases such as both `id` and `orderId` on the same resource DTO unless a documented, time-bounded version migration requires them.
- Keep schema ownership in `data-model`. Service-layer startup/bootstrap code may seed required identity/domain data, but must not run DDL, replay `schema.sql`, or hardcode migration-style `ALTER TABLE`/`CREATE TABLE` statements.
- Keep independently deployed services layered too. FastAPI routes delegate to application services; application services use repository adapters for database access and separate gateways for external providers.
- Do not rely on insecure or shared bootstrap passwords by default. Production bootstrap users need separate account-specific secrets; accounts without one are not created. Any shared `changeme` fallback must require an explicit local/dev opt-in.
- Keep order placement on the persistence path. Non-critical advisory enrichment such as AI prediction and forecast bull score should run asynchronously after the order and fill have been persisted.
- Collection reads and portfolio valuation must not make one synchronous external-provider call per order or position. Use cached snapshots, distinct-symbol batching, or managed asynchronous refresh; reserve a synchronous quote lookup for an explicit command that requires execution-time pricing.
- Keep read and write DTOs separate where response models expose derived/read-only fields. Market context writes accept mutable normalized inputs; derived bullish-percent and availability fields are response-only.
- Preserve signed portfolio semantics. BUY/open long positions are positive quantities, open SELL/short positions are negative quantities, and backend portfolio valuation/history must include both unless a future product decision explicitly disables short selling.
- Keep generated or environment-specific artifacts out of version control.
- Read runtime properties and environment variables only in a typed, module-owned configuration bean. Validate and bound configuration once at startup, then inject that bean into business services and pass immutable settings to plain domain objects.
- For external forecasting/context dependencies, use stale-while-revalidate caches and managed asynchronous refresh. REST request threads must not synchronously chain provider calls or optional narrative generation.
- Keep complex calculations split by responsibility. Portfolio orchestration, signed position replay, current valuation, and history construction belong in separate collaborators; exchange clients should delegate provider parsing and API snapshot mapping to focused components.
- Bound every dynamically created exchange websocket client pool. Use reference-counted release for subscribed trade streams and bounded idle eviction for request-driven order-book clients; return a retryable capacity error instead of growing client maps without limit.
- Keep live-market lifecycle and symbol routing behind `LiveMarketSubscriptionService`. Websocket endpoints own authentication and bounded delivery registration, but must not coordinate event-publisher listeners or trade-pipeline reference counts directly.
- Keep `ArchitectureBoundaryTest` passing. It enforces API/persistence isolation, persistence-neutral `@Local` contracts, explicit cross-module EJB injection, service implementation boundaries, and the ban on service `@LocalBean` views.
- Do not use recursive `ls -R` or `grep -R`; use `find` and `rg`.
- Never add try/catch blocks around imports.
- When adding database columns for persisted data, update the JPA entity/DTO/API response, `schema.sql`, and add a migration under `data-model/src/main/resources/META-INF/db/migrations` when existing databases need it.
- When changing user-visible behavior or deployment steps, update `README.md` or the relevant `/docs` page.

## Useful checks
- Backend/module build: `mvn -DdontBuildReact clean package` or a narrower `mvn -pl <module> -am ...` command.
- Docker image build: `mvn -pl deployment/docker-image -am -Pbuild-image -Ddocker.image.tag=local-test clean package`.
- Frontend build: `cd web/src/main/react && yarn build`.
- Python syntax check: `python3 -m py_compile <file>`.
