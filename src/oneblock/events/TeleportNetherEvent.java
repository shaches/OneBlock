package oneblock.events;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

import oneblock.Oneblock;

public class TeleportNetherEvent implements Listener {
	@EventHandler
    public void NetherPortal(final PlayerPortalEvent e) {
    	if (Oneblock.settings().allow_nether) return;
    	World from = e.getFrom().getWorld();
    	if (!from.equals(Oneblock.getWorld())) return;
    	
    	World to = e.getTo().getWorld();
        if (to.getEnvironment() == World.Environment.NETHER) 
        	e.setCancelled(true);
    }
}
