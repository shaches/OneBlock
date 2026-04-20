package oneblock.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reflection-based coverage of the private helpers that drive
 * {@link LegacyBlocksMigrator#migrateBlocks}:
 *
 * <ul>
 *   <li>{@code classify(text, chestMap)} &mdash; turns a raw legacy pool
 *       string into a {@code "kind|value"} accumulator key.</li>
 *   <li>{@code detectHeaderEnd(list)} &mdash; finds where the positional
 *       header (name / color / style / length) ends and pool entries begin.</li>
 *   <li>{@code buildEntryMap(accKey, weight)} &mdash; materializes a
 *       weighted-schema map from the accumulator key and count.</li>
 * </ul>
 *
 * These are pure logic and deliberately avoid {@code Bukkit.getLogger()}, so
 * they run without a Bukkit server context.
 */
class LegacyBlocksMigratorFlattenTest {

    private static Method CLASSIFY;
    private static Method DETECT_HEADER_END;
    private static Method BUILD_ENTRY_MAP;

    @BeforeAll
    static void reflectPrivates() throws Exception {
        CLASSIFY = LegacyBlocksMigrator.class.getDeclaredMethod(
                "classify", String.class, Map.class);
        CLASSIFY.setAccessible(true);

        DETECT_HEADER_END = LegacyBlocksMigrator.class.getDeclaredMethod(
                "detectHeaderEnd", List.class);
        DETECT_HEADER_END.setAccessible(true);

        BUILD_ENTRY_MAP = LegacyBlocksMigrator.class.getDeclaredMethod(
                "buildEntryMap", String.class, int.class);
        BUILD_ENTRY_MAP.setAccessible(true);
    }

    // ---------- classify ----------

    @Test
    @DisplayName("classify: null / empty -> null")
    void classifyNullEmpty() throws Exception {
        assertThat(classify(null, Collections.emptyMap())).isNull();
        assertThat(classify("", Collections.emptyMap())).isNull();
    }

    @Test
    @DisplayName("classify: leading '/' -> command|<verbatim>")
    void classifyCommand() throws Exception {
        assertThat(classify("/give %s 1 stone", Collections.emptyMap()))
                .isEqualTo("command|/give %s 1 stone");
    }

    @Test
    @DisplayName("classify: chest alias (exact case) -> loot_table|<key>")
    void classifyChestAliasExact() throws Exception {
        Map<String, String> chests = new LinkedHashMap<>();
        chests.put("small_chest", "minecraft:chests/simple_dungeon");
        assertThat(classify("small_chest", chests))
                .isEqualTo("loot_table|minecraft:chests/simple_dungeon");
    }

    @Test
    @DisplayName("classify: chest alias (different case) -> loot_table|<key>")
    void classifyChestAliasCaseFallback() throws Exception {
        // The migrator registers both original-case and lowercase keys in
        // the chest map. classify() looks up case-sensitive first then
        // lowercase — mimic the same double-keying here.
        Map<String, String> chests = new LinkedHashMap<>();
        chests.put("Small_Chest", "minecraft:chests/simple_dungeon");
        chests.put("small_chest", "minecraft:chests/simple_dungeon");
        assertThat(classify("SMALL_CHEST", chests))
                .isEqualTo("loot_table|minecraft:chests/simple_dungeon");
    }

    @Test
    @DisplayName("classify: valid EntityType -> mob|<UPPERCASE>")
    void classifyMob() throws Exception {
        assertThat(classify("zombie", Collections.emptyMap())).isEqualTo("mob|ZOMBIE");
        assertThat(classify("CREEPER", Collections.emptyMap())).isEqualTo("mob|CREEPER");
    }

    @Test
    @DisplayName("classify: unknown text -> block|<verbatim>")
    void classifyBlockFallback() throws Exception {
        // Not a command, not a chest alias, not an EntityType -> block kind.
        assertThat(classify("STONE", Collections.emptyMap())).isEqualTo("block|STONE");
        assertThat(classify("some_weird_id", Collections.emptyMap()))
                .isEqualTo("block|some_weird_id");
    }

    // ---------- detectHeaderEnd ----------

    @Test
    @DisplayName("detectHeaderEnd: empty list -> 0")
    void detectHeaderEmpty() throws Exception {
        assertThat(detectHeaderEnd(Collections.emptyList())).isEqualTo(0);
    }

    @Test
    @DisplayName("detectHeaderEnd: name only -> 1")
    void detectHeaderNameOnly() throws Exception {
        assertThat(detectHeaderEnd(List.of("Level 0"))).isEqualTo(1);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + invalid color -> 1 (probe fails, name alone)")
    void detectHeaderInvalidColor() throws Exception {
        // "STONE" is not a BarColor; probe fails, q stays at 1. "STONE" is
        // then parsed as the first pool entry by the caller.
        assertThat(detectHeaderEnd(List.of("Level 0", "STONE"))).isEqualTo(1);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + valid color -> 2")
    void detectHeaderColor() throws Exception {
        assertThat(detectHeaderEnd(List.of("Level 0", "GREEN"))).isEqualTo(2);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + color + style -> 3")
    void detectHeaderColorStyle() throws Exception {
        assertThat(detectHeaderEnd(List.of("Level 0", "GREEN", "SOLID"))).isEqualTo(3);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + color + style + numeric length -> 4")
    void detectHeaderFullNumeric() throws Exception {
        assertThat(detectHeaderEnd(List.of("Level 0", "GREEN", "SOLID", 100))).isEqualTo(4);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + color + style + string-form length -> 4")
    void detectHeaderFullStringLength() throws Exception {
        assertThat(detectHeaderEnd(List.of("Level 0", "GREEN", "SOLID", "100"))).isEqualTo(4);
    }

    @Test
    @DisplayName("detectHeaderEnd: name + color + style + non-number string -> 3 (probe fails)")
    void detectHeaderLengthProbeFails() throws Exception {
        // "STONE" at position 3 is not a number; the length-probe fails,
        // q stays at 3, and "STONE" becomes the first pool entry.
        assertThat(detectHeaderEnd(List.of("Level 0", "GREEN", "SOLID", "STONE"))).isEqualTo(3);
    }

    // ---------- buildEntryMap ----------

    @Test
    @DisplayName("buildEntryMap: block kind materializes block + weight keys")
    void buildEntryMapBlock() throws Exception {
        Map<String, Object> m = buildEntryMap("block|STONE", 3);
        assertThat(m).hasSize(2).containsEntry("block", "STONE").containsEntry("weight", 3);
    }

    @Test
    @DisplayName("buildEntryMap: mob kind materializes mob + weight keys")
    void buildEntryMapMob() throws Exception {
        Map<String, Object> m = buildEntryMap("mob|ZOMBIE", 1);
        assertThat(m).hasSize(2).containsEntry("mob", "ZOMBIE").containsEntry("weight", 1);
    }

    @Test
    @DisplayName("buildEntryMap: loot_table kind preserves the full loot-table string (colon-in-value)")
    void buildEntryMapLootTable() throws Exception {
        Map<String, Object> m = buildEntryMap("loot_table|minecraft:chests/simple_dungeon", 5);
        assertThat(m).hasSize(2)
                .containsEntry("loot_table", "minecraft:chests/simple_dungeon")
                .containsEntry("weight", 5);
    }

    @Test
    @DisplayName("buildEntryMap: command kind preserves the full command (even with '|' in the template)")
    void buildEntryMapCommand() throws Exception {
        // The classifier uses the FIRST '|' as the kind/value separator; the
        // value itself may contain further '|' characters that must survive.
        Map<String, Object> m = buildEntryMap("command|/give %s 1 | /say done", 2);
        assertThat(m).hasSize(2)
                .containsEntry("command", "/give %s 1 | /say done")
                .containsEntry("weight", 2);
    }

    // ---------- reflection helpers ----------

    private static String classify(String text, Map<String, String> chests) throws Exception {
        return (String) CLASSIFY.invoke(null, text, chests);
    }

    private static int detectHeaderEnd(List<?> raw) throws Exception {
        return (int) DETECT_HEADER_END.invoke(null, raw);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildEntryMap(String accKey, int weight) throws Exception {
        return (Map<String, Object>) BUILD_ENTRY_MAP.invoke(null, accKey, weight);
    }

    // Sanity guard: fail fast if a future rename of the private helper
    // silently breaks these reflection-based tests.
    @Test
    @DisplayName("sanity: reflected method names are present")
    void reflectedMethodsExist() {
        assertThat(CLASSIFY).isNotNull();
        assertThat(DETECT_HEADER_END).isNotNull();
        assertThat(BUILD_ENTRY_MAP).isNotNull();
    }

    // Instantiation helper so the class is not accidentally marked as holding
    // only static members (it is exercised at runtime only through reflection).
    @SuppressWarnings("unused")
    private static final List<Object> KEEP_ALIVE = new ArrayList<>();
}
