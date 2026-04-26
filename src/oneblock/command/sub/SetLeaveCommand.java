package oneblock.command.sub;

import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob setleave} - admin. Captures the sender's current location
 * and persists it as the leave-world destination that {@code /ob leave}
 * teleports players to. Player-only because we read coordinates from
 * the sender's body.
 *
 * <p>Behaviour-equivalent to the legacy {@code "setleave"} admin case
 * extracted in Phase 3.5b.
 */
public final class SetLeaveCommand implements Subcommand {
    @Override public String name() { return "setleave"; }
    @Override public String permission() { return "Oneblock.set"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        ctx.plugin().setLeave(ctx.player().getLocation());
        return true;
    }
}
