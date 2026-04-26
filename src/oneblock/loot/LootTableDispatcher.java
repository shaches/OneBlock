package oneblock.loot;

import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
<<<<<<< HEAD
import org.bukkit.block.BlockState;
=======
>>>>>>> origin/main
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
<<<<<<< HEAD
import org.bukkit.loot.LootTables;
=======
>>>>>>> origin/main

import oneblock.Oneblock;

/**
 * Places a vanilla chest at the given block and populates it with the contents
 * of a {@link LootTable} referenced by {@link NamespacedKey}. Guarded against
<<<<<<< HEAD
 */
public class LootTableDispatcher {
	private static final Logger LOG = Oneblock.plugin.getLogger();
	private static final NamespacedKey FALLBACK_KEY = LootTables.SIMPLE_DUNGEON.getKey();
=======
 * pre-1.9 servers where the LootTable API is unavailable.
 */
public class LootTableDispatcher {
	private static final Logger LOG = Bukkit.getLogger();
	private static final NamespacedKey FALLBACK_KEY = NamespacedKey.minecraft("chests/simple_dungeon");
	private static boolean legacyWarned = false;
>>>>>>> origin/main
	
	public static boolean populate(Block block, NamespacedKey key, Random rnd) {
		if (block == null) return false;
		block.setType(Material.CHEST);
<<<<<<< HEAD
		BlockState bs = block.getState();

		if (!(bs instanceof Chest)) return false;
		Inventory inv = ((Chest) bs).getInventory();
		
		LootTable table = getLootTable(key);
		if (table == null) {
			LOG.warning("Loot table '" + key + "' not found; using vanilla fallback '" + FALLBACK_KEY + "'.");
			table = getLootTable(FALLBACK_KEY);
=======
		if (Oneblock.superlegacy) {
			if (!legacyWarned) {
				LOG.warning("[Oneblock] LootTable API is unsupported on this server version; chests will spawn empty.");
				legacyWarned = true;
			}
			return true;
		}
		if (!(block.getState() instanceof Chest)) return false;
		Chest chest = (Chest) block.getState();
		Inventory inv = chest.getInventory();
		
		LootTable table = key == null ? null : Bukkit.getLootTable(key);
		if (table == null) {
			LOG.warning("[Oneblock] Loot table '" + key + "' not found; using vanilla fallback '" + FALLBACK_KEY + "'.");
			table = Bukkit.getLootTable(FALLBACK_KEY);
>>>>>>> origin/main
			if (table == null) return false;
		}
		
		try {
			LootContext ctx = new LootContext.Builder(block.getLocation()).build();
			table.fillInventory(inv, rnd, ctx);
			return true;
		} catch (Throwable t) {
<<<<<<< HEAD
			LOG.warning("Loot table '" + key + "' failed to populate: " + t.getMessage());
			return false;
		}
	}
	
	/**
	 * I don't know how to get LootTable in 1.8 - 1.12...
	 * Bukkit.getLootTable(key) for 1.13+
	 */
	public static LootTable getLootTable(NamespacedKey key) {
		if (key == null) return null;
	    if (!Oneblock.legacy) 
	        return Bukkit.getLootTable(key); // 1.13+
	    
	    return null;
	}
=======
			LOG.warning("[Oneblock] Loot table '" + key + "' failed to populate: " + t.getMessage());
			return false;
		}
	}
>>>>>>> origin/main
}
