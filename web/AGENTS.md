# Web frontend agent guide

## Responsibility
`web/` contains the React/Vite frontend. Most active code lives under `web/src/main/react`.

## Key areas
- `src/api`: typed API resources and shared API types.
- `src/hooks`: React Query hooks and domain-specific frontend hooks.
- `src/pages`: route-level screens such as charts and order history.
- `src/components`: reusable UI components, including chart panels and layout.
- `src/global`: routes, constants, global providers, and CSS.

## Conventions
- Keep backend contracts reflected in `src/api/types.ts` and resource classes.
- Prefer hooks for data fetching/mutations rather than embedding API calls directly in components.
- Preserve the distinction between `No signal` fallback and backend `BUY`/`SELL`/`HOLD` signals in chart UI.
- Use existing formatting utilities from `src/utils/intl` for currency, numbers, and dates.
- If a user-visible UI behavior changes, update `docs/application-guide.md` or the relevant focused docs.

## Useful checks
- From `web/src/main/react`: `yarn build` for type-check/build.
- From `web/src/main/react`: `yarn lint` if available in the environment.
