package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import oneblock.Island;
import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob islands [true|false | set_my_by_def | default]} - admin.
 * Multi-mode toggle for the welcome-island system:
 * <ul>
 *   <li>{@code true|false} - bool toggle for the
 *       {@code island_for_new_players} flag.</li>
 *   <li>{@code set_my_by_def} - snapshot the sender's current island
 *       layout into {@code config.yml#custom_island} so future spawn
 *       islands use it as the template (player-only,
 *       1.13+ only).</li>
 *   <li>{@code default} - clear any custom layout and revert to the
 *       built-in welcome island (1.13+ only).</li>
 * </ul>
 *
 * <p>Behaviour-equivalent to the legacy {@code "islands"} admin case
 * extracted in Phase 3.5b.4.
 */
public final class IslandsCommand implements Subcommand {
    @Override public String name() { return "islands"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length == 1) {
            ctx.sender().sendMessage(Messages.bool_format);
            return true;
        }
        if (args[1].equals("true") || args[1].equals("false")) {
            Oneblock.settings().island_for_new_players = Boolean.valueOf(args[1]);
            Oneblock.config.set("island_for_new_players", Oneblock.settings().island_for_new_players);
            ctx.sender().sendMessage(ChatColor.GREEN + "Island_for_new_players = " + Oneblock.settings().island_for_new_players);
            return true;
        }
        if (args[1].equals("set_my_by_def")) {
            if (Oneblock.legacy) {
                ctx.sender().sendMessage(ChatColor.RED + "Not supported in legacy versions!");
                return true;
            }
            if (!(ctx.sender() instanceof Player)) {
                ctx.sender().sendMessage(ChatColor.RED + "This subcommand can only be used by a player.");
                return true;
            }
            Player p = (Player) ctx.sender();
            UUID uuid = p.getUniqueId();
            if (PlayerInfo.getId(uuid) != -1) {
                int result[] = ctx.plugin().getIslandCoordinates(PlayerInfo.getId(uuid));
                Island.scan(Oneblock.getWorld(), result[0], Oneblock.getY(), result[1]);
                ctx.sender().sendMessage(ChatColor.GREEN + "A copy of your island has been successfully saved!");
                Oneblock.config.set("custom_island", Island.map());
            } else {
                ctx.sender().sendMessage(ChatColor.RED + "You don't have an island!");
            }
            return true;
        }
        if (args[1].equalsIgnoreCase("default")) {
            if (Oneblock.legacy) {
                ctx.sender().sendMessage(ChatColor.RED + "Not supported in legacy versions!");
                return true;
            }
            Oneblock.config.set("custom_island", Island.custom = null);
            ctx.sender().sendMessage(ChatColor.GREEN + "The default island is installed.");
            return true;
        }
        ctx.sender().sendMessage(Messages.bool_format);
        return true;
    }
}
