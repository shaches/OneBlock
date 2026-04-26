package oneblock.command.sub;

import oneblock.Messages;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.invitation.Invitation;

/**
 * {@code /ob accept} - user-facing. Validates the most recent
 * pending invitation for the sender and (on success) registers the
 * sender as a co-owner on the inviter's island.
 *
 * <p>Behaviour-equivalent to the legacy {@code "accept"} switch case
 * extracted in Phase 3.5b. Does not require a {@link
 * org.bukkit.entity.Player} sender because {@code Invitation.check} is
 * null-safe (returns {@code false} for a {@code null} player), which
 * matches the previous switch's behaviour where console got the "no
 * pending invitations" reply.
 */
public final class AcceptCommand implements Subcommand {
    @Override public String name() { return "accept"; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (Invitation.check(ctx.player()))
            ctx.sender().sendMessage(Messages.accept_success);
        else
            ctx.sender().sendMessage(Messages.accept_none);
        return true;
    }
}
