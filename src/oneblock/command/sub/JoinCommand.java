package oneblock.command.sub;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import oneblock.Island;
import oneblock.Oneblock;
import oneblock.utils.Compat;
import oneblock.PlayerInfo;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.worldguard.OBWorldGuard;

/**
 * {@code /ob j} (alias) and {@code /ob join} - user-facing. Either
 * teleports the sender to their existing island, or - on first call -
 * creates a fresh island, registers it with WorldGuard if enabled,
 * places the welcome layout, kicks off the BossBar, and only then
 * teleports.
 *
 * <p>Behaviour-equivalent to the legacy {@code "j"}/{@code "join"}
 * switch case extracted in Phase 3.5b. Refuses politely (and prompts
 * the admin to run {@code /ob set} first) when {@code offset} is zero
 * or the configured world has not yet loaded. Requires a
 * {@link org.bukkit.entity.Player} sender; console has no location to
 * teleport.
 */
public final class JoinCommand implements Subcommand {
    @Override public String name() { return "join"; }
    @Override public boolean requiresPlayer() { return true; }

    @Override
    public boolean execute(CommandContext ctx) {
        if (Oneblock.getOffset() == 0 || Oneblock.getWorld() == null) {
            ctx.sender().sendMessage(ChatColor.YELLOW + "First you need to set the reference coordinates '/ob set'.");
            return true;
        }
        Oneblock plugin = ctx.plugin();
        UUID uuid = ctx.player().getUniqueId();
        int X_pl = 0, Z_pl = 0;
        int plID = PlayerInfo.getId(uuid);
        if (plID == -1) {
            PlayerInfo inf = new PlayerInfo(uuid);
            plID = PlayerInfo.getFreeId(Oneblock.settings().useEmptyIslands);
            int result[] = plugin.getIslandCoordinates(plID);
            X_pl = result[0]; Z_pl = result[1];
            if (plID != PlayerInfo.size())
                Island.clear(Oneblock.getWorld(), X_pl, Oneblock.getY(), Z_pl, Oneblock.getOffset() / 4);
            Island.place(Oneblock.getWorld(), X_pl, Oneblock.getY(), Z_pl);
            plugin.worldGuard.createRegion(uuid, X_pl, Z_pl, Oneblock.getOffset(), plID);
            PlayerInfo.set(plID, inf);
            if (!Compat.superlegacy)
                inf.createBar(Oneblock.getBarTitle(ctx.player(), 0));
        } else {
            int result[] = plugin.getIslandCoordinates(plID);
            X_pl = result[0]; Z_pl = result[1];
        }
        if (!plugin.enabled) plugin.runMainTask();
        if (Oneblock.settings().progressBar) PlayerInfo.get(plID).bar.setVisible(true);
        ctx.player().teleport(new Location(Oneblock.getWorld(), X_pl + 0.5, Oneblock.getY() + 1.2013, Z_pl + 0.5));
        if (OBWorldGuard.isEnabled()) plugin.worldGuard.addMember(uuid, plID);
        return true;
    }
}
