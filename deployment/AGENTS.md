# Deployment agent guide

## Responsibility
`deployment/` owns packaging and runtime infrastructure for the Java application and local Docker stack.

## Key areas
- `tradernet-ear`: EAR assembly.
- `wildfly-modules`: WildFly module descriptors/resources.
- `docker-image`: Docker image packaging, entrypoint, Docker Compose topology, TimescaleDB init, forecasting service wiring, and Ollama wiring.

## Conventions
- Compose uses the app image tag `tradernet/tradernet:local-test` for local testing.
- Preserve the `timescaledb_data` named volume unless the task explicitly intends to wipe local database state.
- Do not recommend `docker compose down -v` for normal redeploys.
- For app-only redeploys against a running DB, use `docker compose ... up -d --no-deps --force-recreate tradernet`.
- When adding services, volumes, ports, profiles, or init scripts, update `README.md` and `docs/application-guide.md`.
