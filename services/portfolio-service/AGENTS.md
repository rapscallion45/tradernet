# Portfolio service agent guide

## Responsibility
`services/portfolio-service` owns user-scoped portfolio orchestration, signed position replay, current valuation, history construction, and portfolio response DTOs.

## Conventions
- Consume orders only through `OrderPortfolioQueryService`; do not import JPA entities or DAOs.
- Preserve signed positions: BUY opens positive quantity and SELL opens negative quantity.
- Keep position replay, current valuation, and history construction in focused collaborators.
- Use cached current market snapshots for portfolio reads. Do not call an external market provider once per order or position.
- Batch historical FX preparation and historical bar retrieval by distinct symbol/date range before valuation loops.
- Expose portfolio results through `PortfolioQueryService`; keep the REST resource limited to authenticated-user lookup, validation, and response shaping.
