# Backend services

Business logic is split into focused modules under `services/`.

## Core service modules

- `order-service`: order creation, order book operations, and order DTO mapping.
- `trade-service`: trade execution and lifecycle operations.
- `user-service`: user profile/auth-adjacent workflows and bootstrap routines.
- `signal-service`: trading signal processing logic.
- `currency-conversion-service`: currency conversion support and code abstractions.
- `facade-service`: aggregated operations shared across resource boundaries.

## Market AI service

`market-ai-service` is structured into subpackages:

- `stream`: inbound exchange trade stream client.
- `engine`: bar aggregation, feature generation, and event publishing.
- `scoring`: pluggable scoring strategies (rule-based and linear model).
- `model`: bars, trades, features, intervals, signal side, and signal payloads.

This separation keeps ingestion, feature engineering, and scoring loosely coupled.

## Why this split helps

- Clear ownership by domain area.
- Easier testing and future replacement of a specific service module.
- Better long-term maintainability than putting all business logic in one API module.
