package oneblock;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;

/**
 * Value payload for {@link PoolEntry.Kind#DECORATED_BLOCK}. Describes a base block material,
 * the probability (1 in {@code chance}) of placing a random decoration above it, the vertical
 * offset of that decoration, and the list of candidate decoration materials.
 *
 * <p>If {@code decorations} is {@code null} the placement code falls back to the global
 * {@code Oneblock.plugin.flowers} list at runtime.
 */
public record DecoratedBlock(XMaterial base, int chance, int offsetY, List<XMaterial> decorations) {}
