package oneblock.command.sub;

import org.bukkit.ChatColor;

import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;

/**
 * {@code /ob gui} - dual-purpose. With no args, opens the user-facing
 * island GUI for the sender (player-only). With one or more args (e.g.
 * {@code /ob gui true|false}), it's the admin bool-toggle for the
 * {@code gui} flag, gated behind {@code Oneblock.set}.
 *
 * <p>Behaviour-equivalent to the legacy {@code "gui"} switch case +
 * its fall-through chain to the admin {@code default} block, extracted
 * in Phase 3.5b.4. The user-facing path requires no extra permission
 * (the umbrella {@code Oneblock.join} suffices); the admin path
 * defends against non-admins internally.
 */
public final class GuiCommand implements Subcommand {
    @Override public String name() { return "gui"; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (ctx.args().length == 1) {
            // User-facing path: open the GUI for the sender. Console
            // gets nothing useful, matching legacy behaviour.
            if (ctx.player() == null) return false;
            GUI.openGUI(ctx.player());
            return true;
        }
        // Admin path: bool-toggle the `gui` flag.
        if (!ctx.sender().hasPermission("Oneblock.set")) {
            ctx.sender().sendMessage(ChatColor.RED + "You don't have permission [Oneblock.set].");
            return true;
        }
        AdminPrelude.run(ctx);
        return BoolToggleCommand.toggle(ctx, "gui");
    }
}
