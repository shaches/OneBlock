package oneblock.tasks;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.invitation.Guest;

/**
 * Main-thread block-generation pulse: every four seconds (80 ticks) it
 * walks every online island player and, if the generation block is air,
 * delegates to {@link Oneblock#generateBlock} to (re)materialise it. Also
 * enforces the {@code protection} flag - players outside their cell are
 * teleported back via {@code /ob j} after a {@link Messages#protection}
 * message.
 *
 * <p>Renamed from the inner class {@code Oneblock.Task} (whose body
 * carried a single inline comment, "// SubBlockGen", indicating the
 * intent) in Phase 3.4. Runs on the main thread because
 * {@link Block#getType} and {@code generateBlock} both need synchronous
 * world access.
 */
public final class IslandBlockGenTask implements Runnable {
    private final Oneblock plugin;

    public IslandBlockGenTask(Oneblock plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        for (Player player : plugin.cache.getPlayers()) {
            if (player.getWorld() != Oneblock.getWorld()) continue;
            final UUID uuid = player.getUniqueId();
            final int result[] = plugin.cache.getIslandCoordinates(player);
            final int X_pl = result[0], Z_pl = result[1], plID = result[2];

            if (Oneblock.settings().protection && !player.hasPermission("Oneblock.ignoreBarrier")) {
                boolean CheckGuest = false;
                Location loc = player.getLocation();
                PlayerInfo inf = Guest.getPlayerInfo(uuid);
                if (inf != null) {
                    int crd[] = plugin.getIslandCoordinates(PlayerInfo.getId(inf.uuid));
                    CheckGuest = plugin.isWithinIslandBounds(loc, crd[0], crd[1]);
                    if (!CheckGuest) Guest.remove(uuid);
                }
                if (!plugin.isWithinIslandBounds(loc, X_pl, Z_pl) && !CheckGuest) {
                    player.performCommand("ob j");
                    player.sendMessage(Messages.protection);
                    continue;
                }
            }

            final Block block = Oneblock.getWorld().getBlockAt(X_pl, Oneblock.getY(), Z_pl);
            if (block.getType() != Material.AIR) continue;
            if (PlayerInfo.getId(uuid) == -1) continue;

            plugin.generateBlock(X_pl, Z_pl, plID, player, block);
        }
    }
}
