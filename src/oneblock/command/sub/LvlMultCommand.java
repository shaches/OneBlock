package oneblock.command.sub;

import org.bukkit.ChatColor;

import oneblock.Level;
import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob lvl_mult [0..20]} - admin. Sets {@link Level#multiplier},
 * the global multiplier applied to every level's {@code length}
 * (blocks-needed-to-advance) at parse time. Triggers a Blocks reload
 * so the new multiplier takes effect immediately. With no arg, prints
 * the current value.
 *
 * <p>Behaviour-equivalent to the legacy {@code "lvl_mult"} admin case
 * extracted in Phase 3.5b.
 */
public final class LvlMultCommand implements Subcommand {
    @Override public String name() { return "lvl_mult"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length > 1) {
            try {
                int lvl = Integer.parseInt(args[1]);
                if (lvl < 0 || lvl > 20) throw new NumberFormatException();
                Oneblock.config.set("level_multiplier", Level.multiplier = lvl);
                ctx.plugin().configManager.Blockfile();
            } catch (NumberFormatException nfe) {
                ctx.sender().sendMessage(String.format("%sinvalid multiplier value. Possible values: from 0 to 20.", ChatColor.RED));
                return true;
            }
        }
        ctx.sender().sendMessage(String.format("%slevel multiplier now: %d\n5 by default", ChatColor.GREEN, Level.multiplier));
        return true;
    }
}
