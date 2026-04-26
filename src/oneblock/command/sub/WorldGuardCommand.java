package oneblock.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.worldguard.OBWorldGuard;

/**
 * {@code /ob worldguard true|false} - admin. Toggles the OBWorldGuard
 * integration: on enable, recreates a region per existing island; on
 * disable, removes them all. Refuses politely when the WorldGuard
 * plugin is not installed or the licence is non-premium.
 *
 * <p>Behaviour-equivalent to the legacy {@code "worldguard"} admin
 * case extracted in Phase 3.5b. The integration runs through the
 * {@code OBWorldGuard.canUse} probe which reads off the
 * {@code premium=true} marker in the shaded jar metadata.
 */
public final class WorldGuardCommand implements Subcommand {
    @Override public String name() { return "worldguard"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            ctx.sender().sendMessage(String.format("%sThe WorldGuard plugin was not detected!", ChatColor.YELLOW));
            return true;
        }
        if (!OBWorldGuard.canUse) {
            ctx.sender().sendMessage(String.format("%sThis feature is only available in the premium version of the plugin!", ChatColor.YELLOW));
            return true;
        }
        String[] args = ctx.args();
        if (args.length > 1 && (args[1].equals("true") || args[1].equals("false"))) {
            OBWorldGuard.setEnabled(Boolean.valueOf(args[1]));
            Oneblock.config.set("WorldGuard", OBWorldGuard.isEnabled());
            if (OBWorldGuard.isEnabled())
                ctx.plugin().worldGuard.recreateRegions();
            else
                ctx.plugin().worldGuard.removeRegions(PlayerInfo.size());
        } else {
            ctx.sender().sendMessage(Messages.bool_format);
        }
        ctx.sender().sendMessage(String.format("%sthe OBWorldGuard is now %s", ChatColor.GREEN,
                (OBWorldGuard.isEnabled() ? "enabled." : "disabled.")));
        return true;
    }
}
