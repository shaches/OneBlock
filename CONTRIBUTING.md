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
    events/                 Bukkit Listeners
    gui/                    Inventory / boss-bar UI
    invitation/             /ob invite + /ob visit state machines
    loot/                   LootTable dispatch
    migration/              One-shot blocks.yml / chests.yml migrators
    pldata/                 JSON / Hikari storage backends
    universalplace/         ItemsAdder / Oraxen / Nexo / CraftEngine adapters
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
- `Oneblock.TaskSaveData` runs on an async scheduler thread. DB / disk writes
  from there must stay thread-safe (HikariCP is; `JsonSimple.Write` is too).
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
- Mutations of `Oneblock.{x, y, z, offset, wor, leavewor}` and the admin
  flag booleans (`CircleMode`, `UseEmptyIslands`, `protection`, etc).

### Async-scheduler threads (`runTaskTimerAsynchronously`)

- `Oneblock.TaskUpdatePlayers` &rarr; reads `Oneblock.wor`; writes `PlayerCache`
  (volatile `ConcurrentHashMap`; safe).
- `Oneblock.TaskSaveData` &rarr; snapshots `PlayerInfo.list` (`CopyOnWriteArrayList`;
  safe) and calls either `DatabaseManager.save` (HikariCP; safe) or
  `JsonSimple.Write` (file-local, no shared state; safe).
- `Oneblock.TaskParticle` &rarr; reads `PlayerCache` (safe); calls
  `World.spawnParticle` which is main-thread-only on most server forks —
  this is an existing caveat, not fixed by Phase 1.
- `Oneblock.Initialization` &rarr; reads `config`, writes `Oneblock.wor/leavewor`.

### Already thread-safe

- `PlayerInfo.list` (`CopyOnWriteArrayList`).
- `PlayerInfo.UUID_INDEX` (`ConcurrentHashMap`).
- `PlayerInfo.TOP_VERSION` (`AtomicLong`).
- `IslandCoordinateCalculator.cellIndex` (`volatile` + `ConcurrentHashMap`).
- `Oneblock.topCache` / `topCacheVersion` (`volatile` + immutable snapshot).
- `PlayerCache.players` (`volatile` + `ConcurrentHashMap`).

### Known races (documented, not yet fixed)

- `Oneblock.{x, y, z, offset}` are plain `static int`. If an admin runs
  `/ob set` at the same tick an async task reads them, the async task may
  observe a torn or partially-updated origin. Tracked for Phase 2.
- `Guest.list` and `Invitation.list` are `ArrayList`. The invite TTL
  cleanup runs on a delayed `runTaskLater` (main thread, so currently
  fine), but if any future event handler mutates these off-thread it
  will `ConcurrentModificationException`. Tracked for Phase 2.
- `Level.levels` is a plain `ArrayList`. `Blockfile` reload clears + refills
  it on the main thread, and async tasks that iterate it during a reload
  can see a half-populated list. Tracked for Phase 2.
- `Oneblock.config` is a plain static that is reassigned from
  `CommandHandler.onCommand` (admin default, line ~267) and read from the
  async load-path in `DatabaseConfig`. Torn reads possible. Tracked for Phase 2.

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
