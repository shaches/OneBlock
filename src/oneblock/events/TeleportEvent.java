package oneblock.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import oneblock.Oneblock;
import oneblock.PlayerInfo;

public class TeleportEvent implements Listener {
    @EventHandler
    public void Teleport(final PlayerTeleportEvent e) {
    	if (!Oneblock.settings().border) return;
    	Location loc = e.getTo();
    	World to = loc.getWorld();
    	Player p = e.getPlayer();
    	
    	if (!to.equals(Oneblock.getWorld())) {
    		p.setWorldBorder(null);
    		return;
    	}
    	Oneblock.plugin.updateBorderLocation(p, loc);
    	Oneblock.plugin.updateBorder(p);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void Respawn(final PlayerRespawnEvent e) {
		if (!Oneblock.settings().border) return;
		Location loc = e.getRespawnLocation();
		Player p = e.getPlayer();
		if (Oneblock.getWorld().equals(loc.getWorld())) {
			Oneblock.plugin.updateBorderLocation(p, loc);
			Oneblock.plugin.updateBorder(p);
		}
		else
			p.setWorldBorder(null);
    }

    @EventHandler
    public void PlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
		if (!Oneblock.settings().progress_bar) return;
		if (PlayerInfo.list.isEmpty()) return;
    	if (e.getFrom().equals(Oneblock.getWorld()))
    		PlayerInfo.removeBarFor(e.getPlayer());
    }
}
