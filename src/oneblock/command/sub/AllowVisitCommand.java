package oneblock.command.sub;

import java.util.UUID;

import oneblock.Messages;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob allow_visit} - user-facing. Toggles the sender's
 * "allow visitors" flag and sends a confirmation message reflecting
 * the new value.
 *
 * <p>Behaviour-equivalent to the legacy {@code "allow_visit"} switch
 * case extracted in Phase 3.5b. Requires {@code Oneblock.allow_visit}
 * permission and a {@link org.bukkit.entity.Player} sender (the toggle
 * is per-player, console has no island).
 */
public final class AllowVisitCommand implements Subcommand {
    @Override public String name() { return "allow_visit"; }
    @Override public String permission() { return "Oneblock.allow_visit"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        UUID uuid = ctx.player().getUniqueId();
        if (PlayerInfo.GetId(uuid) == -1) return true;
        PlayerInfo inf = PlayerInfo.get(uuid);
        inf.allow_visit = !inf.allow_visit;
        ctx.player().sendMessage(inf.allow_visit ? Messages.allowed_visit : Messages.forbidden_visit);
        return true;
    }
}
