package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;
import oneblock.invitation.Invitation;

/**
 * {@code /ob invite <player>} - user-facing. Sends a co-owner invite
 * to the named online player. The invitee gets a chat prompt + GUI;
 * the inviter gets a confirmation. Cap-checked against
 * {@code settings().maxPlayersTeam} (zero = unlimited).
 *
 * <p>Behaviour-equivalent to the legacy {@code "invite"} switch case
 * extracted in Phase 3.5b. Requires {@code Oneblock.invite} and a
 * {@link Player} sender (console has no island to invite TO).
 */
public final class InviteCommand implements Subcommand {
    @Override public String name() { return "invite"; }
    @Override public String permission() { return "Oneblock.invite"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        Player player = ctx.player();
        String[] args = ctx.args();
        if (args.length < 2) {
            ctx.sender().sendMessage(Messages.invite_usage);
            return true;
        }
        Player inv = Bukkit.getPlayer(args[1]);
        if (inv == null) return true;
        if (inv == player) {
            ctx.sender().sendMessage(Messages.invite_yourself);
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (PlayerInfo.getId(uuid) == -1) {
            ctx.sender().sendMessage(Messages.invite_no_island);
            return true;
        }
        int maxTeam = Oneblock.settings().maxPlayersTeam;
        if (maxTeam != 0) {
            PlayerInfo pinf = PlayerInfo.get(uuid);
            if (pinf.uuids.size() >= maxTeam) {
                ctx.sender().sendMessage(String.format(Messages.invite_team, maxTeam));
                return true;
            }
        }
        Invitation.add(uuid, inv.getUniqueId());
        String name = player.getName();
        GUI.acceptGUI(inv, name);
        inv.sendMessage(String.format(Messages.invited, name));
        ctx.sender().sendMessage(String.format(Messages.invited_success, inv.getName()));
        return true;
    }
}
