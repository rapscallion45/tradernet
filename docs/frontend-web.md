# Frontend web app

The web client lives in `web/src/main/react` and is built with Vite + React + TypeScript.

## Key folders

- `src/pages`: page-level features (Dashboard, Orders, Admin, Login).
- `src/components`: reusable UI components and layout pieces.
- `src/hooks`: stateful data-fetching and view-model hooks.
- `src/api`: typed API client layer for backend communication.
- `src/global`: app-wide constants, route definitions, CSS, and providers.
- `src/utils`: shared utility helpers.

## Runtime flow

1. Router resolves the route.
2. Page component composes reusable components.
3. Hooks fetch/mutate data through `src/api` resources.
4. API client calls backend REST endpoints.
5. UI updates from hook state and renders notifications/loading/errors.

Authentication forms keep only transport-level checks such as required fields and password confirmation. New-password length, byte limits, common-password checks, and hashing policy are canonical in `user-service`; the frontend displays the standard API policy error instead of maintaining a second rule set that can drift.

## Storybook and design support

The project includes story files (`*.stories.tsx`) and Storybook support files for component-level development and preview.
