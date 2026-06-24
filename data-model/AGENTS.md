# Data model agent guide

## Responsibility
`data-model/` owns persisted JPA entities, DAO contracts/implementations, persistence configuration, schema SQL, seed data, and database migrations.

## Key areas
- `src/main/java/com/tradernet/jpa/entities`: JPA entity classes.
- `src/main/java/com/tradernet/jpa/dao`: DAO interfaces and JPA implementations.
- `src/main/resources/META-INF/persistence.xml`: persistence unit configuration.
- `src/main/resources/META-INF/db/schema.sql`: fresh database/H2-compatible schema.
- `src/main/resources/META-INF/db/dev-seed.sql`: local development seed data.
- `src/main/resources/META-INF/db/migrations`: SQL migrations for existing persistent databases.

## Conventions
- When a persisted field is added, update the entity, DTO/API mapping, `schema.sql`, and add a migration if existing databases require the column/table.
- Keep migration SQL idempotent where practical, for example `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
- Do not delete or rewrite existing migrations once they may have been applied to a persistent volume.
- Keep schema changes compatible with the local Docker Compose Postgres/TimescaleDB setup and H2 development paths where applicable.
