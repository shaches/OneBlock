package oneblock.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import oneblock.Level;
import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Utils;

/**
 * {@code /ob progress_bar <subcmd> [args]} - admin. Multi-form
 * controller for the per-player level progress BossBar:
 * <ul>
 *   <li>{@code true|false} - master enable / disable.</li>
 *   <li>{@code color <BarColor>} - set the bar colour
 *       (e.g. {@code RED}, {@code GREEN}).</li>
 *   <li>{@code style <BarStyle>} - set the bar style
 *       (e.g. {@code SOLID}, {@code SEGMENTED_10}).</li>
 *   <li>{@code level} - render the level NAME instead of templated
 *       text.</li>
 *   <li>{@code settext <multi-word text>} - render templated text
 *       (joined from args[2..N]); PlaceholderAPI substitution applies
 *       at render time.</li>
 * </ul>
 *
 * <p>Refuses politely on superlegacy server versions (pre-1.9). The
 * `color`/`style`/`level`/`settext` subcommands are no-ops when
 * {@code progress_bar} is currently disabled.
 *
 * <p>Behaviour-equivalent to the legacy {@code "progress_bar"} admin
 * case extracted in Phase 3.5b.4.
 */
public final class ProgressBarCommand implements Subcommand {
    @Override public String name() { return "progress_bar"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (Oneblock.superlegacy) {
            ctx.sender().sendMessage(String.format("%sYou server version is super legacy! ProgressBar unsupported!", ChatColor.RED));
            return true;
        }
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length == 1) {
            ctx.sender().sendMessage(String.format("%sand?", ChatColor.YELLOW));
            return true;
        }
        if (args[1].equals("true") || args[1].equals("false")) {
            Oneblock.settings().progressBar = Boolean.valueOf(args[1]);
            Oneblock.configManager.loadBlocks();
            Oneblock.config.set("progress_bar", Oneblock.settings().progressBar);
            return true;
        }

        if (!Oneblock.settings().progressBar) return true;

        boolean isColor = args[1].equalsIgnoreCase("color");
        if (isColor || args[1].equalsIgnoreCase("style")) {
            if (args.length == 2) {
                ctx.sender().sendMessage(String.format("%senter a %s name.", ChatColor.YELLOW, args[1].toLowerCase()));
                return true;
            }
            try {
                if (isColor) {
                    Level.max.color = BarColor.valueOf(args[2]);
                    Oneblock.config.set("progress_bar_color", Level.max.color.toString());
                } else {
                    Level.max.style = BarStyle.valueOf(args[2]);
                    Oneblock.config.set("progress_bar_style", Level.max.style.toString());
                }
                Oneblock.configManager.loadBlocks();
                ctx.sender().sendMessage(String.format("%sProgress bar %s = %s", ChatColor.GREEN, args[1].toLowerCase(), args[2]));
            } catch (Exception e) {
                ctx.sender().sendMessage(String.format("%sPlease enter a valid %s. For example: %s", ChatColor.YELLOW, args[1].toLowerCase(), isColor ? "RED" : "SOLID"));
            }
            return true;
        }
        if (args[1].equalsIgnoreCase("level")) {
            Oneblock.settings().lvlBarMode = true;
            Oneblock.config.set("progress_bar_text", "level");
            Oneblock.configManager.setupProgressBar();
            return true;
        }
        if (args[1].equalsIgnoreCase("settext")) {
            String txt_bar = "";
            for (int i = 2; i < args.length; i++)
                txt_bar = i == 2 ? args[i] : String.format("%s %s", txt_bar, args[i]);
            Oneblock.settings().lvlBarMode = false;
            Oneblock.config.set("progress_bar_text", Oneblock.settings().phText = Utils.translateColorCodes(txt_bar));
            Oneblock.configManager.setupProgressBar();
            return true;
        }
        ctx.sender().sendMessage(String.format("%strue, false, settext or level only!", ChatColor.RED));
        return true;
    }
}
