package oneblock.command.sub;

import oneblock.CommandHandler;
import oneblock.Messages;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob idreset} (no arg) - user-facing self-reset. With no arg,
 * runs {@link CommandHandler#idresetCommand} on the sender themselves
 * and then chains to {@code /ob leave /n} (the {@code /n} suffix
 * suppresses the "leave not set" warning when a fresh server has no
 * leaveworld configured yet). Requires {@code Oneblock.idreset}.
 *
 * <p>With one or more args, this command falls through to the
 * {@link AdminIdResetCommand} (which performs the
 * {@code Oneblock.set} permission check internally) - matches the
 * legacy switch fall-through chain from {@code idreset} to
 * {@code default}.
 */
public final class IdResetCommand implements Subcommand {
    private static final AdminIdResetCommand ADMIN_DELEGATE = new AdminIdResetCommand();

    @Override public String name() { return "idreset"; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (ctx.args().length == 1) {
            // User-facing self-reset path.
            if (ctx.player() == null) return false;
            if (!ctx.sender().hasPermission("Oneblock.idreset")) {
                ctx.sender().sendMessage(org.bukkit.ChatColor.RED + "You don't have permission [Oneblock.idreset].");
                return true;
            }
            if (!CommandHandler.idresetCommand(ctx.player())) return true;
            ctx.sender().sendMessage(Messages.idreset);
            ctx.player().performCommand("ob leave /n");
            return true;
        }
        // Multi-arg: delegate to admin form, which checks Oneblock.set.
        if (!ctx.sender().hasPermission("Oneblock.set")) {
            ctx.sender().sendMessage(org.bukkit.ChatColor.RED + "You don't have permission [Oneblock.set].");
            return true;
        }
        return ADMIN_DELEGATE.execute(ctx);
    }
}
