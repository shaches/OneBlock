package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import oneblock.Island;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Utils;

/**
 * {@code /ob clear <player>} - admin. Wipes a player's island level
 * and breaks counter back to zero, hides their BossBar, and erases
 * their island geometry (replacing it with air around the centre block
 * out to {@code offset/4} radius). Does not remove the {@link
 * PlayerInfo} entry; {@code /ob idreset} is the harder reset that
 * also frees the slot.
 *
 * <p>Behaviour-equivalent to the legacy {@code "clear"} admin case
 * extracted in Phase 3.5b.
 */
public final class ClearCommand implements Subcommand {
    @Override public String name() { return "clear"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length <= 1) {
            ctx.sender().sendMessage(String.format("%sinvalid format. try: /ob clear 'nickname'", ChatColor.RED));
            return true;
        }
        OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
        if (offpl == null) {
            ctx.sender().sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
            return true;
        }
        UUID uuid = offpl.getUniqueId();
        int id = PlayerInfo.getId(uuid);
        if (id == -1) {
            ctx.sender().sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
            return true;
        }
        PlayerInfo inf = PlayerInfo.get(id);
        inf.breaks = 0;
        inf.lvl = 0;
        if (Oneblock.settings().progress_bar)
            inf.bar.setVisible(false);
        int result[] = ctx.plugin().getIslandCoordinates(id);
        Island.clear(Oneblock.getWorld(), result[0], Oneblock.getY(), result[1], Oneblock.getOffset() / 4);
        ctx.sender().sendMessage(String.format("%splayer %s island is destroyed! :D", ChatColor.GREEN, args[1]));
        return true;
    }
}
