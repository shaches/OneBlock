package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Utils;

/**
 * {@code /ob setlevel <player> <level>} - admin. Force-sets a player's
 * island level to the given integer (0..10000), zeroes their breaks
 * counter, and re-renders the BossBar if {@code progress_bar} is on
 * and the player is online.
 *
 * <p>Behaviour-equivalent to the legacy {@code "setlevel"} admin case
 * extracted in Phase 3.5b. Used primarily for testing / loadout
 * gifting; the matching {@code clear} command resets levels to zero,
 * not arbitrary values.
 */
public final class SetLevelCommand implements Subcommand {
    @Override public String name() { return "setlevel"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length <= 2) {
            ctx.sender().sendMessage(String.format("%sinvalid format. try: /ob setlevel 'nickname' 'level'", ChatColor.RED));
            return true;
        }
        OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
        UUID uuid = offpl.getUniqueId();
        int plID = PlayerInfo.GetId(uuid);
        if (plID != -1) {
            int setlvl = 0;
            try {
                setlvl = Integer.parseInt(args[2]);
                if (setlvl < 0 || setlvl > 10000) throw new NumberFormatException();
            } catch (NumberFormatException nfe) {
                ctx.sender().sendMessage(String.format("%sinvalid level value.", ChatColor.RED));
                return true;
            }
            PlayerInfo inf = PlayerInfo.get(plID);
            inf.breaks = 0;
            inf.lvl = setlvl;
            if (Oneblock.settings().progress_bar && offpl instanceof Player) {
                inf.createBar(Oneblock.getBarTitle((Player) offpl, inf.lvl));
                inf.bar.setProgress(inf.getPercent());
            }
            ctx.sender().sendMessage(String.format("%sfor player %s, level %s is set.", ChatColor.GREEN, args[1], args[2]));
            return true;
        }
        ctx.sender().sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
        return true;
    }
}
