package oneblock.placement;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public abstract class Place {

  public enum Type {
    legacy,
    basic,
    ItemsAdder,
    Oraxen,
    Nexo,
    CraftEngine
  }

  public static Place getPlacerByType(Type type) {
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

  /**
   * Defensive legacy fallback path for placer subclasses that still receive raw {@code String}
   * payloads (e.g. unresolved Oraxen / Nexo custom-block ids). Only runs the {@code /command}
   * branch since chest-name tokens are handled upstream as {@code LOOT_TABLE} pool entries.
   */
  public boolean setCustomType(Block block, String command) {
    return executeCommand(block, command);
  }

  /**
   * Execute a {@code /command} entry. The command string is the body after the leading slash and is
   * passed through {@link String#format} with the block's {@code (x, y, z)} as positional arguments
   * so admins can reference the generated-block coordinates in the command template.
   */
  public static boolean executeCommand(Block block, String command) {
    if (command == null || command.isEmpty() || command.charAt(0) != '/') return false;
    String template = command.substring(1);
    String dispatched;
    try {
      dispatched = String.format(template, block.getX(), block.getY(), block.getZ());
    } catch (java.util.IllegalFormatException ife) {
      Bukkit.getLogger()
          .warning(
              "[Oneblock] Invalid format specifier in blocks.yml command template: '"
                  + template
                  + "' ("
                  + ife.getMessage()
                  + ")");
      return false;
    }
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), dispatched);
    return true;
  }
}
