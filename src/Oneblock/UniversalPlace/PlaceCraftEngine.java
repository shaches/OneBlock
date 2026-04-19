package oneblock.universalplace;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.block.Block;

public class PlaceCraftEngine extends Place{
	private static final Class<?>  KEY_CLASS;
	private static final Method    KEY_OF;
	private static final Method    BLOCKS_PLACE;
	private static final boolean   AVAILABLE;
	static {
		Class<?> keyClass = null;
		Method   keyOf    = null;
		Method   place    = null;
		boolean  ok       = false;
		try {
			keyClass = Class.forName("net.momirealms.craftengine.core.util.Key");
			Class<?> blocksClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks");
			keyOf = keyClass.getMethod("of", String[].class);
			place = blocksClass.getMethod("place", org.bukkit.Location.class, keyClass, boolean.class);
			ok = true;
		} catch (ReflectiveOperationException ignore) {
			// CraftEngine not on classpath; placer silently no-ops on namespaced ids.
		}
		KEY_CLASS    = keyClass;
		KEY_OF       = keyOf;
		BLOCKS_PLACE = place;
		AVAILABLE    = ok;
	}

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
		if (!AVAILABLE) return false;
		String[] pcid = namespacedId.split(":", 2);
		if (pcid.length != 2) return false;
		try {
			Object key = KEY_OF.invoke(null, (Object) pcid);
			Object result = BLOCKS_PLACE.invoke(null, block.getLocation(), key, false);
			return result instanceof Boolean && (Boolean) result;
		} catch (ReflectiveOperationException e) {
			return false;
		}
	}
}