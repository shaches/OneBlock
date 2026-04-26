package oneblock.command.sub;

import org.bukkit.ChatColor;

import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * Shared admin-toggle implementation for boolean flags whose only
 * surface area is "{@code /ob <flag> true|false}". One instance is
 * registered per flag-name with the {@link oneblock.CommandHandler}
 * router; each instance carries the keyword it should dispatch under
 * and the matching {@code config.yml} key (which is the same string).
 *
 * <p>Currently backs eleven flags: {@code circlemode},
 * {@code useemptyislands}, {@code protection}, {@code droptossup},
 * {@code physics}, {@code autojoin}, {@code particle},
 * {@code allow_nether}, {@code saveplayerinventory}, {@code gui},
 * {@code rebirth_on_the_island}. The two specialised commands that
 * reuse this logic with extra side-effects (`border` reloads
 * world-borders, `gui` no-arg opens the menu) instantiate a
 * {@code BoolToggleCommand} internally and forward to it.
 *
 * <p>Behaviour-equivalent to the legacy 11-case fall-through chain in
 * the admin {@code switch} block, extracted in Phase 3.5b.4.
 */
public final class BoolToggleCommand implements Subcommand {
    private final String flagName;

    public BoolToggleCommand(String flagName) { this.flagName = flagName; }

    @Override public String name() { return flagName; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        return toggle(ctx, flagName);
    }

    /**
     * Body of the toggle, exposed so {@link BorderCommand} and
     * {@link GuiCommand} can reuse it after their own pre-action
     * (border-reload schedule, GUI open) without re-running
     * {@link AdminPrelude}.
     */
    static boolean toggle(CommandContext ctx, String flagName) {
        String[] args = ctx.args();
        if (args.length > 1 && (args[1].equals("true") || args[1].equals("false"))) {
            Oneblock.config.set(flagName, Boolean.valueOf(args[1]));
            ctx.plugin().configManager.updateBoolParameters();
        } else {
            ctx.sender().sendMessage(Messages.bool_format);
        }
        ctx.sender().sendMessage(String.format("%s%s is now %s", ChatColor.GREEN, flagName,
                (Oneblock.config.getBoolean(flagName) ? "enabled." : "disabled.")));
        return true;
    }
}
