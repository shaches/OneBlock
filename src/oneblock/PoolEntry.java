package oneblock;

import oneblock.utils.Compat;
import org.bukkit.NamespacedKey;

/**
 * Tagged union representing one entry of a level's block-pool. Mob entries are kept in a separate
 * {@code WeightedPool<EntityType>} on {@link Level}, so {@link Kind#MOB} is intentionally absent
 * here.
 */
public final class PoolEntry {
  public enum Kind {
    BLOCK,
    LOOT_TABLE,
    CHEST,
    COMMAND,
    DECORATED_BLOCK
  }

  public final Kind kind;
  public final Object value;

  public PoolEntry(Kind kind, Object value) {
    this.kind = kind;
    this.value = value;
  }

  public static final PoolEntry GRASS =
      decorated(new DecoratedBlock(Compat.GRASS_BLOCK, 3, 1, null));

  public static PoolEntry decorated(DecoratedBlock decorated) {
    return new PoolEntry(Kind.DECORATED_BLOCK, decorated);
  }

  public static PoolEntry block(Object material) {
    return new PoolEntry(Kind.BLOCK, material);
  }

  public static PoolEntry lootTable(NamespacedKey key) {
    return new PoolEntry(Kind.LOOT_TABLE, key);
  }

  public static PoolEntry chest(String alias) {
    return new PoolEntry(Kind.CHEST, alias);
  }

  public static PoolEntry command(String cmd) {
    return new PoolEntry(Kind.COMMAND, cmd);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PoolEntry)) return false;
    PoolEntry other = (PoolEntry) obj;
    return kind == other.kind && (value == null ? other.value == null : value.equals(other.value));
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(kind, value);
  }

  @Override
  public String toString() {
    switch (kind) {
      case DECORATED_BLOCK:
        if (value instanceof DecoratedBlock d) return d.base().toString();
        return value == null ? "Grass (default)" : value.toString();
      case BLOCK:
        return value == null ? "Grass (undefined)" : value.toString();
      case LOOT_TABLE:
        return "loot_table: " + value;
      case CHEST:
        return "chest: " + value;
      case COMMAND:
        return "command: " + value;
      default:
        return "?";
    }
  }
}
