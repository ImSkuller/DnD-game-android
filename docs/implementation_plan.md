# Runehearth — Implementation Plan

Status legend: ✅ done · 🚧 in progress · ⏳ planned. (Updated at each milestone; see §13.)

## 1. Project overview
**Runehearth** is a medieval-fantasy companion for tabletop dungeon crawls. Two modes:
- **Local Only** — one device, the DM runs everything (characters, economy, dice, combat, quests).
- **Local Co-op** — the DM's phone hosts; players join from their own phones over Wi-Fi/hotspot with no internet
  and no server. The host is authoritative.

Roles: **Dungeon Master** (campaign control) and **Player** (own character sheet).
Repository state at start: empty (only `.vscode`). Toolchain found: JDK 21/25, Android SDK 36, emulator
(Pixel 9 Pro AVD), Gradle distributions in cache, network available for dependency download.

## 2. Application architecture
Single Gradle module `:app`, strict package layering (module split is a future step; boundaries already enforced
by package dependencies: `ui → domain/engine → data`, `net → engine → data`).

```
com.runehearth.app
 ├─ core/        money (Currency), dice (DiceEngine, RollSpec, Classifier), ids, hashing
 ├─ data/        Room entities, DAOs, database, repository, backup/export, preferences
 ├─ engine/      Action (sealed), ActionEngine (validate+apply), StatCalculator, Leveling, ChangeLog
 ├─ net/         Wire protocol, FrameCodec(+HMAC), HostServer, ClientConnection, NSD discovery, SessionManager
 ├─ art/         Procedural vector assets (AssetRef, AssetCatalog, painters, palette, glow, coins, UI textures)
 ├─ ui/          Theme (tokens), components, screens (home, dm/*, player/*, shared/*), navigation
 └─ MainActivity / RunehearthApp
```
Pattern: unidirectional data flow. ViewModels expose `StateFlow`s from Room `Flow`s; UI sends `Action`s to a
`GameSession` facade. The facade is either **LocalSession** (host or local-only: runs `ActionEngine` directly)
or **RemoteSession** (client: sends the action to the host and observes the local mirror DB).

## 3. Technology stack and justification
| Concern | Choice | Why / rejected alternatives |
|---|---|---|
| Platform | Native Android (Kotlin, Compose) | Kotlin Multiplatform rejected: iOS not required, adds build/tooling cost; the engine layer is pure Kotlin so KMP extraction stays possible later. |
| UI | Jetpack Compose + custom components | Fully custom medieval look; Canvas gives vector art, glow and particles with no bitmap storage. Material3 only as a base (text selection, ripple), never default-looking. |
| DB | Room (KSP) over SQLite | Compile-time-checked queries, Flow, migrations with exported schemas, indexed. |
| Prefs | DataStore (Preferences) | Async, atomic app preferences (theme, thresholds, device id). |
| Serialization | kotlinx.serialization (JSON) | Wire frames, change payloads, export. Reflection-free, small. |
| Concurrency | Kotlin coroutines/Flow | Structured cancellation for sockets & discovery. |
| Networking | Plain TCP sockets + `NsdManager` (mDNS) + manual IP fallback | No Play Services dependency (Nearby Connections needs GMS, poor on emulators/AOSP); works on any shared Wi-Fi or hotspot; no internet needed. Wi-Fi Direct evaluated: pairing UX & group-owner constraints, flaky on many OEMs → documented future transport behind `Transport` interface. Bluetooth: low bandwidth, rejected. |
| Assets | Procedural vector painters + palette recolor + glow in Canvas | 20 designs × 20 colours × glow = 800 combos from code, ~0 KB of images. Rasters (WebP) reserved for optional future illustrations. |
| Fonts | System serif with small-caps/letter-spacing styling (optional bundled OFL font) | Keeps APK small. |
No other runtime dependencies. Dev-only: JUnit, kotlinx-coroutines-test, Room testing.

## 4. Database design (Room, version 1, exported schema)
All ids are UUID strings (stable; updates are `UPDATE`/`UPSERT`, never duplicate rows). Money = `Long` bronze units.
Per-entity `rev` (Long) increments on each change (version tracking).

| Table | Key columns | Indexes |
|---|---|---|
| `campaign` | id, name, mode(LOCAL/COOP), seed, hostDeviceId, settings(json: roll thresholds, rules), lastSeq, rev, createdAt, updatedAt | – |
| `character` | id, campaignId, name, art, level, exp, className, job, race, background, description, base{Hp,Mp,Atk,Def,Spd}, curHp, curMp, str/dex/con/int/wis/cha, notes, ownerDeviceId, isNpc, rev | campaignId |
| `item` | id, campaignId, ownerId(character or null=party stash), name, category, slot, rarity, description, quantity, weight, art, equipped, bonus{Hp,Mp,Atk,Def,Spd}, effectJson, rev | (campaignId, ownerId), ownerId |
| `skill` | id, characterId, name, level, description, rarity, manaCost, cooldown, power, kind(DAMAGE/HEAL/BUFF/UTILITY), statMods, effect, art, rev | characterId |
| `status_effect` | id, targetId, name, description, isBuff, turns, mod{Hp,Mp,Atk,Def,Spd}, art | targetId |
| `wallet` | id, campaignId, ownerId(null=party), balance (bronze), hidden, rev | (campaignId, ownerId) unique |
| `money_tx` | id, campaignId, ts, fromWallet, toWallet, amount, reason, actor | campaignId+ts |
| `entry` | id, campaignId, type(QUEST/NPC/LOCATION/LORE/EVENT/NOTE/ACHIEVEMENT/REWARD), title, body, status, ownerId, rewardBronze, art, isPublic, rev | (campaignId,type) |
| `encounter` | id, campaignId, name, active, round, turnIndex | campaignId |
| `combatant` | id, encounterId, characterId?, name, initiative, hp, maxHp, defense, isPlayer, art, statusJson, sortOrder | encounterId |
| `roll` | id, campaignId, ts, label, expr, resultsJson, modifier, total, tier, crit, isPublic, auditHash, idx | campaignId+idx |
| `event_log` | seq (autoincrement), campaignId, ts, type, text, actor | campaignId+seq |
| `change` | campaignId, entityType, entityId (composite PK), seq (unique per campaign), audience(null=all, characterId, "DM"), payload(json), deleted | (campaignId, seq) |
| `member` | deviceId, campaignId, characterId, name, token, lastSeen, lastSeq | campaignId |
| `processed_action` | actionId PK, deviceId, ts, ok, message | deviceId+ts (pruned to last 200/device) |

`change` is a **compacted** change log: one row per entity (upserted with a new seq on each modification), so it
never grows beyond the number of entities. A reconnecting client asks for `seq > lastSeq` and receives only the
latest state of entities changed since. Deletions are tombstones (pruned after 30 days).
Large/rare data (history) lives in `money_tx`, `roll`, `event_log`, capped by pruning (default 2,000 rows/table).
Transactions (`withTransaction`) wrap every action application: currency transfer, item transfer, equip, combat.
Migrations: `Migration` objects + `exportSchema = true`; migration test via `MigrationTestHelper`.

## 5. Data models
`AssetRef(category, design, color, glow)` encodes to `"d.c.g"` in a single column. Stats: base + equipment bonuses +
status modifiers = final (`StatCalculator`, pure & unit-tested). Level curve: `expToNext(L) = 50·L·(L+1)`;
level-up grants +5 HP, +3 MP, +1 ATK/DEF/SPD (rules configurable per campaign later).
Currency: 1 silver = 10 bronze; 1 gold = 100 bronze; 1 holy gold = 2000 bronze. `Money.breakdown(total)` greedy.

## 6. Multiplayer architecture
- **Host (DM device)** runs `HostServer` (TCP, `ServerSocket` on ephemeral port) while a co-op session is open and
  advertises `_runehearth._tcp` via NSD. A 6-digit **pairing code** is shown on the host.
- **Client** discovers hosts (NSD) or enters IP:port, then pairs with the code.
- Host is the only writer of authoritative state. Clients keep a **local mirror** (same Room schema, filtered to what
  the player is allowed to see) for offline-readable data and instant UI.
- Authority is never transferred silently. **Host migration (documented, not automatic):** the DM exports a
  snapshot (`.rhcamp`) → another device imports it as a new host campaign with `hostDeviceId` changed and
  `epoch+1`; clients must re-pair, and any client with a lower epoch is refused until it resyncs. Majority voting is
  not used.
- Crash recovery: host state is in SQLite (WAL); on restart the DM reopens the campaign and sessions resume;
  clients reconnect with their `lastSeq`.

## 7. Synchronization protocol (v1)
Frame: `[len:u32][payload JSON][mac:16B]`; `mac = HMAC-SHA256(sessionKey, seqNo||payload)[0..16]`; sessionKey =
`HMAC(pairingCode, hostNonce||clientNonce)`. Max frame 1 MiB.
Handshake: `Hello(proto, deviceId, name, campaignId?, lastSeq, epoch, digest)` ⇄ `Challenge(nonce)` ⇄ `Auth(proof)`
→ `Welcome(campaignMeta, claimableCharacters, currentSeq)` → `Claim(characterId | newName)` → `Snapshot|Delta`.
Messages: `ActionMsg(action)` → `ActionResult(actionId, ok, message)`; `Delta(fromSeq,toSeq,changes[])` broadcast to
each client filtered by `audience`; `Ping/Pong` every 5 s, 15 s timeout; `Digest` check.
- **Incremental**: after every applied action the host emits the changed entities (seq-stamped) to subscribed clients.
- **Duplicate prevention**: `Action.id` (UUID) recorded in `processed_action`; replays return the stored result.
- **Conflict handling**: all writes serialize through the host's single `ActionEngine` mutex; entity `rev` is
  checked for optimistic player edits (`expectedRev`) → `ActionResult(ok=false, "stale")` and the client receives the
  fresh entity.
- **Integrity / anti-cheat (replaces "everyone compares"):** the host validates every action (ownership,
  balances, mana, quantity, permissions). On (re)connect the client sends a **digest** (CRC32-folded hash of its
  owned entity revs); on mismatch the host sends a repair `Delta(full for that player)`. Players cannot write
  authoritative stats; the host rolls all dice.
- **Reconnect**: client retries with exponential backoff (0.5 s→8 s), re-handshakes with `lastSeq`.
- Target: sub-second propagation on a LAN; **measured** in `docs/performance.md`.

## 8. Security and data integrity
Pairing-code challenge-response (code never sent), per-frame HMAC, host-side validation, bounded frame sizes, input
sanitation (name/text length caps), no remote endpoints, `allowBackup=false` for game DB (we provide explicit export).
Not encrypted: LAN traffic is authenticated, not confidential (documented limitation). Dice: `SecureRandom`,
rejection-sampled bounded ints; every roll carries a UUID, per-campaign index, and a hash-chain `auditHash =
SHA-256(prevHash‖id‖expr‖results)` seeded from the campaign seed → tamper-evident audit trail.

## 9. Asset pipeline
Everything is drawn from code: `AssetCatalog` maps `(category, design 0..N-1)` → a painter producing layered
shapes in a 100×100 space with *tones* (PRIMARY, SHADE, LIGHT, METAL, WOOD, GEM, OUTLINE). `Palette` provides 20
colour ramps; recolor swaps PRIMARY/SHADE/LIGHT/GEM ramps. **Glow** = soft radial halo + animated shimmer sweep
(Minecraft-enchant style) drawn only when enabled and visible. Built `Path`s are cached per design.
Counts: weapons 20, off-hand/shields 20, helmets 20, chestplates 20, gloves 20, leggings 20, boots 20,
accessories 20, potions 20, runes 30, herbs 20, books 10, chests 20, materials 20, misc 20, monsters 20.
Coins: 4 materials, Chinese cash coin with **diamond-shaped** hole, rim, glyph marks. UI chrome (parchment, wood,
stone, leather, metal frames, corner ornaments, dividers) is procedural with cached gradient brushes.
Launcher icon & logo: vector drawable (adaptive). Total asset payload ≈ code size only (no PNG/WebP).
Asset picker UI: grid of designs with search/category filter → tap → colour row (20 swatches) + glow toggle + live
preview → save.

## 10. UI/UX architecture
`RunehearthTheme` exposes a `ThemeTokens` object (colours, shapes, textures, typography, sounds-off) via
`CompositionLocal`; `Medieval` is the only implementation, others can be added by implementing `ThemeTokens`.
Custom components: `Panel`, `WoodButton`, `Plaque`, `RuneTabBar`, `StatBar`, `CoinPurse`, `ItemCard`, `Dialogs`,
`TextField`, `Toast`. Navigation: tiny typed back-stack in a ViewModel (no extra dependency), animated crossfade/slide.
Animations: dice tumble, number roll-up, coin count tween, tab transitions, glow shimmer, ember particles on home.
Empty/loading/error states are explicit components.

## 11. Performance strategy
Lazy DB open off main thread; Compose lazy lists with stable keys; `derivedStateOf`/`remember` for calc; draw-phase
animations (`graphicsLayer`, `drawBehind`, `Animatable` read in draw); cached `Path`s and `Brush`es; single DB
transaction per action; indexed queries; history pruning; no polling (Flow + socket push); discovery only on the
join/host screens; baseline profile is a future task. Measurements recorded in `docs/performance.md`.

## 12. Testing strategy
JVM unit tests: currency conversion, stat calculation & levelling, inventory stacking, dice distribution
(chi-square on an injected source + bounds/modulo-bias checks), classification, audit chain, wire codec/HMAC,
action validation, duplicate actions, stale-rev conflicts, change-log compaction & incremental delta.
Instrumented/Robolectric-free where possible: Room tests via in-memory DB on device/emulator (persistence,
migrations, offline). Multiplayer: in-process loopback test (host+2 clients over `127.0.0.1`) simulating
disconnect/reconnect; real two-emulator/two-device manual script in README section. Performance: startup via
`adb shell am start -W`, memory via `dumpsys meminfo`, frame stats via `dumpsys gfxinfo`, DB/sync timings in tests.

## 13. Development milestones
1. ✅ Project initialization & docs 2. Architecture/DB foundation 3. Design system & theme 4. Navigation
5. DM interface 6. Player character system 7. Inventory & equipment 8. Economy 9. Dice & RNG
10. Combat & campaign tools 11. Local multiplayer 12. Asset library 13. Animations & polish 14. Testing &
optimisation. (Executed as ≤25 commits, order adjusted so dependencies come first: core/data → engine → art → UI → net.)

## 14. Risks, limitations, decisions
- Procedural art is stylised (vector), not hand-painted illustration; fidelity depends on painter quality.
  Mitigation: painter API is layered; WebP overrides can be added per design later.
- NSD discovery is unreliable on some routers (client isolation, guest Wi-Fi) → manual IP entry + hotspot advice.
- Android may kill the host app in background; a foreground service is a future task (documented debt).
- LAN traffic authenticated but not encrypted. Wi-Fi Direct/BT transports not implemented.
- Co-op players see only their own character in v1 (+party data); no spectator/multi-character devices.
- Host migration is manual (export/import) by design.
- Instrumented tests need an emulator; multi-device validation is partly manual.
- Decision log: ADR-1 native Kotlin; ADR-2 TCP+NSD instead of Nearby; ADR-3 procedural vector assets; ADR-4
  compacted change log instead of event replay; ADR-5 host-authoritative instead of majority comparison (the
  original "all phones compare stats" idea is replaced by digest verification against the host, per project rules).
