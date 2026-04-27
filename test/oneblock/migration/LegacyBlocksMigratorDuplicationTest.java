package oneblock.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import oneblock.config.Settings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for LegacyBlocksMigrator dedup fix. Verifies that PER_LEVEL migration prevents
 * cross-level leakage and that CUMULATIVE migration preserves the union of all levels.
 */
@DisplayName("LegacyBlocksMigrator dedup regression tests")
class LegacyBlocksMigratorDuplicationTest {

  @TempDir File tempDir;

  private File blocksFile;
  private File chestsFile;

  @BeforeEach
  void setUp() throws Exception {
    blocksFile = new File(tempDir, "blocks.yml");
    chestsFile = new File(tempDir, "chests.yml");
    chestsFile.createNewFile();
  }

  @Test
  @DisplayName("PER_LEVEL migration: each level contains only its own entries")
  void perLevelMigration_eachLevelContainsOnlyOwnEntries() throws Exception {
    // Create legacy blocks.yml with 2 levels with overlapping entries
    YamlConfiguration legacy = new YamlConfiguration();
    List<Object> level0 = new ArrayList<>();
    level0.add("Level 0");
    level0.add("BLUE");
    level0.add("SOLID");
    level0.add(100);
    level0.add("stone");
    level0.add("stone");
    level0.add("dirt");

    List<Object> level1 = new ArrayList<>();
    level1.add("Level 1");
    level1.add("GREEN");
    level1.add("SOLID");
    level1.add(100);
    level1.add("stone");
    level1.add("grass");

    legacy.set("0", level0);
    legacy.set("1", level1);
    legacy.set("MaxLevel", "Level: MAX");
    legacy.save(blocksFile);

    // Migrate with PER_LEVEL mode
    LegacyBlocksMigrator.migrateBlocks(blocksFile, chestsFile, Settings.MigrationMode.PER_LEVEL);

    // Verify output
    YamlConfiguration migrated = YamlConfiguration.loadConfiguration(blocksFile);
    List<?> migratedLevel0 = migrated.getList("0");
    List<?> migratedLevel1 = migrated.getList("1");

    // Level 0 should have only: stone (weight 2), dirt (weight 1)
    assertThat(migratedLevel0).hasSize(6); // header (4) + 2 entries
    Map<String, Object> stone0 = (Map<String, Object>) migratedLevel0.get(4);
    Map<String, Object> dirt = (Map<String, Object>) migratedLevel0.get(5);
    assertThat(stone0.get("block")).isEqualTo("stone");
    assertThat(stone0.get("weight")).isEqualTo(2);
    assertThat(dirt.get("block")).isEqualTo("dirt");
    assertThat(dirt.get("weight")).isEqualTo(1);

    // Level 1 should have only: stone (weight 1), grass (weight 1)
    assertThat(migratedLevel1).hasSize(6); // header (4) + 2 entries
    Map<String, Object> stone1 = (Map<String, Object>) migratedLevel1.get(4);
    Map<String, Object> grass = (Map<String, Object>) migratedLevel1.get(5);
    assertThat(stone1.get("block")).isEqualTo("stone");
    assertThat(stone1.get("weight")).isEqualTo(1);
    assertThat(grass.get("block")).isEqualTo("grass");
    assertThat(grass.get("weight")).isEqualTo(1);
  }

  @Test
  @DisplayName("CUMULATIVE migration: level 1 contains union of level 0 + level 1")
  void cumulativeMigration_level1ContainsUnionOfBothLevels() throws Exception {
    // Create legacy blocks.yml with 2 levels with overlapping entries
    YamlConfiguration legacy = new YamlConfiguration();
    List<Object> level0 = new ArrayList<>();
    level0.add("Level 0");
    level0.add("BLUE");
    level0.add("SOLID");
    level0.add(100);
    level0.add("stone");
    level0.add("stone");
    level0.add("dirt");

    List<Object> level1 = new ArrayList<>();
    level1.add("Level 1");
    level1.add("GREEN");
    level1.add("SOLID");
    level1.add(100);
    level1.add("stone");
    level1.add("grass");

    legacy.set("0", level0);
    legacy.set("1", level1);
    legacy.set("MaxLevel", "Level: MAX");
    legacy.save(blocksFile);

    // Migrate with CUMULATIVE mode
    LegacyBlocksMigrator.migrateBlocks(blocksFile, chestsFile, Settings.MigrationMode.CUMULATIVE);

    // Verify output
    YamlConfiguration migrated = YamlConfiguration.loadConfiguration(blocksFile);
    List<?> migratedLevel0 = migrated.getList("0");
    List<?> migratedLevel1 = migrated.getList("1");

    // Level 0 should have: stone (weight 2), dirt (weight 1)
    assertThat(migratedLevel0).hasSize(6); // header (4) + 2 entries
    Map<String, Object> stone0 = (Map<String, Object>) migratedLevel0.get(4);
    Map<String, Object> dirt = (Map<String, Object>) migratedLevel0.get(5);
    assertThat(stone0.get("block")).isEqualTo("stone");
    assertThat(stone0.get("weight")).isEqualTo(2);
    assertThat(dirt.get("block")).isEqualTo("dirt");
    assertThat(dirt.get("weight")).isEqualTo(1);

    // Level 1 should have union: stone (weight 3), dirt (weight 1), grass (weight 1)
    assertThat(migratedLevel1).hasSize(7); // header (4) + 3 entries
    Map<String, Object> stone1 = (Map<String, Object>) migratedLevel1.get(4);
    Map<String, Object> dirt1 = (Map<String, Object>) migratedLevel1.get(5);
    Map<String, Object> grass = (Map<String, Object>) migratedLevel1.get(6);
    assertThat(stone1.get("block")).isEqualTo("stone");
    assertThat(stone1.get("weight")).isEqualTo(3); // 2 from level 0 + 1 from level 1
    assertThat(dirt1.get("block")).isEqualTo("dirt");
    assertThat(dirt1.get("weight")).isEqualTo(1);
    assertThat(grass.get("block")).isEqualTo("grass");
    assertThat(grass.get("weight")).isEqualTo(1);
  }

  @Test
  @DisplayName("PER_LEVEL migration: MaxLevel does not accumulate entries from previous levels")
  void perLevelMigration_maxLevelDoesNotAccumulateEntries() throws Exception {
    YamlConfiguration legacy = new YamlConfiguration();
    List<Object> level0 = new ArrayList<>();
    level0.add("Level 0");
    level0.add("BLUE");
    level0.add("SOLID");
    level0.add(100);
    level0.add("stone");

    List<Object> level1 = new ArrayList<>();
    level1.add("Level 1");
    level1.add("GREEN");
    level1.add("SOLID");
    level1.add(100);
    level1.add("dirt");

    legacy.set("0", level0);
    legacy.set("1", level1);
    legacy.set("MaxLevel", "Level: MAX");
    legacy.save(blocksFile);

    // Migrate with PER_LEVEL mode
    LegacyBlocksMigrator.migrateBlocks(blocksFile, chestsFile, Settings.MigrationMode.PER_LEVEL);

    // Verify MaxLevel has only name, no accumulated entries
    YamlConfiguration migrated = YamlConfiguration.loadConfiguration(blocksFile);
    List<?> maxLevel = migrated.getList("MaxLevel");
    assertThat(maxLevel).hasSize(1); // Only the name
    assertThat(maxLevel.get(0)).isEqualTo("Level: MAX");
  }

  @Test
  @DisplayName("CUMULATIVE migration: MaxLevel accumulates all entries")
  void cumulativeMigration_maxLevelAccumulatesAllEntries() throws Exception {
    YamlConfiguration legacy = new YamlConfiguration();
    List<Object> level0 = new ArrayList<>();
    level0.add("Level 0");
    level0.add("BLUE");
    level0.add("SOLID");
    level0.add(100);
    level0.add("stone");

    List<Object> level1 = new ArrayList<>();
    level1.add("Level 1");
    level1.add("GREEN");
    level1.add("SOLID");
    level1.add(100);
    level1.add("dirt");

    legacy.set("0", level0);
    legacy.set("1", level1);
    legacy.set("MaxLevel", "Level: MAX");
    legacy.save(blocksFile);

    // Migrate with CUMULATIVE mode
    LegacyBlocksMigrator.migrateBlocks(blocksFile, chestsFile, Settings.MigrationMode.CUMULATIVE);

    // Verify MaxLevel has name + accumulated entries
    YamlConfiguration migrated = YamlConfiguration.loadConfiguration(blocksFile);
    List<?> maxLevel = migrated.getList("MaxLevel");
    assertThat(maxLevel).hasSize(3); // name + 2 entries
    assertThat(maxLevel.get(0)).isEqualTo("Level: MAX");
    Map<String, Object> stone = (Map<String, Object>) maxLevel.get(1);
    Map<String, Object> dirt = (Map<String, Object>) maxLevel.get(2);
    assertThat(stone.get("block")).isEqualTo("stone");
    assertThat(dirt.get("block")).isEqualTo("dirt");
  }

  @Test
  @DisplayName("PER_LEVEL migration: no cross-level leakage with overlapping entries")
  void perLevelMigration_noCrossLevelLeakageWithOverlappingEntries() throws Exception {
    YamlConfiguration legacy = new YamlConfiguration();
    List<Object> level0 = new ArrayList<>();
    level0.add("Level 0");
    level0.add("BLUE");
    level0.add("SOLID");
    level0.add(100);
    level0.add("stone");
    level0.add("stone");
    level0.add("stone");

    List<Object> level1 = new ArrayList<>();
    level1.add("Level 1");
    level1.add("GREEN");
    level1.add("SOLID");
    level1.add(100);
    level1.add("stone");
    level1.add("dirt");

    legacy.set("0", level0);
    legacy.set("1", level1);
    legacy.set("MaxLevel", "Level: MAX");
    legacy.save(blocksFile);

    // Migrate with PER_LEVEL mode
    LegacyBlocksMigrator.migrateBlocks(blocksFile, chestsFile, Settings.MigrationMode.PER_LEVEL);

    // Verify no cross-level leakage: level 0 has stone (weight 3), level 1 has stone (weight 1) + dirt (weight 1)
    YamlConfiguration migrated = YamlConfiguration.loadConfiguration(blocksFile);
    List<?> migratedLevel0 = migrated.getList("0");
    List<?> migratedLevel1 = migrated.getList("1");

    assertThat(migratedLevel0).hasSize(5); // header (4) + 1 entry
    Map<String, Object> stone0 = (Map<String, Object>) migratedLevel0.get(4);
    assertThat(stone0.get("block")).isEqualTo("stone");
    assertThat(stone0.get("weight")).isEqualTo(3);

    assertThat(migratedLevel1).hasSize(6); // header (4) + 2 entries (stone + dirt)
    Map<String, Object> stone1 = (Map<String, Object>) migratedLevel1.get(4);
    Map<String, Object> dirt1 = (Map<String, Object>) migratedLevel1.get(5);
    assertThat(stone1.get("block")).isEqualTo("stone");
    assertThat(stone1.get("weight")).isEqualTo(1);
    assertThat(dirt1.get("block")).isEqualTo("dirt");
    assertThat(dirt1.get("weight")).isEqualTo(1);
  }
}
