package oneblock.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIHolder implements InventoryHolder {

  public enum GUIType {
    MAIN_MENU,
    TOP,
    INVITE,
    VISIT
  }

  private final GUIType guiType;

  public GUIHolder(GUIType guiType) {
    this.guiType = guiType;
  }

  @Override
  public Inventory getInventory() {
    throw new UnsupportedOperationException("getInventory() not supported for this holder");
  }

  public GUIType getGuiType() {
    return guiType;
  }
}
