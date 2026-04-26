package oneblock.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob border true|false} - admin. Schedules a
 * {@code Oneblock.reloadBorders()} call 2 ticks later so the
 * subsequent bool toggle persists {@code border=true|false} via
 * {@link AdminPrelude}'s delayed save first; then runs the shared
 * {@link BoolToggleCommand#toggle} body. Refuses politely on legacy
 * server versions (pre-1.18.2) where {@code WorldBorder} is
 * unsupported.
 *
 * <p>The 2-tick delay + ordering is load-bearing: if
 * {@code reloadBorders} ran before the {@code config.set} write, the
 * border state and the persisted state would diverge for that single
 * tick. Behaviour-equivalent to the legacy {@code "border"} admin
 * fall-through case extracted in Phase 3.5b.4.
 */
public final class BorderCommand implements Subcommand {
    @Override public String name() { return "border"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (!Oneblock.isBorderSupported) {
            ctx.sender().sendMessage(String.format("%sThe border can only be used on version 1.18.2 and above!", ChatColor.YELLOW));
            return true;
        }
        AdminPrelude.run(ctx);
        // reloadBorders is scheduled 2 ticks later so the persisted
        // `border=true/false` write below lands first; the ordering is
        // load-bearing.
        Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> ctx.plugin().reloadBorders(), 2L);
        return BoolToggleCommand.toggle(ctx, "border");
    }
}
