package oneblock.tasks;

import org.bukkit.World;

import oneblock.Oneblock;

/**
 * Async-scheduled refresh of {@link oneblock.PlayerCache} contents from
 * the live online-player snapshot of the island world. Runs every six
 * seconds (120 ticks) so the slower per-tick block-gen and particle
 * runners can iterate the cache instead of re-querying Bukkit each time.
 *
 * <p>Renamed from the inner class {@code Oneblock.TaskUpdatePlayers} in
 * Phase 3.4.
 */
public final class PlayerCacheRefreshTask implements Runnable {
    private final Oneblock plugin;

    public PlayerCacheRefreshTask(Oneblock plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        World w = Oneblock.getWor();
        if (w != null) plugin.cache.updateCache(w.getPlayers());
    }
}
