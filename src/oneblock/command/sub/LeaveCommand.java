package oneblock.command.sub;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob leave} - user-facing. Removes the per-player progress
 * BossBar and teleports the sender to the configured leave-world. If
 * leave-world is unset, replies with a "leave not set" message
 * (suppressed when called as part of an internal chain like
 * {@code /ob leave /n} from the idreset path).
 *
 * <p>Behaviour-equivalent to the legacy {@code "leave"} switch case
 * extracted in Phase 3.5b. Player is required because we need a
 * concrete {@code Player} reference to teleport.
 */
public final class LeaveCommand implements Subcommand {
    @Override public String name() { return "leave"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        PlayerInfo.removeBarFor(ctx.player());
        if (Oneblock.leavewor == null || Oneblock.config.getDouble("yleave") == 0) {
            String[] args = ctx.args();
            if (!args[args.length - 1].equals("/n"))
                ctx.sender().sendMessage(Messages.leave_not_set);
            return true;
        }
        ctx.player().teleport(ctx.plugin().getLeave());
        return true;
    }
}
