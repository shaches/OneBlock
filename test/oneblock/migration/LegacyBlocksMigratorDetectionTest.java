package oneblock.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic coverage of the legacy-format detection predicates on
 * {@link LegacyBlocksMigrator}. These do not call {@code Bukkit.getLogger()}
 * and therefore run without a Bukkit server.
 */
class LegacyBlocksMigratorDetectionTest {

    // ---------- isLegacyBlocks ----------

    @Test
    @DisplayName("isLegacyBlocks: scalar MaxLevel alone -> not legacy (nothing to migrate)")
    void scalarMaxLevelAloneIsNotLegacy() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("MaxLevel", "Level: MAX");
        assertThat(LegacyBlocksMigrator.isLegacyBlocks(cfg)).isFalse();
    }

    @Test
    @DisplayName("isLegacyBlocks: scalar MaxLevel with decorated map entries -> modern")
    void scalarMaxLevelWithMapEntriesIsModern() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("MaxLevel", "Level: MAX");

        List<Object> level0 = new ArrayList<>();
        level0.add("Level 0");
        level0.add("GRASS_BLOCK");
        Map<String, Object> decorated = new LinkedHashMap<>();
        decorated.put("decorated", "GRASS_BLOCK");
        decorated.put("chance", 3);
        level0.add(decorated);
        cfg.set("0", level0);

        assertThat(LegacyBlocksMigrator.isLegacyBlocks(cfg)).isFalse();
    }

    @Test
    @DisplayName("isLegacyBlocks: any pool entry that's a raw String -> legacy")
    void rawStringPoolEntryIsLegacy() {
        YamlConfiguration cfg = new YamlConfiguration();
        // level 0: [name, "STONE", "DIRT"]  (two raw string entries post-header)
        List<Object> level0 = new ArrayList<>();
        level0.add("Level 0");
        level0.add("STONE");
        level0.add("DIRT");
        cfg.set("0", level0);

        List<Object> maxList = new ArrayList<>();
        maxList.add("Level: MAX");
        cfg.set("MaxLevel", maxList);

        assertThat(LegacyBlocksMigrator.isLegacyBlocks(cfg)).isTrue();
    }

    @Test
    @DisplayName("isLegacyBlocks: all entries are Maps -> modern format")
    void mapEntriesIsModern() {
        YamlConfiguration cfg = new YamlConfiguration();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("block", "STONE");
        entry.put("weight", 3);

        List<Object> level0 = new ArrayList<>();
        level0.add("Level 0");
        level0.add(entry);
        cfg.set("0", level0);

        List<Object> maxList = new ArrayList<>();
        maxList.add("Level: MAX");
        maxList.add(entry);
        cfg.set("MaxLevel", maxList);

        assertThat(LegacyBlocksMigrator.isLegacyBlocks(cfg)).isFalse();
    }

    @Test
    @DisplayName("isLegacyBlocks: empty config -> not legacy")
    void emptyConfigIsNotLegacy() {
        YamlConfiguration cfg = new YamlConfiguration();
        assertThat(LegacyBlocksMigrator.isLegacyBlocks(cfg)).isFalse();
    }

    // ---------- isLegacyChests ----------

    @Test
    @DisplayName("isLegacyChests: any list value -> legacy")
    void listValueIsLegacy() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> items = new ArrayList<>();
        items.add("DIAMOND");
        items.add("IRON_INGOT");
        cfg.set("small_chest", items);
        assertThat(LegacyBlocksMigrator.isLegacyChests(cfg)).isTrue();
    }

    @Test
    @DisplayName("isLegacyChests: all values are strings -> modern alias map")
    void stringValuesIsModern() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("small_chest",  "minecraft:chests/simple_dungeon");
        cfg.set("medium_chest", "minecraft:chests/abandoned_mineshaft");
        assertThat(LegacyBlocksMigrator.isLegacyChests(cfg)).isFalse();
    }

    @Test
    @DisplayName("isLegacyChests: empty -> not legacy")
    void emptyChestsConfigIsNotLegacy() {
        assertThat(LegacyBlocksMigrator.isLegacyChests(new YamlConfiguration())).isFalse();
    }
}
