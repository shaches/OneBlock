package oneblock.events;

import oneblock.Oneblock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class TeleportNetherEvent implements Listener {
  @EventHandler
  public void onNetherPortal(final PlayerPortalEvent e) {
    if (Oneblock.settings().allowNether) return;
    World from = e.getFrom().getWorld();
    if (from == null) return;
    if (!from.equals(Oneblock.getWorld())) return;

    Location toLoc = e.getTo();
    if (toLoc == null) return;
    World to = toLoc.getWorld();
    if (to == null) return;
    if (to.getEnvironment() == World.Environment.NETHER) e.setCancelled(true);
  }
}
