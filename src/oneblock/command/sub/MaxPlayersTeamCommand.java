package oneblock.command.sub;

import org.bukkit.ChatColor;

import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob max_players_team [0..20]} - admin. Caps the number of
 * co-owners on a single island. Zero means unlimited (the default).
 * With no arg, prints the current value.
 *
 * <p>Behaviour-equivalent to the legacy {@code "max_players_team"}
 * admin case extracted in Phase 3.5b. Persisted to {@code config.yml}
 * under the {@code max_players_team} key and into
 * {@link oneblock.config.Settings} for runtime use.
 */
public final class MaxPlayersTeamCommand implements Subcommand {
    @Override public String name() { return "max_players_team"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length > 1) {
            try {
                int mpt = Integer.parseInt(args[1]);
                if (mpt < 0 || mpt > 20) throw new NumberFormatException();
                Oneblock.config.set("max_players_team", Oneblock.settings().max_players_team = mpt);
            } catch (NumberFormatException nfe) {
                ctx.sender().sendMessage(String.format("%sinvalid max_players_team value. Possible values: from 0 to 20.", ChatColor.RED));
                return true;
            }
        }
        ctx.sender().sendMessage(String.format("%smax_players_team now: %d\n0 is unlimited", ChatColor.GREEN, Oneblock.settings().max_players_team));
        return true;
    }
}
