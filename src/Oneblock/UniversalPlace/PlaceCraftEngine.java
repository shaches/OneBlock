package Oneblock.UniversalPlace;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class PlaceCraftEngine extends Place{

	@Override
	public boolean setType(Block block, Object material_, boolean physics) {
		if (material_ instanceof Material) 
			block.setType((Material)material_, physics);
		else if (material_ instanceof String && ((String)material_).contains(":")) {
			return placeCraftEngineBlock(block, (String)material_);
		}
		else if (material_ instanceof String) 
			return setCustomType(block, (String)material_);
		
		return false;
	}

	private boolean placeCraftEngineBlock(Block block, String namespacedId) {
		try {
			String[] pcid = namespacedId.split(":", 2);
			if (pcid.length != 2) return false;
			Class<?> keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
			Method ofMethod = keyClass.getMethod("of", String[].class);
			Object key = ofMethod.invoke(null, (Object) pcid);
			Class<?> craftEngineBlocksClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
			Method placeMethod = craftEngineBlocksClass.getMethod("place", org.bukkit.Location.class, keyClass, boolean.class);
			return (boolean) placeMethod.invoke(null, block.getLocation(), key, false);
		} catch (Exception e) {
			return false;
		}
	}
}