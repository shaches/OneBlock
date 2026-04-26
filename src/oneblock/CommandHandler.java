package oneblock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;

import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.worldguard.OBWorldGuard;

public final class CommandHandler implements CommandExecutor {

	/**
	 * Phase 3.5a router registry. Each {@link Subcommand} is registered
	 * under its primary {@link Subcommand#name()}; aliases (e.g.
	 * {@code j} -> {@code join}) reuse the same instance under the alias
	 * key. Phase 3.5a leaves this map empty so every dispatch still falls
	 * through to the legacy {@code switch} below; the next sub-slice
	 * (3.5b) populates it as subcommands are extracted into
	 * {@code oneblock.command.sub.*}. Once the registry covers every
	 * {@code args[0]}, the legacy switch shrinks to just the
	 * version-info fallback.
	 */
	private static final Map<String, Subcommand> SUBCOMMANDS = new HashMap<>();

	static void register(Subcommand sub, String... aliases) {
		SUBCOMMANDS.put(sub.name(), sub);
		for (String alias : aliases) SUBCOMMANDS.put(alias, sub);
	}

	static {
		// Phase 3.5b - Batch 1: trivial user-facing commands. Each impl
		// is stateless; the registry holds a shared singleton. Aliases
		// (e.g. command-name == primary registration name) are listed in
		// the trailing varargs.
		register(new oneblock.command.sub.AcceptCommand());
		register(new oneblock.command.sub.TopCommand());
		register(new oneblock.command.sub.HelpCommand());
		register(new oneblock.command.sub.LeaveCommand());
		register(new oneblock.command.sub.AllowVisitCommand());
		// Phase 3.5b - Batch 2: complex user-facing commands. JoinCommand
		// owns the `j` alias too; VisitCommand owns `v`; the rest are
		// single-name.
		register(new oneblock.command.sub.JoinCommand(), "j");
		register(new oneblock.command.sub.VisitCommand(), "v");
		register(new oneblock.command.sub.InviteCommand());
		register(new oneblock.command.sub.KickCommand());
		// Phase 3.5b - Batch 3: simple admin commands. Each runs the
		// AdminPrelude (config reload + 2-tick delayed save) before its
		// own logic. IdResetCommand internally delegates to
		// AdminIdResetCommand for the multi-arg admin path.
		register(new oneblock.command.sub.SetLeaveCommand());
		register(new oneblock.command.sub.WorldGuardCommand());
		register(new oneblock.command.sub.ReloadCommand());
		register(new oneblock.command.sub.SetLevelCommand());
		register(new oneblock.command.sub.ClearCommand());
		register(new oneblock.command.sub.LvlMultCommand());
		register(new oneblock.command.sub.MaxPlayersTeamCommand());
		register(new oneblock.command.sub.ListLvlCommand());
		register(new oneblock.command.sub.IdResetCommand());
		// Phase 3.5b - Batch 4: remaining admin commands. The 11 boolean
		// flags share BoolToggleCommand; `border` and `gui` wrap it with
		// extra side-effects. SetCommand owns the big position+offset
		// setup; ProgressBarCommand has its own sub-subcommand tree.
		register(new oneblock.command.sub.SetCommand());
		register(new oneblock.command.sub.ProgressBarCommand());
		register(new oneblock.command.sub.IslandsCommand());
		register(new oneblock.command.sub.ChestCommand());
		register(new oneblock.command.sub.BorderCommand());
		register(new oneblock.command.sub.GuiCommand());
		// 11 admin bool-toggles backed by the shared impl.
		register(new oneblock.command.sub.BoolToggleCommand("circlemode"));
		register(new oneblock.command.sub.BoolToggleCommand("useemptyislands"));
		register(new oneblock.command.sub.BoolToggleCommand("protection"));
		register(new oneblock.command.sub.BoolToggleCommand("droptossup"));
		register(new oneblock.command.sub.BoolToggleCommand("physics"));
		register(new oneblock.command.sub.BoolToggleCommand("autojoin"));
		register(new oneblock.command.sub.BoolToggleCommand("particle"));
		register(new oneblock.command.sub.BoolToggleCommand("allow_nether"));
		register(new oneblock.command.sub.BoolToggleCommand("saveplayerinventory"));
		register(new oneblock.command.sub.BoolToggleCommand("rebirth_on_the_island"));
	}

	public static boolean idresetCommand(OfflinePlayer pl) {
		if (pl == null) return false;
		UUID uuid = pl.getUniqueId();
		int PlId = PlayerInfo.getId(uuid);
		if (PlId == -1) return false;
		PlayerInfo plp = PlayerInfo.get(PlId);
		plp.removeBar(pl);
		plp.removeUUID(uuid);

		if (!Oneblock.settings().saveplayerinventory && pl instanceof Player) ((Player) pl).getInventory().clear();

		if (OBWorldGuard.isEnabled())
			Oneblock.plugin.worldGuard.removeMember(uuid, PlId);

		return true;
	}
	
	private boolean requirePermission(CommandSender sender, String permission) {
	    if (!sender.hasPermission(permission)) {
	        sender.sendMessage(ChatColor.RED + "You don't have permission [" + permission + "].");
	        return false;
	    }
	    return true;
	}
	
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (!cmd.getName().equalsIgnoreCase("oneblock")) return false;
        if (!requirePermission(sender, "Oneblock.join")) return true;
        if (args.length == 0) args = new String[] {"j"};
        
        Player player = sender instanceof Player ? (Player) sender : null;
        
        String parametr = args[0].toLowerCase();

        // Phase 3.5a router-fallback: any subcommand registered in
        // SUBCOMMANDS gets dispatched through the new Subcommand interface.
        // Anything else flows through to the legacy switch below.
        Subcommand routed = SUBCOMMANDS.get(parametr);
        if (routed != null) {
            if (routed.requiresPlayer() && player == null) return false;
            String perm = routed.permission();
            if (perm != null && !requirePermission(sender, perm)) return true;
            return routed.execute(new CommandContext(sender, player, args, Oneblock.plugin));
        }
        // Phase 3.5b.5: every recognised subcommand is registered in
        // SUBCOMMANDS above and dispatched by the router-fallback block.
        // The legacy 649-line switch was removed; reaching here means
        // the user typed an unknown subcommand. Reply with a friendly
        // version-info splash instead of a blunt usage error so admins
        // can confirm they are talking to the right plugin build.
        sender.sendMessage(
                ChatColor.values()[Oneblock.rnd.nextInt(ChatColor.values().length)] +
                "\n▄▀▄ ██▄" +
                "\n▀▄▀ █▄█  by MrMarL" +
                "\nPlugin version: v" + Oneblock.plugin.version +
                "\nServer version: " + (Oneblock.superlegacy ? "super legacy " : (Oneblock.legacy ? "legacy " : "")) + XMaterial.getVersionMajor() + "." + XMaterial.getVersionMinor() + ".X");
        return true;
    }
}