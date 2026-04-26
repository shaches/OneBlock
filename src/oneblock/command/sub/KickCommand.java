package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Utils;
import oneblock.worldguard.OBWorldGuard;

/**
 * {@code /ob kick <player>} - user-facing. The island owner removes a
 * co-owner; if the kicked player is currently online and standing on
 * the island, they are also bounced back to the world spawn via
 * {@code /ob j} (which routes through their own join logic).
 *
 * <p>Behaviour-equivalent to the legacy {@code "kick"} switch case
 * extracted in Phase 3.5b. Requires {@code Oneblock.kick} and a
 * {@link Player} sender (the owner). The owner cannot kick themselves;
 * the kick is also a no-op if the named player is not actually on the
 * sender's invite list.
 */
public final class KickCommand implements Subcommand {
    @Override public String name() { return "kick"; }
    @Override public String permission() { return "Oneblock.kick"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        Oneblock plugin = ctx.plugin();
        Player player = ctx.player();
        String[] args = ctx.args();
        if (args.length < 2) {
            ctx.sender().sendMessage(Messages.kick_usage);
            return true;
        }
        OfflinePlayer member = Utils.getOfflinePlayerByName(args[1]);
        if (member == null) return true;
        if (member == player) {
            ctx.sender().sendMessage(Messages.kick_yourself);
            return true;
        }
        UUID owner_uuid = player.getUniqueId(), member_uuid = member.getUniqueId();
        if (!PlayerInfo.existsAsOwner(owner_uuid))
            return true;
        int ownerID = PlayerInfo.getId(owner_uuid);
        PlayerInfo info = PlayerInfo.get(ownerID);
        if (info.uuids.contains(member_uuid)) {
            info.removeInvite(member_uuid);
            if (OBWorldGuard.isEnabled())
                plugin.worldGuard.removeMember(member_uuid, ownerID);
        }
        if (!(member instanceof Player)) return true;
        Player member_ex = (Player) member;
        int memberID = plugin.findNearestRegionId(member_ex.getLocation());
        if (memberID == ownerID) {
            if (!member_ex.hasPermission("Oneblock.set"))
                member_ex.performCommand("ob j");
            info.removeBar(member_ex);
            ctx.sender().sendMessage(member.getName() + Messages.kicked);
        }
        return true;
    }
}
