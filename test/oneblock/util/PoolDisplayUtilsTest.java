package oneblock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import oneblock.Level;
import oneblock.PoolEntry;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PoolDisplayUtils} collapsing functionality. */
@DisplayName("PoolDisplayUtils tests")
class PoolDisplayUtilsTest {

  private Level level;

  @BeforeEach
  void setUp() {
    level = new Level("Test Level");
  }

  @Test
  @DisplayName("getCollapsedBlocks with duplicate entries collapses weights")
  void getCollapsedBlocks_withDuplicates_collapsesWeights() {
    PoolEntry stone1 = PoolEntry.block("stone");
    PoolEntry stone2 = PoolEntry.block("stone");
    PoolEntry dirt = PoolEntry.block("dirt");

    level.blockPool.add(stone1, 5);
    level.blockPool.add(stone2, 10);
    level.blockPool.add(dirt, 3);

    Map<PoolEntry, Integer> collapsed = PoolDisplayUtils.getCollapsedBlocks(level);

    assertThat(collapsed).hasSize(2);
    assertThat(collapsed.get(stone1)).isEqualTo(15); // 5 + 10
    assertThat(collapsed.get(dirt)).isEqualTo(3);
  }

  @Test
  @DisplayName(
      "getCollapsedBlocks with multiple distinct PoolEntry objects representing same block")
  void getCollapsedBlocks_withDistinctObjects_representingSameBlock() {
    // Create distinct PoolEntry objects with same kind and value
    PoolEntry stoneA = new PoolEntry(PoolEntry.Kind.BLOCK, "stone");
    PoolEntry stoneB = new PoolEntry(PoolEntry.Kind.BLOCK, "stone");

    level.blockPool.add(stoneA, 5);
    level.blockPool.add(stoneB, 10);

    Map<PoolEntry, Integer> collapsed = PoolDisplayUtils.getCollapsedBlocks(level);

    // Verify equals/hashCode works: distinct objects with same kind/value are treated as equal
    assertThat(collapsed).hasSize(1);
    assertThat(collapsed.get(stoneA)).isEqualTo(15); // Collapsed
    assertThat(collapsed.get(stoneB)).isEqualTo(15); // Same key
  }

  @Test
  @DisplayName("getCollapsedBlocks with empty pool returns empty map")
  void getCollapsedBlocks_withEmptyPool_returnsEmptyMap() {
    Map<PoolEntry, Integer> collapsed = PoolDisplayUtils.getCollapsedBlocks(level);
    assertThat(collapsed).isEmpty();
  }

  @Test
  @DisplayName("getCollapsedBlocks with single entry returns map with one entry")
  void getCollapsedBlocks_withSingleEntry_returnsMapWithOneEntry() {
    PoolEntry stone = PoolEntry.block("stone");
    level.blockPool.add(stone, 5);

    Map<PoolEntry, Integer> collapsed = PoolDisplayUtils.getCollapsedBlocks(level);

    assertThat(collapsed).hasSize(1);
    assertThat(collapsed.get(stone)).isEqualTo(5);
  }

  @Test
  @DisplayName("getCollapsedMobs with duplicate entries collapses weights")
  void getCollapsedMobs_withDuplicates_collapsesWeights() {
    level.mobPool.add(EntityType.ZOMBIE, 5);
    level.mobPool.add(EntityType.ZOMBIE, 10);
    level.mobPool.add(EntityType.SKELETON, 3);

    Map<EntityType, Integer> collapsed = PoolDisplayUtils.getCollapsedMobs(level);

    assertThat(collapsed).hasSize(2);
    assertThat(collapsed.get(EntityType.ZOMBIE)).isEqualTo(15); // 5 + 10
    assertThat(collapsed.get(EntityType.SKELETON)).isEqualTo(3);
  }

  @Test
  @DisplayName("getCollapsedMobs with empty pool returns empty map")
  void getCollapsedMobs_withEmptyPool_returnsEmptyMap() {
    Map<EntityType, Integer> collapsed = PoolDisplayUtils.getCollapsedMobs(level);
    assertThat(collapsed).isEmpty();
  }

  @Test
  @DisplayName("getCollapsedMobs with single entry returns map with one entry")
  void getCollapsedMobs_withSingleEntry_returnsMapWithOneEntry() {
    level.mobPool.add(EntityType.ZOMBIE, 5);

    Map<EntityType, Integer> collapsed = PoolDisplayUtils.getCollapsedMobs(level);

    assertThat(collapsed).hasSize(1);
    assertThat(collapsed.get(EntityType.ZOMBIE)).isEqualTo(5);
  }
}
