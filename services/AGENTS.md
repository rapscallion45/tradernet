# Services agent guide

## Responsibility
`services/` contains business logic modules. API resources should delegate here rather than implementing domain behavior directly.

## Module map
- `order-service`: order lifecycle, order DTO mapping, and persistence-neutral portfolio projections.
- `portfolio-service`: user-scoped position replay, current valuation, portfolio history, and portfolio DTOs.
- `trade-service`: trade command handling and user-scoped trade queries.
- `user-service`: user profiles, credentials/sessions, policy persistence/evaluation, and identity bootstrap data.
- `currency-conversion-service`: currency conversion, quote-currency resolution, and exchange-rate lookup. Do not couple this module to market model classes.
- `market-ai-service`: market bars, display-currency market data views, signal scoring, forecasting integration, order-book depth, and market context.

## Conventions
- Keep service APIs stable for `api/` callers.
- Expose focused `@Local` contracts for API and cross-module consumers. Keep concrete bean classes and entity-bearing methods internal to the owning module, do not use service `@LocalBean` views, and avoid catch-all facade services.
- Prefer container-managed EJBs for service and DAO collaborators. Use `@Stateless` for operation-oriented services and `@Singleton` only for application-wide shared state, cache, registry, lifecycle, or subscriptions.
- Keep all JPA, JPQL, SQL, `EntityManager`, `DataSource`, and JDBC access inside DAO implementations in `data-model`. Service EJBs own policy and orchestration and inject DAO interfaces; they must not double as repositories.
- Keep persistence-oriented services/DAOs transactional; mark HTTP clients, caches, event publishers, websocket lifecycle helpers, and other non-database collaborators as non-transactional.
- Keep blocking IO out of singleton locks and request/websocket hot paths where practical. Use `@Asynchronous` EJB methods, caches, queues, or snapshot-style responses for slow external calls, websocket startup/resync, forecast refreshes, and background persistence.
- Read runtime settings through a typed configuration EJB owned by the module. Business services and plain domain engines must not parse `System.getProperty` or environment values independently.
- Prefer stale-while-revalidate services for externally enriched reads: return the latest snapshot immediately, single-flight a managed background refresh, and provide a deterministic cold-cache response.
- Batch user, role, and resource assignment lookups in DAOs; do not issue one query per submitted collection element.
- Plain Java websocket listeners must hand recovery/reconnect work back to an intercepted EJB method. Do not call synchronous REST snapshot recovery or other blocking provider IO from an exchange callback thread.
- Revalidate account eligibility when resolving every persisted session, not only during login. Enforce idle and absolute expiry, persist lockout deadlines and cluster-wide source throttles, let lockouts recover automatically, clear account lockout on successful authentication/password reset, and schedule expired session/reset/throttle cleanup.
- Keep new-password policy, NFC normalization, Unicode/UTF-8 bounds, breached/context blocklists, Argon2id hashing, BCrypt migration, and uniform unknown-user checks centralized in the user-service password collaborator.
- Keep one reset row per user. Use `user -> reset row` as the lock order for both issuance and consumption, and consume the token in the same transaction as password validation/update so policy failures roll back consumption.
- Emit sanitized authentication outcomes through the dedicated security-audit collaborator. Never log passwords, raw session/reset tokens, or password hashes.
- Pass the authenticated actor into group/role mutation services. Enforce privilege ceilings in the service transaction, serialize policy writes with a database lock, and audit the actor and outcome; API path authorization alone is not sufficient for security administration.
- Let the API module register its route policy catalog through the user-service policy registration contract. Seed built-in role/resource, group/role, and bootstrap-user/group relationships only when their owning object is first created. Runtime authorization changes must survive restart. Reject persisted known development credentials when insecure bootstrap mode is disabled.
- Resolve authorization from current committed database policy with a targeted DAO query over only the request path and its parent prefixes. Choose the longest matching normalized path and prefer an exact HTTP method over a wildcard; broader parent resources must not weaken a specific route.
- Fetch historical FX data in provider-supported date ranges and warm the conversion cache before collection mapping. Cache only provider-sourced rates and raise a typed availability failure when no authoritative rate exists; never substitute static exchange-rate tables.
- Keep non-critical enrichment out of the write path for user commands. For example, persist order placement/fill state first, then populate advisory forecast fields asynchronously.
- Do not perform provider IO once per item while mapping a collection. Resolve distinct symbols in one pass from cached snapshots or a batch gateway; an explicit execution command may request one current quote when its business semantics require it.
- Keep DTOs, JPA entities, value objects, pure helpers, and per-symbol runtime objects as plain Java classes unless container lifecycle, injection, transactions, or concurrency are needed.
- Keep JPA entities confined to persistence workflows inside their owning service module. EJB contracts consumed by API code or another service module must use service-owned DTOs, results, commands, and enums; authentication/session collaborators must exchange `AuthUserDto`-style contracts rather than `UserEntity`.
- Separate write DTOs from read DTOs when response models include derived fields; do not add no-op setters just to make read models deserialize as requests.
- Treat data availability as explicit state when zero is a valid domain value. Partial updates must preserve per-field availability instead of inferring presence from numeric magnitude.
- Own domain calculations in service/backend modules rather than duplicating formulas in API resources or frontend code.
- Put multi-step workflows such as portfolio construction, valuation, history generation, and response DTO assembly behind service-layer EJBs; keep API resources as thin delegators.
- Split portfolio orchestration, signed position replay, current valuation, and history generation into focused collaborators so one accounting rule cannot drift between summary and history paths.
- Portfolio services must preserve signed position semantics: long holdings are positive quantities, short/open SELL positions are negative quantities, and valuation/history should include both unless short selling is explicitly disabled.
- Keep dynamically created exchange client pools explicitly bounded. Trade-stream subscriptions must release reference counts on disconnect; request-driven order-book clients must expire after bounded idle time.
- Expose live trade streams through a symbol-scoped subscription contract. The market module owns pipeline acquisition/release and publisher registration atomically; transport modules receive one subscription id and must not access publisher or pipeline implementation classes.
- Treat `api/src/test/java/com/tradernet/api/ArchitectureBoundaryTest.java` as a required build guard when adding modules or contracts.
- Reuse persistence-neutral shared values and normalization from `domain-model` for concepts used by multiple services. Do not depend on an unrelated service module only to reuse a helper.
- Keep canonical validation patterns next to their owning shared domain value and reuse them in request DTOs and API parameters so service entry points agree on accepted input.
- Avoid coupling unrelated service modules directly unless there is an explicit orchestration reason.
- If a service change affects persistence, coordinate with `data-model/` schema/entity/DAO changes.
- Do not run schema creation or migration DDL from service-layer startup code. `data-model` schema SQL and migrations own database shape.
- If a service change affects UI-visible behavior, update API DTOs/types and docs.
