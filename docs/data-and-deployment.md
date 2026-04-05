# Data and deployment

This document explains persistence and runtime packaging.

## Persistence/data model

`data-model/` contains:

- JPA entities (users, roles, groups, orders, trades, signals, passwords, resources).
- DAO interfaces and JPA implementations.
- Persistence configuration (`persistence.xml`).
- SQL schema + seed resources for local/dev bootstrapping.
- Supporting utilities and exceptions.

This module is shared by service modules so persistence concerns stay centralized.

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

The Docker runtime supports environment-variable-driven database wiring (H2 by default, Postgres optional) and startup admin-user setup.
