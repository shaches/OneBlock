package oneblock.events;

import oneblock.Oneblock;
import oneblock.PlayerInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RespawnJoinEvent implements Listener {
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRespawn(final PlayerRespawnEvent e) {
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
  public void onAutoJoin(final PlayerTeleportEvent e) {
    if (!Oneblock.settings().autojoin) return;
    Location loc = e.getTo();
    if (loc == null) return;
    World from = e.getFrom().getWorld();
    if (from == null) return;
    World to = loc.getWorld();
    if (to == null) return;
    World ob = Oneblock.getWorld();
    if (ob == null) return;
    if (!from.equals(ob)
        && to.equals(ob)
        && Math.abs(loc.getY() - (Oneblock.getY() + 1.2013)) > 1e-4) {
      e.setCancelled(true);
      e.getPlayer().performCommand("ob j");
    }
  }

  @EventHandler
  public void onJoinAuto(final PlayerJoinEvent e) {
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
