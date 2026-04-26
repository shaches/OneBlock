package oneblock.events;

import static oneblock.Oneblock.*;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import oneblock.PlayerInfo;

public class TeleportEvent implements Listener {
    @EventHandler
    public void Teleport(final PlayerTeleportEvent e) {
    	if (!settings().border) return;
    	Location loc = e.getTo();
    	World to = loc.getWorld();
    	Player p = e.getPlayer();
    	
    	if (!to.equals(getWorld())) {
    		p.setWorldBorder(null);
    		return;
    	}
    	plugin.updateBorderLocation(p, loc);
    	plugin.updateBorder(p);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void Respawn(final PlayerRespawnEvent e) {
		if (!settings().border) return;
		Location loc = e.getRespawnLocation();
		Player p = e.getPlayer();
		if (getWorld().equals(loc.getWorld())) {
			plugin.updateBorderLocation(p, loc);
			plugin.updateBorder(p);
		}
		else
			p.setWorldBorder(null);
    }

    @EventHandler
    public void PlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
		if (!settings().progress_bar) return;
		if (PlayerInfo.list.isEmpty()) return;
    	if (e.getFrom().equals(getWorld()))
    		PlayerInfo.removeBarFor(e.getPlayer());
    }
}
