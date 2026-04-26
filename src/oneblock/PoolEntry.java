package oneblock;

<<<<<<< HEAD
=======
import org.bukkit.NamespacedKey;

>>>>>>> origin/main
/**
 * Tagged union representing one entry of a level's block-pool.
 * Mob entries are kept in a separate {@code WeightedPool<EntityType>} on {@link Level},
 * so {@link Kind#MOB} is intentionally absent here.
 */
<<<<<<< HEAD
public class PoolEntry {
	public enum Kind { BLOCK, CHEST, COMMAND, DEFAULT_GRASS }
=======
public final class PoolEntry {
	public enum Kind { BLOCK, LOOT_TABLE, COMMAND, DEFAULT_GRASS }
>>>>>>> origin/main

	public final Kind kind;
	public final Object value;

	public PoolEntry(Kind kind, Object value) {
		this.kind = kind;
		this.value = value;
	}

	public static final PoolEntry GRASS = new PoolEntry(Kind.DEFAULT_GRASS, null);

	public static PoolEntry block(Object material)       { return new PoolEntry(Kind.BLOCK, material); }
<<<<<<< HEAD
	public static PoolEntry chest(String chest_name) 	 { return new PoolEntry(Kind.CHEST, chest_name); }
=======
	public static PoolEntry lootTable(NamespacedKey key) { return new PoolEntry(Kind.LOOT_TABLE, key); }
>>>>>>> origin/main
	public static PoolEntry command(String cmd)          { return new PoolEntry(Kind.COMMAND, cmd); }

	@Override
	public String toString() {
		switch (kind) {
<<<<<<< HEAD
			case DEFAULT_GRASS: return "GRASS (undefined)";
			case BLOCK:         return value == null ? "null" : value.toString();
			case CHEST:    		return "chest: " + value;
=======
			case DEFAULT_GRASS: return "Grass (default)";
			case BLOCK:         return value == null ? "Grass (undefined)" : value.toString();
			case LOOT_TABLE:    return "loot_table: " + value;
>>>>>>> origin/main
			case COMMAND:       return "command: " + value;
			default:            return "?";
		}
	}
}
