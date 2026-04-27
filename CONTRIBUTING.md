# Contributing to OneBlock

Thanks for your interest in improving OneBlock. This document explains the
project layout, local tooling, test expectations and static-analysis gates
that every contribution is expected to pass before merge.

## Project layout

```
src/
  plugin.yml                Bukkit descriptor (name: Oneblock, main: oneblock.Oneblock)
  config.yml blocks.yml ... Shipped resources copied into the jar verbatim
  oneblock/                 Main Java source tree (package `oneblock`)
    command/                Subcommand interface, router context, /ob arg parsing
    command/sub/            One class per /ob subcommand (~25 + bool toggles)
    config/                 Settings (admin-flag holder, volatile fields)
    events/                 Bukkit Listeners
    gui/                    Inventory / boss-bar UI
    invitation/             /ob invite + /ob visit state machines
    loot/                   LootTable dispatch
    migration/              One-shot blocks.yml / chests.yml migrators
    placement/              ItemsAdder / Oraxen / Nexo / CraftEngine adapters
    storage/                JSON / Hikari player-data backends
    tasks/                  Top-level Runnable scheduler tasks
    utils/                  Shared helpers
    worldguard/             WorldGuard region integration shims
test/
  oneblock/                 JUnit 5 + AssertJ + Mockito test suite
```

The build deliberately uses a non-standard Maven layout (`src` / `test`
instead of `src/main/java` / `src/test/java`) configured via `<sourceDirectory>`
and `<testSourceDirectory>` in `pom.xml`. Stay inside these trees; do NOT add
files under `src/main/java` etc.

## Tooling requirements

| Tool | Version | Source |
| --- | --- | --- |
| JDK         | 21 (LTS)         | [Temurin](https://adoptium.net/) |
| Apache Maven| 3.9.15           | Vendored under `resources/apache-maven-3.9.15/` |
| Git         | 2.40+            | https://git-scm.com/ |

The vendored Maven copy is the only supported build path — `JAVA_HOME` must
point to a JDK 21 install, but Maven should be invoked via the script under
`resources/`.

## Common commands

```powershell
# Run all unit tests
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B test

# Compile only, no tests
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B -q -DskipTests compile

# Produce the shaded plugin jar (target/Oneblock-<version>.jar)
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B -DskipTests clean package

# Full verify (tests + shade + static-analysis gates once Phase 7 lands)
.\resources\apache-maven-3.9.15\bin\mvn.cmd -B verify
```

## Test expectations

Every PR must:

1. Keep the existing test suite green (JUnit 5 + AssertJ). Tests run via the Surefire plugin
   with JUnit 5 Platform; no tests may be `@Disabled` without an accompanying
   issue link.
2. Add tests for any new business logic. Pure-Java logic belongs in a
   `*Test.java` next to an existing suite; behaviors that touch Bukkit statics
   should be exercised via Mockito / MockedStatic rather than a live server.
3. Avoid loading `oneblock.Oneblock` in tests. Its `<clinit>` calls
   `XMaterial.supports(...)` which fails outside a real Bukkit runtime.

## Design constraints

The refactor that produced the current layout enforces these invariants.
Please don't break them:

- `PlayerInfo.list` is a `CopyOnWriteArrayList`; mutations must go through
  `PlayerInfo.set(...)`, `PlayerInfo.replaceAll(...)`, `addInvite/removeInvite`
  or `removeUUID`. Direct `.list.add / .list.set` bypasses the reverse
  `UUID → island id` index.
- `oneblock.tasks.PlayerDataSaveTask` runs on an async scheduler thread. DB
  / disk writes from there must stay thread-safe (HikariCP is;
  `oneblock.storage.JsonPlayerDataStore.write` is too).
- `IslandCoordinateCalculator` caches a spatial cell index that's invalidated
  on every `PlayerInfo.set` / `replaceAll`. New mutation paths must call
  `IslandCoordinateCalculator.invalidateCellIndex()` or go through PlayerInfo.
- Permission strings in `plugin.yml` and string literals like
  `"Oneblock.set"` are PART OF THE PUBLIC API — never rename them.

## Thread-safety snapshot

Documents current concurrency expectations so future changes don't
accidentally widen a data race. Check back here before touching any
static field on `Oneblock`, `Level`, `Guest`, or `Invitation`.

### Main thread only (Bukkit event / command dispatch)

- `CommandHandler.onCommand` and every subcommand path it reaches.
- `Oneblock.setPosition`, `Oneblock.setLeave`, `Oneblock.reload`.
- All `@EventHandler` methods in `oneblock.events.*` and `oneblock.gui.GUIListener`.
- Swaps of `Oneblock.ORIGIN` (the `(world, x, y, z, offset)` record) and
  writes to `Oneblock.leavewor` / `Oneblock.settings()` admin-flag
  booleans (`circleMode`, `useEmptyIslands`, `protection`, etc.). Reads
  of these may happen from any thread.

### Async-scheduler threads (`runTaskTimerAsynchronously`)

- `oneblock.tasks.PlayerCacheRefreshTask` &rarr; reads `Oneblock.getWor()`; writes `PlayerCache`
  (volatile `ConcurrentHashMap`; safe).
- `oneblock.tasks.PlayerDataSaveTask` &rarr; delegates to `Oneblock.saveData()`
  which snapshots `PlayerInfo.list` (`CopyOnWriteArrayList`; safe) and
  calls either `DatabaseManager.save` (HikariCP; safe) or
  `JsonPlayerDataStore.write` (file-local, no shared state; safe).
- `oneblock.tasks.IslandParticleTask` &rarr; reads `PlayerCache` (safe) and
  computes the per-player particle locations on the async thread, then
  hands the resulting list to `Bukkit.getScheduler().runTask(...)` so
  every `World.spawnParticle` call happens on the main thread (Phase
  4.3). `collectSpawns(...)` is exposed package-private and exercised by
  `IslandParticleTaskTest`.
- `oneblock.tasks.WorldInitTask` &rarr; reads `Oneblock.config`, swaps
  `Oneblock.ORIGIN` via `updateOriginWorld` (atomic) and writes
  `Oneblock.leavewor` (volatile).
- `oneblock.tasks.IslandBlockGenTask` runs on the **main** thread
  (`runTaskTimer`, not async); reads `Oneblock.settings().protection`,
  iterates `Guest.list` (`CopyOnWriteArrayList`; safe even if a future
  off-thread mutator is added), and dispatches block generation through
  `Oneblock.generateBlock`.

### Already thread-safe

- `PlayerInfo.list` (`CopyOnWriteArrayList`).
- `PlayerInfo.uuids` (`CopyOnWriteArrayList`; Phase 4.4 &mdash; main-thread
  invitee mutations vs. async `PlayerDataSaveTask` iteration).
- `PlayerInfo.UUID_INDEX` (`ConcurrentHashMap`).
- `PlayerInfo.TOP_VERSION` (`AtomicLong`).
- `Guest.list` and `Invitation.list` (`CopyOnWriteArrayList`; Phase 4.2).
- `Level.levels` (`private static volatile List<Level>`; Phase 4.1 &mdash;
  publication-safe immutable swap. Mutate only via `Level.replaceAll`,
  read either via `Level.get(int)` / `Level.size()` or by capturing
  `Level.snapshot()` once at the top of an iteration).
- `IslandCoordinateCalculator.cellIndex` (`volatile` + `ConcurrentHashMap`).
- `Oneblock.topCache` / `topCacheVersion` (`volatile` + immutable snapshot).
- `PlayerCache.players` (`volatile` + `ConcurrentHashMap`).
- `Oneblock.ORIGIN` (`AtomicReference<IslandOrigin>`; single-swap writes via
  `updateAndGet`, readers snapshot via `Oneblock.origin()` &mdash; see
  `IslandOriginTest` and `OneblockOriginConcurrencyTest`).
- `Oneblock.leavewor`, `Oneblock.config`, and every admin-flag field on
  `oneblock.config.Settings` (`circleMode`, `useEmptyIslands`,
  `protection`, `border`, `autojoin`, `droptossup`, `physics`,
  `lvl_bar_mode`, `particle`, `allow_nether`, `saveplayerinventory`,
  `progress_bar`, `island_for_new_players`, `rebirth`, `phText`,
  `max_players_team`, `mob_spawn_chance`) are `volatile` for cross-thread
  visibility of single-value writes. Reach the singleton via
  `Oneblock.settings()`.

### Fixed in Phase 2

- `Oneblock.{x, y, z, offset, wor}` were four plain `static int`s and one
  plain instance `World`; a concurrent `/ob set` admin call could expose
  a torn origin (e.g. new `x` with old `offset`) to the async
  `Task` / `TaskParticle` / `Initialization` readers. They were folded
  into an immutable `IslandOrigin` record published through
  `Oneblock.ORIGIN` (`AtomicReference`); writers swap the record in one
  `updateAndGet` and readers call `Oneblock.origin()` once.
- `Oneblock.config`, `Oneblock.leavewor`, `Oneblock.phText`, and the
  admin-flag booleans are now `volatile` so writes from
  `CommandHandler.onCommand` / `ConfigManager` become visible to async
  scheduler threads without a happens-before from an unrelated sync.

### Fixed in Phase 3

- `Oneblock` itself was a 522-line god-object holding admin flags,
  inner `Runnable` task classes, and the dispatch machinery. Phase 3
  decomposed it into:
  - `oneblock.config.Settings` — ~17 admin flags as `volatile`
    instance fields, one source of truth, accessed via
    `Oneblock.settings()`.
  - `oneblock.tasks.*` — the 5 inner `Runnable` classes
    (`Initialization`, `TaskUpdatePlayers`, `TaskSaveData`,
    `TaskParticle`, `Task`) became top-level `WorldInitTask`,
    `PlayerCacheRefreshTask`, `PlayerDataSaveTask`,
    `IslandParticleTask`, `IslandBlockGenTask`. Each takes a
    constructor-injected `Oneblock plugin`.
  - `oneblock.command.*` — the 649-line `CommandHandler.onCommand`
    switch (originally a single 25-case nested switch) was replaced by
    a `Subcommand` interface + `CommandContext` record + a
    router-fallback dispatch on the surviving 167-line
    `CommandHandler`. Each of the 25 subcommands is a separate class
    under `oneblock.command.sub.*`. The 11-flag bool-toggle group
    shares one parameterised `BoolToggleCommand` impl.
  - `oneblock.storage.*` — the `pldata` package was renamed and the
    classes inside (`JsonSimple`, `ReadOldData`) renamed to
    `JsonPlayerDataStore`, `LegacyYamlPlayerDataStore`. Method names
    on the storage layer also follow Java conventions now
    (`Read`/`Write` → `read`/`write`).
  - `oneblock.placement.*` — the `universalplace` package renamed.
- Method renames (PascalCase → camelCase) on the public-Java surface:
  `BlockGen`→`generateBlock`, `GetId`→`getId`, `Configfile`→
  `loadMainConfig`, `Check(...)`→`readOrDefault(...)`,
  `ReCreateRegions`→`recreateRegions`, etc. (~30 methods total).
- Field renames `CircleMode`→`circleMode`, `UseEmptyIslands`→
  `useEmptyIslands` (now on `Settings`), `OBWG`→`worldGuard`. The
  on-disk YAML keys are unchanged.
- `PlayerInfo` and `AbstractInvitation` got proper `equals` +
  `hashCode` keyed on the natural identity (UUID for `PlayerInfo`,
  `(inviting, invited)` pair for `AbstractInvitation`).
- 13 non-subclassed concrete classes marked `final`.
- Test count grew 83 → 104 (+21 across `IslandOriginTest`,
  `OneblockOriginConcurrencyTest`, `SettingsTest`,
  `PlayerInfoEqualsTest`, `AbstractInvitationEqualsTest`).

### Fixed in Phase 4

Phase 4 retires the four entries that the pre-Phase-4 "Known races"
section used to call out. Every entry below has a regression test that
fails (CME or torn read) if reverted:

- **4.1 &mdash; `Level.levels` publication-safe.** The field was a public
  mutable `ArrayList` that `ConfigManager.loadBlocks` would
  `clear()`-then-refill on every `/ob reload`, while the async
  `IslandBlockGenTask` could be mid-iteration via `Level.get(int)`. It
  is now a `private static volatile List<Level>` initialised to
  `Collections.emptyList()`. `ConfigManager.loadBlocks` parses into a
  local `ArrayList` and only calls `Level.replaceAll(...)` after the
  whole file is parsed without throwing &mdash; readers either see the old
  list or the complete new one, never a half-populated one. Covered by
  `LevelPublicationTest` (3 tests).
- **4.2 &mdash; `Guest.list` + `Invitation.list` &rarr; `CopyOnWriteArrayList`.**
  Both lists are mutated from the main thread (`/ob invite`,
  `/ob accept`, the `runTaskLater` TTL cleanup) and iterated from the
  same thread by `IslandBlockGenTask.run` and `Guest.check` /
  `Invitation.check`. They were previously `ArrayList` and would CME
  the moment any future handler mutated them off-thread. Covered by
  `GuestInvitationConcurrencyTest` (4 tests).
- **4.3 &mdash; `IslandParticleTask.spawnParticle` on the main thread.** The
  task ran via `runTaskTimerAsynchronously` and called
  `World.spawnParticle` directly &mdash; main-thread-only on Paper /
  Folia / most server forks, undefined behaviour on the rest. The
  task now collects per-player particle locations on the async tick
  via `collectSpawns(...)` (a pure helper, package-private for
  testability), then hops back to the main thread via
  `Bukkit.getScheduler().runTask(plugin, () -> ...)` to fire the
  particles. Covered by `IslandParticleTaskTest` (4 tests).
- **4.4 &mdash; `PlayerInfo.uuids` &rarr; `CopyOnWriteArrayList`.** The per-island
  invitee/co-owner list was iterated by the async `PlayerDataSaveTask`
  &mdash; via `JsonPlayerDataStore.write` and `DatabaseManager.save` &mdash;
  every 6 000 ticks, while the main thread can concurrently call
  `addInvite` / `removeInvite` / `removeUUID`. An unlucky overlap
  threw `ConcurrentModificationException` out of the save loop and
  lost the entire tick's player data. The PlaceholderAPI hook in
  `OBP.onRequest` (`%OB_number_of_invited%`) compounded the exposure
  since PAPI may dispatch from any thread. Covered by
  `PlayerInfoUuidsConcurrencyTest` (2 tests).

Test count grew 104 → 117 (+13 across the four suites above).

## Static analysis (Phase 7)

The CI pipeline runs (or will run, once Phase 7 lands):

- **JUnit 5 Jupiter + AssertJ** — unit tests
- **Spotless (google-java-format)** — format enforcement (`mvn spotless:apply` to fix)
- **SpotBugs + find-sec-bugs** — static bug / security analysis
- **JaCoCo** — coverage report

Failure of any gate blocks merge. Run `mvn verify` locally before pushing.

## Submitting a change

1. Fork + branch: `feature/<short-name>` or `fix/<short-name>`.
2. Run `mvn verify` until green.
3. Open a PR against `main`. Include:
   - What changed and *why* (root cause, not just the fix).
   - A mention of any invariant from the "Design constraints" section you touched.
   - New tests or a justification for the absence of new tests.
4. Expect review from a maintainer within a few days.
