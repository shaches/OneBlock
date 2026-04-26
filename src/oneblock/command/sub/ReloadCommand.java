package oneblock.command.sub;

import org.bukkit.ChatColor;

import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob reload} - admin. Re-reads every yaml under the plugin
 * data folder, recomputes the BossBar / level pool / chest alias /
 * region caches, and re-creates WorldGuard regions. Equivalent to a
 * full restart for plugin-internal state but does not affect player
 * data.
 *
 * <p>Behaviour-equivalent to the legacy {@code "reload"} admin case
 * extracted in Phase 3.5b.
 */
public final class ReloadCommand implements Subcommand {
    @Override public String name() { return "reload"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        ctx.sender().sendMessage(String.format("%sReloading Plugin & Plugin Modules.", ChatColor.YELLOW));
        ctx.plugin().reload();
        ctx.sender().sendMessage(String.format("%sAll *.yml reloaded!", ChatColor.GREEN));
        return true;
    }
}
