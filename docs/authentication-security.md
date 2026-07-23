# Authentication security

Tradernet's local authentication path is owned by `user-service`; the API is only the HTTP/cookie adapter and `data-model` owns durable security state.

## Password and identity rules

- `tblUsers.password_hash` is the only password-hash source of truth.
- New hashes use Argon2id with a default 19 MiB memory cost, two iterations, and one lane. Existing BCrypt hashes remain valid and are replaced with Argon2id after the next successful login.
- Passwords are normalized to Unicode NFC before Argon2id hashing. Policy counts Unicode code points, accepts 15 to 128 characters by default, caps normalized input at 512 UTF-8 bytes, and does not impose composition rules.
- New passwords are rejected when they contain the canonical username or exactly match the bundled common-password blocklist. Production deployments should mount a substantially larger organization-approved breached-password list and set `tradernet.auth.password.blocklistPath` or `TRADERNET_AUTH_PASSWORD_BLOCKLIST_PATH`.
- Usernames have a persisted NFKC, trimmed, lowercase canonical value in `tblUsers.username_normalized`. The database unique index prevents case/width-equivalent duplicate accounts.
- Local password authentication rejects accounts marked as external identities. Those accounts require their external identity provider.

## Abuse controls and audit

- Failed attempts are serialized with a pessimistic user-row lock. Five failures produce a persisted 15-minute account lockout by default.
- Login and password-reset requests also consume database-backed, source-address rate-limit buckets. The defaults are 60 login attempts and 20 reset attempts per source in five minutes, followed by a 15-minute block. A blocked API request returns HTTP 429 and `Retry-After`.
- Source buckets store SHA-256 source identifiers rather than raw addresses. The API uses `HttpServletRequest.getRemoteAddr()` and never trusts client-supplied forwarding headers. Configure WildFly/Undertow to rewrite the remote address only when traffic arrives through a trusted reverse proxy.
- `AuthenticationAuditService` emits sanitized key/value events through the `com.tradernet.security.audit` logger for login, reset, session rejection, REST authorization, privileged policy mutation, and WebSocket outcomes. Route this logger to access-controlled, append-only centralized storage and alert on lockout/throttle spikes. Passwords and bearer tokens are never logged.

## Sessions and reset tokens

- Session and reset bearer values contain 256 bits of randomness; only SHA-256 token hashes are stored in the database.
- Authenticated sessions have an eight-hour absolute lifetime and a 30-minute idle timeout by default. Account policy is reloaded on every session resolution, so disabled, deleted, expired, locked, or password-change-required users lose access immediately.
- Browser auth cookies are non-persistent, HttpOnly, `SameSite=Strict`, and Secure by default. Local HTTP must explicitly set `TRADERNET_AUTH_COOKIE_SECURE=false` or `-Dtradernet.auth.cookie.secure=false`.
- Authentication responses, including exception-mapped failures, include `Cache-Control: no-store` and `Pragma: no-cache`.
- Password reset cookies last ten minutes by default. Reset rows are keyed by user, so only the newest token can be valid. Issuance and consumption use a consistent `user -> reset row` lock order, and consumption is atomic with the password update.
- A password change revokes all authenticated sessions for that user and immediately closes the user's live market WebSockets. Scheduled tasks remove expired sessions, reset tokens, and throttle buckets.
- The market WebSocket validates the same persisted `GET market` policy as REST, rejects missing or untrusted browser origins, closes matching connections immediately on logout, and revalidates session/account/role eligibility every 30 seconds without extending the idle timeout.

## Authorization administration

- Group and role updates receive the authenticated actor and enforce privilege ceilings inside the service transaction. `Admin Rights` may manage ordinary group membership but cannot grant, remove, or modify an `ALL Rights` assignment; that requires an actor who already holds `ALL Rights`.
- Role and group policy writes use database row locks. Authorization denials and successful privilege changes are emitted through the security-audit logger with the acting username.
- Built-in access-control relationships are seeded only when their role, group, or bootstrap user is first created. Runtime revocations therefore survive restart.
- Role, group, and resource names are unique, and each normalized resource path/method rule is unique. Apply `20260721-harden-authorization-policy.sql` before deploying against an existing database.

## Runtime settings

| Setting | Default | Purpose |
| --- | --- | --- |
| `TRADERNET_AUTH_COOKIE_SECURE` / `tradernet.auth.cookie.secure` | `true` | Secure cookie flag; set false only for explicit local HTTP. |
| `TRADERNET_AUTH_WEBSOCKET_ALLOWED_ORIGINS` / `tradernet.auth.websocket.allowedOrigins` | same origin | Comma-separated browser-origin allowlist for proxied or multi-origin deployments. |
| `tradernet.auth.maxFailedLoginAttempts` | `5` | Account lockout threshold. |
| `tradernet.auth.lockoutDurationSeconds` | `900` | Account lockout duration. |
| `tradernet.auth.password.minimumLength` | `15` | Minimum Unicode code points. |
| `tradernet.auth.password.maximumLength` | `128` | Maximum Unicode code points. |
| `tradernet.auth.password.maximumBytes` | `512` | Maximum normalized UTF-8 bytes. |
| `tradernet.auth.password.argon2.memoryKiB` | `19456` | Argon2id memory cost. |
| `tradernet.auth.password.argon2.iterations` | `2` | Argon2id iterations. |
| `tradernet.auth.password.argon2.parallelism` | `1` | Argon2id lanes. |
| `tradernet.auth.password.blocklistPath` | unset | Additional UTF-8 common/breached-password file, one value per line. |
| `tradernet.auth.rateLimit.login.maxAttempts` | `60` | Login requests per source/window. |
| `tradernet.auth.rateLimit.passwordReset.maxAttempts` | `20` | Reset requests per source/window. |
| `tradernet.auth.rateLimit.windowSeconds` | `300` | Rate-limit counting window. |
| `tradernet.auth.rateLimit.blockSeconds` | `900` | Source block duration. |
| `tradernet.auth.session.absoluteSeconds` | `28800` | Absolute session lifetime. |
| `tradernet.auth.session.idleSeconds` | `1800` | Idle session lifetime. |
| `tradernet.auth.passwordReset.durationSeconds` | `600` | Password-reset token lifetime. |

Malformed or out-of-range security settings fail application startup. The idle timeout cannot exceed the absolute timeout.

Production bootstrap accounts require separate secrets: `TRADERNET_BOOTSTRAP_SUPERUSER_PASSWORD`, `TRADERNET_BOOTSTRAP_ADMIN_PASSWORD`, and `TRADERNET_BOOTSTRAP_STANDARD_PASSWORD`, or the equivalent `tradernet.bootstrap.*Password` Java properties. Accounts without a configured secret are not created. `TRADERNET_BOOTSTRAP_ALLOW_DEFAULT_PASSWORD=true` enables the shared `changeme` fallback only for local development; it must never be enabled in production. Disabling the fallback also rejects any persisted bootstrap account whose hash still matches that known password. Every newly created bootstrap account is forced to change its password at first login.

## Deployment gate

Before deploying this authentication model to an existing PostgreSQL database, apply `20260721-authentication-production-hardening.sql`. It canonicalizes usernames, rejects canonical collisions, invalidates existing sessions/reset tokens, adds idle-session fields and indexes, changes reset-token ownership to one row per user, and creates the shared rate-limit table.

Authentication code readiness does not replace environment controls. Production still requires TLS termination, trusted-proxy configuration, secret-manager delivery, restricted audit-log access, database backup/monitoring, dependency scanning, and a security test of the deployed topology. Privileged or regulated deployments should use MFA through the external identity provider.

WildFly management authentication is separate from application authentication. The container creates no management user by default; set both `ADMIN_USERNAME` and `ADMIN_PASSWORD` only when management access is required. Never expose port 9990 publicly. The entry script does not enable shell tracing, so configured secrets are not echoed into container logs.
