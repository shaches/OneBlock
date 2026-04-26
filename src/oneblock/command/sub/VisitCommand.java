package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;
import oneblock.invitation.Guest;
import oneblock.utils.Utils;

/**
 * {@code /ob v} (alias) and {@code /ob visit} - user-facing. With no
 * arg, opens the visit-target picker GUI. With one arg (a player name),
 * teleports the sender to that player's island, registering them as a
 * {@link Guest} if {@code protection} is enabled.
 *
 * <p>Behaviour-equivalent to the legacy {@code "v"}/{@code "visit"}
 * switch case extracted in Phase 3.5b. Requires the
 * {@code Oneblock.visit} permission and a {@link Player} sender. Has a
 * sneaky fast-path: visiting yourself just performs {@code ob j}, so
 * {@code /ob v Alice} from Alice teleports her home rather than
 * generating an "you don't have an island" error message.
 */
public final class VisitCommand implements Subcommand {
    @Override public String name() { return "visit"; }
    @Override public String permission() { return "Oneblock.visit"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        Oneblock plugin = ctx.plugin();
        Player player = ctx.player();
        String[] args = ctx.args();
        if (args.length < 2) {
            GUI.visitGUI(player, Bukkit.getOfflinePlayers());
            return true;
        }
        OfflinePlayer inv = Utils.getOfflinePlayerByName(args[1]);
        if (inv == null) return true;
        if (inv == player) {
            player.performCommand("ob j");
            return true;
        }
        UUID uuid = inv.getUniqueId();
        final int plID = PlayerInfo.GetId(uuid);
        if (plID == -1) {
            ctx.sender().sendMessage(Messages.invite_no_island);
            return true;
        }
        PlayerInfo pinf = PlayerInfo.get(uuid);
        if (!pinf.allow_visit || (inv instanceof Player && !((Player) inv).hasPermission("Oneblock.allow_visit"))) {
            pinf.allow_visit = false;
            ctx.sender().sendMessage(Messages.not_allow_visit);
            return true;
        }
        final int result[] = plugin.getIslandCoordinates(plID);
        final int X_pl = result[0], Z_pl = result[1];

        if (Oneblock.settings().protection) Guest.list.add(new Guest(uuid, player.getUniqueId()));
        player.teleport(new Location(Oneblock.getWorld(), X_pl + 0.5, Oneblock.getY() + 1.2013, Z_pl + 0.5));
        PlayerInfo.removeBarStatic(player);
        return true;
    }
}
