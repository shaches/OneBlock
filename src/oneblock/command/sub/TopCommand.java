package oneblock.command.sub;

import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;

/**
 * {@code /ob top} - user-facing. Opens the top-list GUI for the
 * sender, ranking islands by level then breaks descending.
 *
 * <p>Behaviour-equivalent to the legacy {@code "top"} switch case
 * extracted in Phase 3.5b. Console invocation is a silent no-op
 * (matches the legacy behaviour where {@code GUI.topGUI(null)} was
 * tolerated) - we keep that contract by NOT flagging
 * {@code requiresPlayer()}.
 */
public final class TopCommand implements Subcommand {
    @Override public String name() { return "top"; }

    @Override
    public boolean execute(CommandContext ctx) {
        GUI.topGUI(ctx.player());
        return true;
    }
}
