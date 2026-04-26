package oneblock.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import oneblock.Oneblock;

/**
 * Async-scheduled per-island particle effect: every two seconds (40
 * ticks) every online player's home island emits four
 * {@link Particle#PORTAL} bursts at the four corners of the generation
 * block, one block above {@code y}. Honours
 * {@code Oneblock.settings().particle} as a global kill-switch.
 *
 * <p>Renamed from the inner class {@code Oneblock.TaskParticle} in
 * Phase 3.4. The {@code PARTICLE_OFFSETS} constant moved here from
 * {@code Oneblock} since this task is its only consumer.
 *
 * <p><b>Caveat (unchanged from Phase 1):</b> {@code World.spawnParticle}
 * is main-thread-only on most server forks; this task runs async because
 * the cache iteration itself is the hot part. The particle send is
 * tolerated because Bukkit silently no-ops or dispatches via packet on
 * Paper. If a future fork enforces strict main-thread-only the call
 * site needs to wrap {@code spawnParticle} in {@code runTask}.
 */
public final class IslandParticleTask implements Runnable {
    private static final double[][] PARTICLE_OFFSETS = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};

    private final Oneblock plugin;

    public IslandParticleTask(Oneblock plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        if (!Oneblock.settings().particle) return;

        for (Player ponl: plugin.cache.getPlayers()) {
            int[] result = plugin.cache.getIslandCoordinates(ponl);
            if (result == null) continue;
            int X_pl = result[0], Z_pl = result[1];
            double baseY = Oneblock.getY() + 0.5;

            for (double[] offset : PARTICLE_OFFSETS) {
                Location loc = new Location(Oneblock.getWor(), X_pl + offset[0], baseY, Z_pl + offset[1]);
                Oneblock.getWor().spawnParticle(Particle.PORTAL, loc, 5, 0, 0, 0, 0);
            }
        }
    }
}
