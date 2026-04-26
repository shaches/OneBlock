package oneblock.events;
<<<<<<< HEAD

import static oneblock.Oneblock.*;
=======
>>>>>>> origin/main

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import oneblock.Oneblock;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;

public class ItemsAdderEvent implements Listener {
	@EventHandler
    public void ItemsAdderLoad(ItemsAdderLoadDataEvent event) {
		Oneblock.configManager.loadBlocks();
    }
}
