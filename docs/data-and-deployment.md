# Data and deployment

This document explains persistence and runtime packaging.

## Persistence/data model

`data-model/` contains:

- JPA entities for active backend domains such as users, roles, groups, orders, trades, resources, auth sessions, and market data compatibility tables.
- DAO interfaces and JPA/JDBC implementations, including identity tokens and market-bar persistence.
- Persistence configuration (`persistence.xml`).
- SQL schema, seed resources, and versioned migrations for local/dev bootstrapping and long-lived database upgrades.
- Supporting utilities and exceptions.

This module is shared by service modules so persistence concerns stay centralized. Service and API modules do not issue JPQL/SQL or inject `EntityManager`/`DataSource`; they delegate durable operations through these DAOs.

Authorization policies are persisted in `tblResources` as a canonical uppercase HTTP method plus normalized resource path. `*` is reserved for an intentional all-method policy; bootstrap data should prefer explicit methods so read and mutation permissions can be assigned independently. Existing databases receive this field through `20260721-add-resource-http-method.sql`, and `20260721-index-resource-policy-lookup.sql` indexes targeted path/method lookups.

Apply `20260721-harden-authorization-policy.sql` to existing PostgreSQL databases. It rejects ambiguous duplicate role, group, resource-name, or normalized path/method policy rows before adding the non-null and unique constraints used by authorization. Resolve any reported collision deliberately before retrying the migration; do not silently union duplicate permissions.

Temporary account lockout state is persisted in `tblUsers.lockoutUntil`. Apply `20260721-add-user-lockout-until.sql` to existing databases before deploying this version; new databases receive the column from `schema.sql`.

Apply `20260721-authentication-production-hardening.sql` before deploying the hardened authentication code to an existing PostgreSQL database. It adds canonical usernames, session idle metadata, one-reset-token-per-user storage, and cluster-wide throttle buckets; it deliberately invalidates existing login and reset sessions.

## Deployment modules

- `deployment/tradernet-ear`: enterprise archive assembly.
- `deployment/wildfly-modules`: custom WildFly module descriptors/resources.
- `deployment/docker-image`: Docker build files, compose file, and entry scripts.

## Packaging model

A full build can produce:

1. Compiled backend modules.
2. Web frontend static assets.
3. API WAR containing static frontend assets.
4. Optional EAR and container image for deployment.

## Runtime configuration highlights

The Docker runtime supports environment-variable-driven database wiring (H2 by default, Postgres optional) and startup identity setup. Application bootstrap seeds missing roles, method-scoped resources, groups, initial assignments, and only those production users with separate account-specific secrets. Existing assignments are not restored after an administrator removes them. The shared `changeme` fallback is an explicit local-development option; startup fails if a persisted bootstrap user still matches it after that option is disabled. Schema changes belong in `data-model` SQL and migrations rather than service-layer startup code.
