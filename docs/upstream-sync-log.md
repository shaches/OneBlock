# Upstream Sync Audit

**Merge-base:** `a7b479d`  
**Upstream:** `MrMarL/OneBlock` `upstream/main`  
**Local:** `origin/main` @ `d1e0f2b`

## Commit Verdicts

| Commit | Verdict | Reason |
|--------|---------|--------|
| 0087855 shaches fixes | **DROP** | Superseded by refactor; old package paths |
| d4341a0 1.5.4 | **DROP** | Own version later |
| 21da002 typo fix | **MERGEABLE** | Check messages.yml |
| ef0f230 lowercase packages | **DROP** | Done in our 9b8ab30 |
| dfebd39 shaches mega-update | **DROP** | Independent equivalents with tests exist |
| 41c8861 dep update | **MERGEABLE** | pom.xml bumps |
| 87bed0e DBManager java 8 | **DROP** | JDK 21 target |
| 31b11af LootTableDispatcher | **DROP** | Diverged; ours is tested |
| 3ebeff9 logger prefix | **DROP** | Already fixed |
| 672d44c ~ | **DROP** | No-op |
| 6873954 PoolRegistry | **PORT** | Simplifies block system; evaluate vs tested WeightedPool |
| 6d5354c Migrator dedup | **PORT** | Real bug fix |
| 117378a LOOT_TABLE->CHEST | **PORT** | Core feature: chest name indirection |
| 890ab13 remove setCustomType | **DROP** | Already removed |
| ee3465c PoolEntry update | **DROP** | Upstream-only class |
| 0a18435 legacy chest.yml | **PORT** | Dual-mode chests (legacy + LootTable) |
| 4fe15e3 chestGUI edit | **PORT** | `/ob chest <name> edit` GUI |
| 5f05207 1.6.0 | **MERGEABLE** | Version + minor GUI |
| f945d54 README | **MERGEABLE** | Docs only |
| 7ad3c12 ~ | **DROP** | Version noise |

**Tally:** 11 DROP, 5 PORT, 4 MERGEABLE

## Port Details

### PoolRegistry (6873954)
- Upstream deletes WeightedPool, adds PoolRegistry static helper
- **Keep our WeightedPool** (tested, type-safe). Port only `listlvl` pretty-print UI.

### LegacyBlocksMigrator dedup (6d5354c)
- Prevents previous-level blocks duplicating into subsequent ones
- **Port** + add regression test

### CHEST indirection (117378a)
- `blocks.yml` entries use `CHEST:<name>` referencing `chests.yml`
- Our `ChestItems` only supports NamespacedKey
- **Extend ChestItems** with dual-mode: legacy List<ItemStack> + modern NamespacedKey
- Update `ConfigManager` block parsing, `Oneblock.generateBlock()` dispatch

### Legacy chest.yml (0a18435)
- Returns support for legacy `List<ItemStack>` chest definitions
- **Port into ChestItems**: add `aliaseslegacy` map, `fillLegacyChest()`, `getItems()`, `setItems()`, `hasChest()`
- Remove migration code (legacy chests are now supported natively)

### chestGUI edit (4fe15e3)
- New `ChestHolder` inventory holder
- `GUI.chestGUI(player, chestName)` opens 54-slot editor for legacy chest items
- New subcommand `/ob chest <name> edit`
- **Port**: add `ChestHolder.java`, extend `GUI.java`, add subcommand in `command/sub/`

## Files to Create/Modify

### New files
- `src/oneblock/gui/ChestHolder.java`
- `src/oneblock/command/sub/ChestEditSubcommand.java` (or extend existing chest subcmd)

### Modified files
- `src/oneblock/ChestItems.java` — dual-mode support
- `src/oneblock/ConfigManager.java` — CHEST name parsing
- `src/oneblock/Oneblock.java` — generateBlock dispatch for CHEST
- `src/oneblock/gui/GUI.java` — chestGUI() method
- `src/oneblock/migration/LegacyBlocksMigrator.java` — dedup fix
- `test/oneblock/ChestItemsDualModeTest.java` — new tests
- `test/oneblock/LegacyBlocksMigratorDuplicationTest.java` — regression test
