package oneblock.events;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import oneblock.Oneblock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ItemsAdderEvent implements Listener {
  @EventHandler
  public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
    Oneblock.configManager.loadBlocks();
  }
}
