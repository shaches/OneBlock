package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Unit coverage for the {@link ConfigManager} blocks.yml parser:
 * {@link ConfigManager#parseLevelFromList(List, Level, int)} and the
 * {@link ConfigManager#parsePoolEntry(Object, Level)} entry resolver
 * it delegates to. Phase 5.1 drove these out of the {@code 1%-covered}
 * post-Phase-4 gap; without these tests every Phase 6 rename of the
 * YAML parse helpers is a blind change.
 *
 * <p>The parser is exercised directly (the helpers are package-private
 * after Phase 5.1) so we don't need an on-disk {@code blocks.yml} or a
 * live Bukkit server. {@link Oneblock#plugin} is replaced with a
 * Mockito mock returning a mock {@link Logger} so warning-path
 * branches ({@code unknown mob}, {@code bad weight}, {@code unknown
 * kind}, {@code malformed loot table key}) can be exercised without
 * NPE-ing on {@code Oneblock.plugin.getLogger()}.
 *
 * <p><b>Bukkit-runtime constraint.</b> {@code org.bukkit.Material#matchMaterial}
 * goes through {@code org.bukkit.Registry}, whose {@code <clinit>} cannot
 * run outside a real server (NoClassDefFoundError). Every test in this
 * suite therefore stays on parser branches that <em>don't</em> reach
 * {@code resolveBlock} for a non-empty input - i.e. mob entries,
 * command entries (string starting with {@code '/'}), loot-table map
 * entries, and the null/empty short-circuit. Coverage of the vanilla
 * {@code Material} resolution path is deferred to a future slice with
 * a {@code MockedStatic<Material>} harness.
 */
class ConfigManagerParseLevelTest {

    private static Oneblock savedPlugin;
    private static Logger pluginLogger;

    @BeforeAll
    static void installMockPlugin() {
        savedPlugin = Oneblock.plugin;
        pluginLogger = mock(Logger.class);
        Oneblock mockPlugin = mock(Oneblock.class);
        when(mockPlugin.getLogger()).thenReturn(pluginLogger);
        Oneblock.plugin = mockPlugin;
    }

    @AfterAll
    static void restorePlugin() {
        Oneblock.plugin = savedPlugin;
    }

    private ConfigManager cm;
    private Level level;

    @BeforeEach
    void freshFixture() {
        // Reset accumulated invocations so verify(...) checks don't trip on
        // warnings emitted by previous tests in this class.
        Mockito.reset(pluginLogger);
        cm = new ConfigManager();
        level = new Level("placeholder");
        // Reset Level.max baselines so duck-typed fall-throughs in the
        // colour / style probes have a deterministic anchor.
        Level.max.color = BarColor.GREEN;
        Level.max.style = BarStyle.SOLID;
    }

    // --------------------------------------------------------------
    // parseLevelFromList: header parsing
    // --------------------------------------------------------------

    @Test
    @DisplayName("null / empty list is a no-op (no name overwrite, no pool entries)")
    void nullAndEmptyAreNoOp() {
        level.name = "untouched";
        cm.parseLevelFromList(null, level, 0);
        cm.parseLevelFromList(Collections.emptyList(), level, 0);
        assertThat(level.name).isEqualTo("untouched");
        assertThat(level.blockPoolSize()).isZero();
        assertThat(level.mobPoolSize()).isZero();
    }

    @Test
    @DisplayName("first string is consumed as the level name and colour-code translated")
    void firstStringBecomesName() {
        cm.parseLevelFromList(Arrays.asList("&aShiny"), level, 0);
        // & -> section sign after Utils.translateColorCodes
        assertThat(level.name).contains("Shiny");
        assertThat(level.name.charAt(0)).isEqualTo('\u00a7');
    }

    @Test
    @DisplayName("name + valid colour + valid style consumes 3 header positions")
    void nameColorStyleHeader() {
        cm.parseLevelFromList(Arrays.asList("Forest", "BLUE", "SEGMENTED_10"), level, 0);
        assertThat(level.name).isEqualTo("Forest");
        assertThat(level.color).isEqualTo(BarColor.BLUE);
        assertThat(level.style).isEqualTo(BarStyle.SEGMENTED_10);
        assertThat(level.blockPoolSize()).isZero();
    }

    @Test
    @DisplayName("invalid colour token falls back to Level.max.color and is consumed downstream")
    void invalidColorFallsBackToMaxAndFlowsThrough() {
        Level.max.color = BarColor.PURPLE;
        // "/cmd" fails BarColor.valueOf -> level.color = max.color, q stays.
        // Style probe sees "/cmd", fails BarStyle.valueOf -> level.style =
        // max.style, q stays. Length probe sees "/cmd", not a Number,
        // Integer.parseInt fails -> length = 16 + idx*multiplier, q stays.
        // Pool-entry loop consumes "/cmd" as a command (starts with '/')
        // bypassing resolveBlock entirely.
        cm.parseLevelFromList(Arrays.asList("Mystery", "/cmd"), level, 3);
        assertThat(level.color).isEqualTo(BarColor.PURPLE);
        assertThat(level.length).isEqualTo(16 + 3 * Level.multiplier);
        assertThat(level.blockPoolSize()).isEqualTo(1);
        PoolEntry entry = level.blockPool.entries().get(0).value;
        assertThat(entry.kind).isEqualTo(PoolEntry.Kind.COMMAND);
        assertThat(entry.value).isEqualTo("/cmd");
    }

    @Test
    @DisplayName("integer length is parsed and Math.max-1 clamped")
    void integerLengthClamped() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 42), level, 0);
        assertThat(level.length).isEqualTo(42);
    }

    @Test
    @DisplayName("zero / negative integer length is clamped to 1")
    void zeroOrNegativeLengthClampedToOne() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 0), level, 0);
        assertThat(level.length).isEqualTo(1);
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", -7), level, 0);
        assertThat(level.length).isEqualTo(1);
    }

    @Test
    @DisplayName("string-numeric length is parseInt'd and consumes the position")
    void stringNumericLengthParsed() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", "75"), level, 0);
        assertThat(level.length).isEqualTo(75);
    }

    @Test
    @DisplayName("non-numeric length string falls through to pool-entry loop with idx-default length")
    void nonNumericLengthFallsThroughToPoolEntries() {
        // After consuming "Lvl", "RED", "SOLID", the next string "/help" is
        // the length probe target. Integer.parseInt fails -> length =
        // 16 + idx*mult, q is NOT advanced (the catch only sets the field).
        // The pool-entry loop then picks up "/help" as a command entry.
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", "/help"), level, 2);
        assertThat(level.length).isEqualTo(16 + 2 * Level.multiplier);
        assertThat(level.blockPoolSize()).isEqualTo(1);
        PoolEntry entry = level.blockPool.entries().get(0).value;
        assertThat(entry.kind).isEqualTo(PoolEntry.Kind.COMMAND);
        assertThat(entry.value).isEqualTo("/help");
    }

    // --------------------------------------------------------------
    // parsePoolEntry / parseLevelFromList: pool entries
    // --------------------------------------------------------------

    @Test
    @DisplayName("plain string starting with '/' is a command entry")
    void plainStringWithSlashIsCommand() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, "/give @p diamond"), level, 0);
        assertThat(level.blockPoolSize()).isEqualTo(1);
        PoolEntry entry = level.blockPool.entries().get(0).value;
        assertThat(entry.kind).isEqualTo(PoolEntry.Kind.COMMAND);
        assertThat(entry.value).isEqualTo("/give @p diamond");
    }

    @Test
    @DisplayName("plain string matching an EntityType is a mob entry, uppercased")
    void plainStringEntityTypeIsMob() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, "zombie"), level, 0);
        assertThat(level.blockPoolSize()).isZero();
        assertThat(level.mobPoolSize()).isEqualTo(1);
        WeightedPool.Entry<EntityType> mob = level.mobPool.entries().get(0);
        assertThat(mob.value).isEqualTo(EntityType.ZOMBIE);
        assertThat(mob.weight).isEqualTo(1);
    }

    @Test
    @DisplayName("map entry with mob + integer weight produces weighted mobPool entry")
    void mapEntryMobWithIntWeight() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("mob", "creeper");
        entry.put("weight", 7);
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.mobPoolSize()).isEqualTo(1);
        WeightedPool.Entry<EntityType> mob = level.mobPool.entries().get(0);
        assertThat(mob.value).isEqualTo(EntityType.CREEPER);
        assertThat(mob.weight).isEqualTo(7);
    }

    @Test
    @DisplayName("map entry with mob + numeric-string weight is parsed")
    void mapEntryMobWithStringWeight() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("mob", "skeleton");
        entry.put("weight", "3");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        WeightedPool.Entry<EntityType> mob = level.mobPool.entries().get(0);
        assertThat(mob.value).isEqualTo(EntityType.SKELETON);
        assertThat(mob.weight).isEqualTo(3);
    }

    @Test
    @DisplayName("map entry with loot_table produces a LOOT_TABLE PoolEntry")
    void mapEntryLootTable() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("loot_table", "minecraft:chests/simple_dungeon");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.blockPoolSize()).isEqualTo(1);
        PoolEntry pool = level.blockPool.entries().get(0).value;
        assertThat(pool.kind).isEqualTo(PoolEntry.Kind.LOOT_TABLE);
        assertThat(pool.value).hasToString("minecraft:chests/simple_dungeon");
    }

    @Test
    @DisplayName("map entry with command produces a COMMAND PoolEntry")
    void mapEntryCommand() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("command", "/say hi");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.blockPoolSize()).isEqualTo(1);
        PoolEntry pool = level.blockPool.entries().get(0).value;
        assertThat(pool.kind).isEqualTo(PoolEntry.Kind.COMMAND);
        assertThat(pool.value).isEqualTo("/say hi");
    }

    @Test
    @DisplayName("map weight is clamped to >= 1 (negative -> 1)")
    void mapWeightClampedToOne() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("mob", "zombie");
        entry.put("weight", -5);
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.mobPool.entries().get(0).weight).isEqualTo(1);
    }

    // --------------------------------------------------------------
    // Warning paths: must not throw, must log, must skip the entry
    // --------------------------------------------------------------

    @Test
    @DisplayName("map entry with no recognized kind is skipped with a warning")
    void mapWithoutKindIsSkippedWithWarning() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("flowers", "yes");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.blockPoolSize()).isZero();
        assertThat(level.mobPoolSize()).isZero();
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger).warning(msg.capture());
        assertThat(msg.getValue()).contains("no recognized kind");
    }

    @Test
    @DisplayName("non-numeric weight string logs a warning and defaults to 1")
    void nonNumericWeightLogsWarning() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("mob", "zombie");
        entry.put("weight", "many");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.mobPool.entries().get(0).weight).isEqualTo(1);
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger).warning(msg.capture());
        assertThat(msg.getValue()).contains("non-numeric weight");
    }

    @Test
    @DisplayName("unknown mob name is skipped with a warning, neither pool grows")
    void unknownMobIsSkippedWithWarning() {
        Map<String, Object> entry = new HashMap<>();
        entry.put("mob", "ZOMBIE_DRAGON_OF_DOOM");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.mobPoolSize()).isZero();
        assertThat(level.blockPoolSize()).isZero();
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger).warning(msg.capture());
        assertThat(msg.getValue()).contains("unknown mob");
    }

    @Test
    @DisplayName("malformed loot_table key is skipped with a warning")
    void malformedLootTableSkippedWithWarning() {
        Map<String, Object> entry = new HashMap<>();
        // NamespacedKey forbids spaces in the path -> ChestItems.parseKey returns null.
        entry.put("loot_table", "bad key with spaces");
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, entry), level, 0);
        assertThat(level.blockPoolSize()).isZero();
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger).warning(msg.capture());
        assertThat(msg.getValue()).contains("invalid loot table key");
    }

    @Test
    @DisplayName("null and empty-string entries are silently skipped")
    void nullAndEmptyStringEntriesSkipped() {
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, null, ""), level, 0);
        assertThat(level.blockPoolSize()).isZero();
        assertThat(level.mobPoolSize()).isZero();
    }

    @Test
    @DisplayName("non-Map non-String entry (e.g. boxed Integer beyond the length slot) is silently skipped")
    void nonMapNonStringEntrySkipped() {
        // The length slot consumes the first numeric, so the SECOND numeric
        // is what we want to land in the pool-entry loop. After consuming
        // header (Lvl, RED, SOLID, 10), the loop sees Integer.valueOf(99)
        // which is neither Map nor String -> the parsePoolEntry early-return
        // skips it without throwing.
        cm.parseLevelFromList(Arrays.asList("Lvl", "RED", "SOLID", 10, Integer.valueOf(99)), level, 0);
        assertThat(level.blockPoolSize()).isZero();
    }

    // --------------------------------------------------------------
    // resolveBlock: null / empty fallback (other paths require Material
    // and are deferred to a MockedStatic-based slice).
    // --------------------------------------------------------------

    @Test
    @DisplayName("resolveBlock: null / empty input returns the default-grass sentinel")
    void resolveBlockNullEmptyReturnsGrass() {
        assertThat(cm.resolveBlock(null)).isSameAs(PoolEntry.GRASS);
        assertThat(cm.resolveBlock("")).isSameAs(PoolEntry.GRASS);
    }
}
