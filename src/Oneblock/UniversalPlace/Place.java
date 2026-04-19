package Oneblock.UniversalPlace;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import Oneblock.ChestItems;

public abstract class Place {
	
	public enum Type
	{
	    legacy,
	    basic,
	    ItemsAdder,
	    Oraxen,
	    Nexo,
	    CraftEngine
	}
	
	public static Place GetPlacerByType(Type type) {
		switch (type) {
		case legacy:
			return new Place1_8to1_12();
		case ItemsAdder:
			return new PlaceItemsAdder();
		case Oraxen:
			return new PlaceOraxen();
		case Nexo:
			return new PlaceNexo();
		case CraftEngine:
			return new PlaceCraftEngine();
		default:
			return new Place1_13plus();
		}
	}
	
	public abstract boolean setType(Block block, Object material_, boolean physics);
	
	public boolean setCustomType(Block block, String command) {
		if (command == null || command.isEmpty()) return false;
		if (command.charAt(0) == '/') {
			String template = command.replaceFirst("/", "");
			String dispatched;
			try {
				dispatched = String.format(template, block.getX(), block.getY(), block.getZ());
			} catch (java.util.IllegalFormatException ife) {
				// Admin-provided blocks.yml template is malformed; log once per block-gen instead of
				// throwing on every tick (which would DoS the main task scheduler).
				Bukkit.getLogger().warning("[Oneblock] Invalid format specifier in blocks.yml command template: '"
						+ template + "' (" + ife.getMessage() + ")");
				return false;
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), dispatched);
			return true;
		}
    	
		block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        Inventory inv = chest.getInventory();
        ChestItems.fillChest(inv, command);
    	return true;
    }
}