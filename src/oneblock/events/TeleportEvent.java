package oneblock.events;

import oneblock.Oneblock;
import oneblock.PlayerInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportEvent implements Listener {
  @EventHandler
  public void onTeleport(final PlayerTeleportEvent e) {
    if (!Oneblock.settings().border) return;
    Location loc = e.getTo();
    if (loc == null) return;
    World to = loc.getWorld();
    if (to == null) return;
    Player p = e.getPlayer();

    if (!to.equals(Oneblock.getWorld())) {
      p.setWorldBorder(null);
      return;
    }
    Oneblock.plugin.updateBorderLocation(p, loc);
    Oneblock.plugin.updateBorder(p);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerRespawn(final PlayerRespawnEvent e) {
    if (!Oneblock.settings().border) return;
    Location loc = e.getRespawnLocation();
    Player p = e.getPlayer();
    if (Oneblock.getWorld().equals(loc.getWorld())) {
      Oneblock.plugin.updateBorderLocation(p, loc);
      Oneblock.plugin.updateBorder(p);
    } else p.setWorldBorder(null);
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
    if (!Oneblock.settings().progressBar) return;
    if (PlayerInfo.list.isEmpty()) return;
    if (e.getFrom().equals(Oneblock.getWorld())) PlayerInfo.removeBarFor(e.getPlayer());
  }
}
