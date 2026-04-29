package oneblock.tasks;

import oneblock.Oneblock;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Async-scheduled poll that resolves the configured island {@code world} once Bukkit has finished
 * loading worlds. On success it folds the resolved {@link World} into {@link Oneblock#origin()}
 * (preserving the already-loaded {@code x/y/z/offset}), kicks off the four steady-state runners via
 * {@link Oneblock#runMainTask()} and triggers a {@link Oneblock#reload()} so all dependent caches
 * see the new world.
 *
 * <p>Renamed from the inner class {@code Oneblock.Initialization} in Phase 3.4. The class is no
 * longer self-cancelling - {@code runMainTask} cancels the existing task pool, which transitively
 * cancels this one via the standard Bukkit scheduler API.
 */
public final class WorldInitTask implements Runnable {
  private final Oneblock plugin;

  public WorldInitTask(Oneblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    if (Oneblock.getWorld() != null) return;
    String worldName = Oneblock.config.getString("world");
    String leaveWorldName = Oneblock.config.getString("leaveworld");
    final World w = worldName != null ? Bukkit.getWorld(worldName) : null;
    World leaveWorld = leaveWorldName != null ? Bukkit.getWorld(leaveWorldName) : null;
    if (leaveWorld != null) Oneblock.leavewor = leaveWorld;
    if (w != null) {
      // Atomic swap: fold the freshly-resolved world into ORIGIN while
      // preserving the existing x/y/z/offset loaded earlier by
      // ConfigManager.loadMainConfig(). Runs on the async scheduler thread.
      plugin.updateOriginWorld(w);
      plugin.getLogger().info("The initialization of the world was successful!");
      plugin.runMainTask();
      plugin.reload();
    } else {
      plugin
          .getLogger()
          .info(
              "Waiting for initialization of world '"
                  + Oneblock.config.getString("world")
                  + "'...");
    }
  }
}
