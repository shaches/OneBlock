package oneblock.tasks;

import oneblock.IslandOrigin;
import oneblock.Oneblock;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Async-scheduled poll that resolves the configured island {@code world} once Bukkit has finished
 * loading worlds. On success it folds the resolved {@link World} into {@link Oneblock#origin()}
 * (preserving the already-loaded {@code x/y/z/offset}), kicks off the four steady-state runners via
 * {@link Oneblock#runMainTask()} and triggers a {@link Oneblock#reload()} so all dependent caches
 * see the new world.
 */
public final class WorldInitTask implements Runnable {
  private final Oneblock plugin;

  public WorldInitTask(Oneblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
      if (Oneblock.getWor() != null) return;
      final World w = Bukkit.getWorld(Oneblock.config.getString("world"));
      
      if (w != null) {
    	  Oneblock.ORIGIN.updateAndGet(prev -> new IslandOrigin(w, prev.x(), prev.y(), prev.z(), prev.offset()));
    	  Oneblock.leavewor = Bukkit.getWorld(Oneblock.config.getString("leaveworld"));
    	  plugin.getLogger().info("The initialization of the world was successful!");
    	  plugin.runMainTask();
    	  plugin.reload();
      } else {
    	  plugin.getLogger().info("Waiting for initialization of world '" + Oneblock.config.getString("world") + "'...");
      }
  }
}
