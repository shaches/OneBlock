package oneblock.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import oneblock.Oneblock;
import oneblock.PlayerInfo;

public class RespawnJoinEvent implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
    public void Respawn(final PlayerRespawnEvent e) {
		if (!Oneblock.settings().rebirth) return;
		Player pl = e.getPlayer();
		World world = Oneblock.getWorld();
		if (!pl.getWorld().equals(world)) return;
		int plID = PlayerInfo.getId(pl.getUniqueId());
		if (plID == -1) return;
		
		int result[] = Oneblock.plugin.getIslandCoordinates(plID);
		Location loc = new Location(world, result[0] + .5, Oneblock.getY() + 1.75, result[1] + .5);
		e.setRespawnLocation(loc);
    }
    @EventHandler
    public void AutoJoin(final PlayerTeleportEvent e) {
		if (!Oneblock.settings().autojoin)
			return;
		Location loc = e.getTo();
		World from = e.getFrom().getWorld();
		World to = loc.getWorld();
		World ob = Oneblock.getWorld();
		if (!from.equals(ob) && to.equals(ob) && loc.getY() != Oneblock.getY() + 1.2013) {
			e.setCancelled(true);
			e.getPlayer().performCommand("ob j");
		}
    }
    @EventHandler
    public void JoinAuto(final PlayerJoinEvent e) {
		Player pl = e.getPlayer();
		if (pl.getWorld().equals(Oneblock.getWorld())) {
			if (Oneblock.settings().autojoin) pl.performCommand("ob j");
			if (Oneblock.settings().border) {
				Oneblock.plugin.updateBorderLocation(pl, pl.getLocation());
				Oneblock.plugin.updateBorder(pl);
			}
		}
	}
}
