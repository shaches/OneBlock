package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the static-state contract of {@link Level}:
 * lookup fallback to {@link Level#max}, pool-size reflection, multiplier
 * default, {@link Level#getId} fallback on a detached instance, and
 * {@link Level#resetPools} atomicity.
 *
 * <p>{@code Level} holds mutable statics ({@code levels}, {@code max},
 * {@code multiplier}); each test snapshots and restores them to avoid
 * leaking state to sibling tests in the suite.
 */
class LevelTest {

    private List<Level> savedLevels;
    private Level savedMax;
    private int savedMultiplier;

    @BeforeEach
    void snapshotAndReset() {
        savedLevels = new ArrayList<>(Level.levels);
        savedMax = Level.max;
        savedMultiplier = Level.multiplier;

        Level.levels.clear();
        Level.max = new Level("Level: MAX");
        Level.multiplier = 5;
    }

    @AfterEach
    void restore() {
        Level.levels.clear();
        Level.levels.addAll(savedLevels);
        Level.max = savedMax;
        Level.multiplier = savedMultiplier;
    }

    @Test
    @DisplayName("empty levels list: get(0) and get(N) both return max")
    void getOutOfRangeReturnsMax() {
        assertThat(Level.get(0)).isSameAs(Level.max);
        assertThat(Level.get(999)).isSameAs(Level.max);
    }

    @Test
    @DisplayName("populated levels list: get(i) returns levels.get(i); out-of-range returns max")
    void getInRangeReturnsEntry() {
        Level l0 = new Level("Zero");
        Level l1 = new Level("One");
        Level.levels.add(l0);
        Level.levels.add(l1);

        assertThat(Level.get(0)).isSameAs(l0);
        assertThat(Level.get(1)).isSameAs(l1);
        assertThat(Level.get(2)).isSameAs(Level.max);
    }

    @Test
    @DisplayName("size() reflects Level.levels.size()")
    void sizeReflectsList() {
        assertThat(Level.size()).isZero();
        Level.levels.add(new Level("foo"));
        assertThat(Level.size()).isEqualTo(1);
        Level.levels.add(new Level("bar"));
        assertThat(Level.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("max default: name is 'Level: MAX' after reset")
    void maxDefaultName() {
        // Reset in @BeforeEach re-news max, so this is deterministic.
        assertThat(Level.max.name).isEqualTo("Level: MAX");
    }

    @Test
    @DisplayName("multiplier default: 5")
    void multiplierDefault() {
        assertThat(Level.multiplier).isEqualTo(5);
    }

    @Test
    @DisplayName("constructor: name is set, length default is 100")
    void constructorSetsNameAndLength() {
        Level l = new Level("Some Level");
        assertThat(l.name).isEqualTo("Some Level");
        // Level.length has an explicit static initializer of 100; the
        // config loader overrides this per-level from blocks.yml. Guard
        // regression in the default value.
        assertThat(l.length).isEqualTo(100);
    }

    @Test
    @DisplayName("getId: detached instance returns 1 (hardcoded fallback)")
    void getIdFallback() {
        Level detached = new Level("Not In List");
        // The impl scans levels for `this` and returns 1 when not found.
        assertThat(detached.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getId: attached instance returns its index")
    void getIdMatchesIndex() {
        Level l0 = new Level("Zero");
        Level l1 = new Level("One");
        Level l2 = new Level("Two");
        Level.levels.add(l0);
        Level.levels.add(l1);
        Level.levels.add(l2);

        assertThat(l0.getId()).isEqualTo(0);
        assertThat(l1.getId()).isEqualTo(1);
        assertThat(l2.getId()).isEqualTo(2);
    }

    @Test
    @DisplayName("blockPoolSize / mobPoolSize reflect underlying WeightedPool.size()")
    void poolSizesDelegate() {
        Level l = new Level("x");
        assertThat(l.blockPoolSize()).isZero();
        assertThat(l.mobPoolSize()).isZero();

        l.blockPool.add(PoolEntry.GRASS, 1);
        l.mobPool.add(org.bukkit.entity.EntityType.ZOMBIE, 2);
        assertThat(l.blockPoolSize()).isEqualTo(1);
        assertThat(l.mobPoolSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("resetPools: replaces both pools with fresh empty instances")
    void resetPoolsEmptiesBoth() {
        Level l = new Level("x");
        l.blockPool.add(PoolEntry.block("STONE"), 1);
        l.mobPool.add(org.bukkit.entity.EntityType.ZOMBIE, 2);

        WeightedPool<PoolEntry> blockRef = l.blockPool;
        WeightedPool<org.bukkit.entity.EntityType> mobRef = l.mobPool;

        l.resetPools();

        // Reference swap (not in-place clear): sibling parsers that cache
        // the old pool keep operating on the pre-reset snapshot.
        assertThat(l.blockPool).isNotSameAs(blockRef);
        assertThat(l.mobPool).isNotSameAs(mobRef);
        assertThat(l.blockPoolSize()).isZero();
        assertThat(l.mobPoolSize()).isZero();
    }
}
