package oneblock.util;

import java.util.LinkedHashMap;
import java.util.Map;
import oneblock.Level;
import oneblock.PoolEntry;
import oneblock.WeightedPool;
import org.bukkit.entity.EntityType;

/**
 * Utility methods for displaying weighted pool contents in collapsed format. Collapses duplicate
 * entries by summing their weights, providing a cleaner summary view for admin commands and
 * debugging.
 */
public final class PoolDisplayUtils {

  private PoolDisplayUtils() {
    /* utility class */
  }

  /**
   * Returns a map of PoolEntry to summed weight for the given level's block pool. Duplicate
   * PoolEntry objects (same kind and value) are collapsed into a single entry with the sum of their
   * weights.
   *
   * @param level the level whose block pool to collapse
   * @return map of PoolEntry to summed weight, preserving insertion order
   */
  public static Map<PoolEntry, Integer> getCollapsedBlocks(Level level) {
    Map<PoolEntry, Integer> map = new LinkedHashMap<>();
    for (WeightedPool.Entry<PoolEntry> e : level.blockPool.entries())
      map.merge(e.value, e.weight, Integer::sum);
    return map;
  }

  /**
   * Returns a map of EntityType to summed weight for the given level's mob pool. Duplicate
   * EntityType entries are collapsed into a single entry with the sum of their weights.
   *
   * @param level the level whose mob pool to collapse
   * @return map of EntityType to summed weight, preserving insertion order
   */
  public static Map<EntityType, Integer> getCollapsedMobs(Level level) {
    Map<EntityType, Integer> map = new LinkedHashMap<>();
    for (WeightedPool.Entry<EntityType> e : level.mobPool.entries())
      map.merge(e.value, e.weight, Integer::sum);
    return map;
  }
}
