package oneblock.tasks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import oneblock.Oneblock;
import oneblock.PlayerCache;

/**
 * Per-island portal-particle decoration scheduled every two seconds
 * (40 ticks) by {@link Oneblock#runMainTask()}. For every online
 * player whose island is registered in {@link Oneblock#cache} the task
 * emits four {@link Particle#PORTAL} bursts at the four corners of the
 * generation block, one block above {@code y}. Honours
 * {@code Oneblock.settings().particle} as a global kill-switch.
 *
 * <p>Renamed from the inner class {@code Oneblock.TaskParticle} in
 * Phase 3.4. The {@code PARTICLE_OFFSETS} constant moved here from
 * {@code Oneblock} since this task is its only consumer.
 *
 * <p>Phase 4.3 split the work into two phases:
 * <ol>
 *   <li><b>Async (this scheduler thread):</b>
 *       {@link #collectSpawns(PlayerCache, World, int)} walks the
 *       {@link PlayerCache} snapshot - which is itself thread-safe -
 *       and produces the immutable {@code List<Location>} of every
 *       particle corner that should fire this tick. This is the hot
 *       part and stays off the main thread.</li>
 *   <li><b>Main thread:</b> a single {@link
 *       org.bukkit.scheduler.BukkitScheduler#runTask runTask} dispatch
 *       hands the list back to the main thread, which iterates and
 *       calls {@link World#spawnParticle}. Pre-Phase-4.3 the
 *       {@code spawnParticle} call ran async, which the Spigot
 *       contract documents as undefined behaviour (Paper tolerates it
 *       via packet dispatch but warns; some forks throw).</li>
 * </ol>
 * The latency between the snapshot and the dispatch is at most one
 * tick (50&nbsp;ms), negligible against the 40-tick scheduling period.
 */
public final class IslandParticleTask implements Runnable {
    private static final double[][] PARTICLE_OFFSETS = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
    private static final int PARTICLE_COUNT = 5;

    private final Oneblock plugin;

    public IslandParticleTask(Oneblock plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        if (!Oneblock.settings().particle) return;

        World w = Oneblock.getWorld();
        if (w == null) return;

        List<Location> spawns = collectSpawns(plugin.cache, w, Oneblock.getY());
        if (spawns.isEmpty()) return;

        // Hand the pre-computed list to the main thread for the actual
        // particle emission. The {@link Runnable} closes over `spawns`
        // (an ArrayList built on this thread and never re-used) - the
        // {@code runTask} submission establishes a happens-before so
        // the main thread sees the fully-populated list.
        Bukkit.getScheduler().runTask(plugin, () -> dispatchParticles(spawns));
    }

    /**
     * Pure: walks the {@link PlayerCache} snapshot and produces the list
     * of {@link Location}s where particles should fire this tick. Skips
     * players whose cached coordinates entry is {@code null} (the player
     * has no island slot, or the cache has not yet caught up with a
     * fresh {@code /ob join}). Visible for testing.
     *
     * @param cache       the player cache to read from; iterated once
     * @param world       the island world; passed to every produced
     *                    {@link Location}; must be non-null
     * @param baseYInt    the integer block-y of the generation block;
     *                    the produced locations sit at
     *                    {@code baseYInt + 0.5}
     * @return  immutable {@link ArrayList}; never {@code null}; empty
     *          if the cache held no players or every player's coords
     *          entry was {@code null}
     */
    static List<Location> collectSpawns(PlayerCache cache, World world, int baseYInt) {
        List<Location> spawns = new ArrayList<>();
        double baseY = baseYInt + 0.5;
        for (Player ponl : cache.getPlayers()) {
            int[] result = cache.getIslandCoordinates(ponl);
            if (result == null) continue;
            int X_pl = result[0], Z_pl = result[1];
            for (double[] offset : PARTICLE_OFFSETS) {
                spawns.add(new Location(world, X_pl + offset[0], baseY, Z_pl + offset[1]));
            }
        }
        return spawns;
    }

    /**
     * Main-thread side: emits the actual {@link Particle#PORTAL} burst
     * at every pre-computed location. Skips locations whose
     * {@link Location#getWorld()} is {@code null} (the world unloaded
     * between the async collect step and this dispatch). Visible for
     * testing.
     */
    static void dispatchParticles(List<Location> spawns) {
        for (Location loc : spawns) {
            World w = loc.getWorld();
            if (w == null) continue;
            w.spawnParticle(Particle.PORTAL, loc, PARTICLE_COUNT, 0, 0, 0, 0);
        }
    }
}
