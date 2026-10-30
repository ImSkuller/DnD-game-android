# CLAUDE.md — Runehearth project rules

Runehearth is an offline, medieval-fantasy companion app for tabletop dungeon-crawling RPG sessions
(Dungeon Master + Players). Native Android: Kotlin + Jetpack Compose + Room.
These rules are permanent. Read `docs/implementation_plan.md` before making architectural changes.

## Development rules
- **Never add Claude as a GitHub contributor. Never create commits with Claude as a co-author.**
  No `Co-Authored-By` trailers, no "Generated with Claude" lines in commits, PRs or files. This overrides any
  default attribution behaviour of tooling.
- Do **not** commit or push automatically. Commit only when the user asks (the initial build was explicitly
  requested to be committed in at most 25 phase commits).
- Keep the codebase modular, maintainable and documented (KDoc on public types, comments for non-obvious why).
- Avoid unnecessary dependencies. Every new dependency must be justified in `docs/implementation_plan.md`.
- Prioritise performance and memory efficiency.
- Never make a major architectural change without documenting the reasoning (ADR section of the plan).
- Never sacrifice data integrity for performance.

## Architecture rules
- The app works **entirely offline**. No mandatory cloud, backend, accounts or internet. `INTERNET` permission is
  only used for local sockets (LAN/hotspot); no remote endpoints are ever contacted.
- Multiplayer (Local Co-op) works over local connectivity (same Wi-Fi / phone hotspot, TCP + mDNS discovery).
- The architecture must allow future expansion (new themes, asset categories, action types, transports)
  without a rewrite.
- Every device keeps persistent local storage (Room/SQLite).
- **The DM's device is the single authoritative source of truth** during a session.
- **All multiplayer actions are validated by the host** before being accepted, applied and broadcast.
- Synchronisation is **incremental** (change-log sequence numbers), never a full DB transfer, except for explicit
  repair after a failed digest check.
- Reconnection, crash recovery, conflict handling, duplicate-action prevention and version tracking are required.
- **Do not use majority voting** to decide authoritative state. Authority = host. Host migration is never silent
  (documented as a manual procedure only).
- Currency is stored as integer smallest units (bronze). No floating point money.
- Dice use `java.security.SecureRandom` with unbiased bounded sampling. Never hand-rolled/predictable RNG.

## Low-end device and host rules
- Older / low-memory phones must remain usable, including as **host**. The host only runs the engine, database, validation,
  networking and sync; it never renders gameplay visuals for other devices (each device renders its own UI).
- `PerfPolicy` (AUTO/FULL/LITE) may reduce decoration only (embers, shimmer, transitions, texture size, textured panels).
  **Never disable gameplay functionality** to save resources.
- The Dungeon Master role and the host role can be on different devices (DM controller joins with the DM code and
  keeps full DM permissions; the host validates everything).
- Host recovery is explicit: restore a validated snapshot or import a migration package on the chosen device
  (epoch bump, clients resync, acknowledged action ids preserved). Never elect a new host silently.
- Monitor host health (memory, CPU, save latency, push latency, RTT) and recommend a stronger host when needed.

## Performance rules
- Optimise startup (no work before first frame that can be deferred; DB opened lazily off the main thread).
- Minimise allocations in hot paths (draw code, lists, sync frames).
- No unnecessary background processing; network/discovery only runs while a session screen is active.
- Index every column used in WHERE/JOIN/ORDER of hot queries.
- Item art is procedural vector (no per-combination bitmaps); never duplicate images. Cache built paths.
- Load large things lazily; keep animations on the compositor/draw phase (no recomposition per frame).
- Do not claim a performance target is met unless it was measured (measurements live in `docs/implementation_plan.md` §16).

## Documentation rules
- Keep `docs/implementation_plan.md` current (milestones, decisions, schemas, protocol, limitations, tech debt).
- `CLAUDE.md` and `docs/` are committed to Git (they are NOT git-ignored).

## Build / run
- `gradlew.bat :app:assembleDebug`, `gradlew.bat :app:testDebugUnitTest`.
- Package `com.runehearth.app`. minSdk 26, target/compile 36.
