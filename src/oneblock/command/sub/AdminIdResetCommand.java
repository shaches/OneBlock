package oneblock.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import oneblock.CommandHandler;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Utils;

/**
 * {@code /ob idreset <player>} - admin form. Calls
 * {@link CommandHandler#idresetCommand} on the named offline player,
 * which removes them from {@link oneblock.PlayerInfo}, drops the
 * BossBar, optionally clears their inventory (gated on
 * {@code settings().saveplayerinventory}), and removes their
 * WorldGuard membership.
 *
 * <p>The user-facing self-{@code /ob idreset} form (no arg) is handled
 * by a separate {@code IdResetCommand} that requires only
 * {@code Oneblock.idreset} permission. Since both forms share the same
 * top-level {@code idreset} keyword, the user-facing impl owns the
 * keyword and delegates to this admin impl when {@code args.length > 1}.
 */
public final class AdminIdResetCommand implements Subcommand {
    @Override public String name() { return "idreset_admin"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length <= 1) return true;
        OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
        if (CommandHandler.idresetCommand(offpl))
            ctx.sender().sendMessage(String.format("%splayer %s id is reseted! :D", ChatColor.GREEN, args[1]));
        else
            ctx.sender().sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
        return true;
    }
}
