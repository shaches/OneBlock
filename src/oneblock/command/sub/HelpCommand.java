package oneblock.command.sub;

import oneblock.Messages;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob help} - user-facing. Sends the help text to the sender;
 * admins (with {@code Oneblock.set}) get the longer admin-help variant.
 *
 * <p>Behaviour-equivalent to the legacy {@code "help"} switch case
 * extracted in Phase 3.5b.
 */
public final class HelpCommand implements Subcommand {
    @Override public String name() { return "help"; }

    @Override
    public boolean execute(CommandContext ctx) {
        ctx.sender().sendMessage(
                ctx.sender().hasPermission("Oneblock.set")
                        ? Messages.help_adm
                        : Messages.help);
        return true;
    }
}
