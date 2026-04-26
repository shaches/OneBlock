package oneblock;

import static oneblock.Oneblock.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;
import oneblock.invitation.Guest;
import oneblock.invitation.Invitation;
import oneblock.utils.Utils;
import oneblock.worldguard.OBWorldGuard;

public class CommandHandler implements CommandExecutor {

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
	}

	public static boolean idresetCommand(OfflinePlayer pl) {
		if (pl == null) return false;
		UUID uuid = pl.getUniqueId();
		int PlId = PlayerInfo.GetId(uuid);
		if (PlId == -1) return false;
		PlayerInfo plp = PlayerInfo.get(PlId);
		plp.removeBar(pl);
		plp.removeUUID(uuid);

		if (!settings().saveplayerinventory && pl instanceof Player) ((Player) pl).getInventory().clear();

		if (OBWorldGuard.isEnabled())
			plugin.OBWG.removeMember(uuid, PlId);

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
            return routed.execute(new CommandContext(sender, player, args, plugin));
        }

        switch (parametr) 
        {
	        case ("gui"): {
	        	if (args.length == 1) {
	        		GUI.openGUI(player);
	        		return true;
	        	}
	        	// Fall-through contract for the next three cases ("gui", "idreset", default):
	        	//   single-arg -> user-facing behaviour (open GUI / self-idreset), returns.
	        	//   multi-arg  -> intentional fall-through to the admin `default` block,
	        	//                 which performs the Oneblock.set permission check and
	        	//                 re-dispatches to the admin switch's matching case.
	        	// DO NOT insert new cases in between without preserving the chain, or
	        	// `/ob gui true|false` and `/ob idreset <name>` will silently stop
	        	// reaching the admin handler.
	        	// fall through: `/ob gui true|false` reaches the admin bool-toggle via default.
	        }
	        case ("idreset"):{
	        	if (args.length == 1) {
	        		if (player == null) return false;
		        	if (!requirePermission(sender, "Oneblock.idreset")) return true;
		        	if (!idresetCommand(player)) return true;
		        	sender.sendMessage(Messages.idreset);
		        	player.performCommand("ob leave /n");
		        	return true; 
	        	}
	        	// fall through: `/ob idreset <name>` reaches the admin idreset via default.
	        }
	        default: {//admin commands
	        	if (requirePermission(sender, "Oneblock.set")) 
		        {
	        		final java.io.File _cfgFile = plugin.configManager.getMainConfigFile();
	        		config = YamlConfiguration.loadConfiguration(_cfgFile); // Loading the config.yml file before making changes.
	        		Bukkit.getScheduler().runTaskLater(plugin, () -> { LegacyConfigSaver.save(config, _cfgFile); }, 2L); // Saving the config.yml file after making changes.
		        	switch (parametr) {
			        	case ("set"): {
			        		if (player == null && args.length < 6) {
			        	        sender.sendMessage(ChatColor.RED + "Usage from console: /ob set <offset> <x> <y> <z> [world]");
			        	        return true;
			        	    }
			        	    
			        	    if (args.length >= 2) {
			        	        try {
			        	            int off_set = Integer.parseInt(args[1]);
			        	            if (off_set == 0 || off_set > 10000 || off_set < -10000) throw new NumberFormatException();
			        	            plugin.setOffset(off_set);
			        	        } catch (NumberFormatException nfe) {
			        	            sender.sendMessage(Messages.invalid_value);
			        	            return true;
			        	        }
			        	    }
			        	    
			        	    Location location;
			        	    if (args.length >= 5) {
			        	        try {
			        	            int x = Integer.parseInt(args[2]);
			        	            int y = Integer.parseInt(args[3]);
			        	            int z = Integer.parseInt(args[4]);
			        	            
			        	            World world = args.length >= 6 ? Bukkit.getWorld(args[5]) : 
			        	                         (player != null ? player.getWorld() : null);
			        	            
			        	            if (world == null) {
			        	                sender.sendMessage(ChatColor.YELLOW + "World not found!");
			        	                return true;
			        	            }
			        	            
			        	            location = new Location(world, x, y, z);
			        	        } catch (NumberFormatException nfe) {
			        	            sender.sendMessage(Messages.invalid_value);
			        	            return true;
			        	        }
			        	    } else if (player != null) {
			        	    	location = player.getLocation();
			        	    } else {
			        	    	sender.sendMessage(ChatColor.RED + "Usage from console: /ob set <offset> <x> <y> <z> [world]");
			        	    	return true;
			        	    }
			        	    
			        	    // plugin.setOffset above already persisted `set` to config; no explicit set needed here.
			        	    plugin.setPosition(location);
			        	    
			        	    if (!plugin.enabled) plugin.runMainTask();
			        	    
			        	    getWorld().getBlockAt(getX(), getY(), getZ()).setType(GRASS_BLOCK.get());
			        	    plugin.OBWG.ReCreateRegions();
			        	    LegacyConfigSaver.save(config, plugin.configManager.getMainConfigFile());
			        	    
			        	    sender.sendMessage(ChatColor.GREEN + "set OneBlock on: \n" +
			        	                      ChatColor.WHITE + getX() + ", " + getY() + ", " + getZ() +
			        	                      ChatColor.GRAY + " in world " + ChatColor.WHITE + getWorld().getName());
			        	    return true;
			        	}
			            case ("setleave"):{
			            	if (player == null) return false;
			                plugin.setLeave(player.getLocation());
			                return true;
			            }
			            case ("worldguard"):{
			            	if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")){
			                    sender.sendMessage(String.format("%sThe WorldGuard plugin was not detected!", ChatColor.YELLOW));
			                    return true;
			                }
			            	if (!OBWorldGuard.canUse) {
			                    sender.sendMessage(String.format("%sThis feature is only available in the premium version of the plugin!", ChatColor.YELLOW));
			                    return true;
			                }
			            	if (args.length > 1 &&
			                	(args[1].equals("true") || args[1].equals("false"))) {
			            			OBWorldGuard.setEnabled(Boolean.valueOf(args[1]));
			                    	config.set("WorldGuard", OBWorldGuard.isEnabled());
			                    	if (OBWorldGuard.isEnabled())
			                    		plugin.OBWG.ReCreateRegions();
			                    	else
			                    		plugin.OBWG.RemoveRegions(PlayerInfo.size());
			                }
			                else sender.sendMessage(Messages.bool_format);
			            	sender.sendMessage(String.format("%sthe OBWorldGuard is now %s", ChatColor.GREEN, (OBWorldGuard.isEnabled()?"enabled.":"disabled.")));
			           		return true;
			            }
			            case ("border"):
			            	if (!isBorderSupported){
			                    sender.sendMessage(String.format("%sThe border can only be used on version 1.18.2 and above!", ChatColor.YELLOW));
			                    return true;
			                }
			            	// ReloadBorders is scheduled 2 ticks later so the fall-through
			            	// below can first persist `border=true/false` via the shared
			            	// bool-toggle body; the ordering is load-bearing.
			            	Bukkit.getScheduler().runTaskLater(plugin, () -> { plugin.ReloadBorders(); }, 2L);
			            	// fall through to shared bool-toggle body:
			            case ("circlemode"):
			            case ("useemptyislands"):
			            case ("protection"):
			            case ("droptossup"):
			            case ("physics"):
			            case ("autojoin"):
			            case ("particle"):
			            case ("allow_nether"):
			            case ("saveplayerinventory"):
			            case ("gui"):
			            case ("rebirth_on_the_island"):{
			            	if (args.length > 1 &&
			                    	(args[1].equals("true") || args[1].equals("false"))) {
			                    	config.set(parametr, Boolean.valueOf(args[1]));
			                    	configManager.updateBoolParameters();
			                }
			                else sender.sendMessage(Messages.bool_format);
			                sender.sendMessage(String.format("%s%s is now %s", ChatColor.GREEN, parametr, (config.getBoolean(parametr)?"enabled.":"disabled.")));
			           		return true;
			            }
			            case ("setlevel"):{
			                if (args.length <= 2) {
			                    sender.sendMessage(String.format("%sinvalid format. try: /ob setlevel 'nickname' 'level'", ChatColor.RED));
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
			                    } 
			                    catch (NumberFormatException nfe) {
			                    	sender.sendMessage(String.format("%sinvalid level value.", ChatColor.RED));
			                    	return true;
			                    }
			                    PlayerInfo inf = PlayerInfo.get(plID);
		                        inf.breaks = 0;
		                        inf.lvl = setlvl;
		                        if (settings().progress_bar && offpl instanceof Player) {
		                        	inf.createBar(getBarTitle((Player) offpl, inf.lvl));
	                                inf.bar.setProgress(inf.getPercent());
	                            }
		                        sender.sendMessage(String.format("%sfor player %s, level %s is set.", ChatColor.GREEN, args[1], args[2]));
		                        return true;
			                }
			                sender.sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
			                return true;
			            }
			            case ("idreset"):{
			            	if (args.length <= 1) return true;
			            	OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
			            	if (idresetCommand(offpl))
		                    	sender.sendMessage(String.format("%splayer %s id is reseted! :D", ChatColor.GREEN, args[1]));
		                    else
		                    	sender.sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
		                    return true;
			            }
			            case ("clear"):{
			                if (args.length <= 1) {
			                    sender.sendMessage(String.format("%sinvalid format. try: /ob clear 'nickname'", ChatColor.RED));
			                    return true;
			                }
			                OfflinePlayer offpl = Utils.getOfflinePlayerByName(args[1]);
			                if (offpl == null) {
			                	sender.sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
				                return true;
			                }
			                UUID uuid = offpl.getUniqueId();
			                int id = PlayerInfo.GetId(uuid);
			                if (id == -1) {
			                	sender.sendMessage(String.format("%sa player named %s was not found.", ChatColor.RED, args[1]));
				                return true;
			                }
			                PlayerInfo inf = PlayerInfo.get(id);
		                    inf.breaks = 0;
		                    inf.lvl = 0;
		                    if (settings().progress_bar)
		                    	inf.bar.setVisible(false);
		                    int result[] = plugin.getIslandCoordinates(id);
		                    Island.clear(getWorld(), result[0], getY(), result[1], getOffset()/4);
		                    sender.sendMessage(String.format("%splayer %s island is destroyed! :D", ChatColor.GREEN, args[1]));
		                    return true;
			            }
			            case ("lvl_mult"): {
			                if (args.length > 1) {
				                try {
				                    int lvl = Integer.parseInt(args[1]);
				                    if (lvl < 0 || lvl > 20) throw new NumberFormatException();
				                    config.set("level_multiplier", Level.multiplier = lvl);
				                    configManager.Blockfile();
				                } 
				                catch (NumberFormatException nfe) {
				                    sender.sendMessage(String.format("%sinvalid multiplier value. Possible values: from 0 to 20.", ChatColor.RED));
				                    return true;
				                }
			                }
			                sender.sendMessage(String.format("%slevel multiplier now: %d\n5 by default", ChatColor.GREEN, Level.multiplier));
			                return true;
			            }
			            case ("max_players_team"): {
			                if (args.length > 1) {
				                try {
				                    int mpt = Integer.parseInt(args[1]);
				                    if (mpt < 0 || mpt > 20) throw new NumberFormatException();
				                    config.set("max_players_team", settings().max_players_team = mpt);
				                } 
				                catch (NumberFormatException nfe) {
				                    sender.sendMessage(String.format("%sinvalid max_players_team value. Possible values: from 0 to 20.", ChatColor.RED));
				                    return true;
				                }
			                }
			                sender.sendMessage(String.format("%smax_players_team now: %d\n0 is unlimited", ChatColor.GREEN, settings().max_players_team));
			                return true;
			            }
			            case ("progress_bar"):{
			                if (superlegacy) {
			                    sender.sendMessage(String.format("%sYou server version is super legacy! ProgressBar unsupported!", ChatColor.RED));
			                    return true;
			                }
			                if (args.length == 1) {
			                    sender.sendMessage(String.format("%sand?", ChatColor.YELLOW));
			                    return true;
			                }
			                if (args[1].equals("true") || args[1].equals("false")) {
			                    settings().progress_bar = Boolean.valueOf(args[1]);
			                    configManager.Blockfile();
			                    config.set("progress_bar", settings().progress_bar);
			                    return true;
			                }
			                
			                if (!settings().progress_bar) return true;
			                
			                boolean isColor = args[1].equalsIgnoreCase("color");
			                if (isColor || args[1].equalsIgnoreCase("style")) {
			                    if (args.length == 2) {
			                        sender.sendMessage(String.format("%senter a %s name.", ChatColor.YELLOW, args[1].toLowerCase()));
			                        return true;
			                    }
			                    
			                    try {
			                        if (isColor) {
			                            Level.max.color = BarColor.valueOf(args[2]);
			                            config.set("progress_bar_color", Level.max.color.toString());
			                        } else {
			                            Level.max.style = BarStyle.valueOf(args[2]);
			                            config.set("progress_bar_style", Level.max.style.toString());
			                        }
			                        configManager.Blockfile();
			                        sender.sendMessage(String.format("%sProgress bar %s = %s", ChatColor.GREEN, args[1].toLowerCase(), args[2]));
			                    } 
			                    catch (Exception e) {
			                        sender.sendMessage(String.format("%sPlease enter a valid %s. For example: %s", ChatColor.YELLOW, args[1].toLowerCase(), isColor ? "RED" : "SOLID"));
			                    }
			                    return true;
			                }
			                if (args[1].equalsIgnoreCase("level")) {
			                	settings().lvl_bar_mode = true;
			                    config.set("progress_bar_text", "level");
			                    configManager.SetupProgressBar();
			                    return true;
			                }
			                if (args[1].equalsIgnoreCase("settext")) {
			                    String txt_bar = "";
								for (int i = 2; i < args.length; i++)
									txt_bar = i == 2 ? args[i] : String.format("%s %s", txt_bar, args[i]);
			                    settings().lvl_bar_mode = false;
			                    config.set("progress_bar_text", settings().phText = Utils.translateColorCodes(txt_bar));
			                    configManager.SetupProgressBar();
			                    return true;
			                }
			                sender.sendMessage(String.format("%strue, false, settext or level only!", ChatColor.RED));
			                return true;
			            }
			            case ("listlvl"):{
			                if (args.length > 1) {
			                	int temp = 0;
			                    try {
			                    	temp = Integer.parseInt(args[1]);
			                    	if (temp < 0 || temp >= Level.size()) throw new NumberFormatException();
			                    } 
			                    catch (NumberFormatException nfe) {
			                    	sender.sendMessage(String.format("%sundefined lvl", ChatColor.RED));
			                    	return true;
			                    }
			                    Level lvl = Level.get(temp);
			                    sender.sendMessage(String.format("%s%s %s(weight total: %d)", ChatColor.GREEN, lvl.name, ChatColor.GRAY, lvl.blockPool.totalWeight() + lvl.mobPool.totalWeight()));
			                    for (WeightedPool.Entry<PoolEntry> e : lvl.blockPool.entries())
			                    	sender.sendMessage("  " + e.value + " (weight " + e.weight + ")");
			                    for (WeightedPool.Entry<EntityType> e : lvl.mobPool.entries())
			                    	sender.sendMessage("  mob: " + e.value + " (weight " + e.weight + ")");
			                    return true;
			                }
			                for(int i = 0;i<Level.size();i++)
			                	sender.sendMessage(String.format("%d: %s%s", i, ChatColor.GREEN, Level.get(i).name));
			                return true;
			            }
			            case ("reload"):{
			            	sender.sendMessage(String.format("%sReloading Plugin & Plugin Modules.", ChatColor.YELLOW));
			            	plugin.reload();
			            	sender.sendMessage(String.format("%sAll *.yml reloaded!", ChatColor.GREEN));
			            	return true;
			            }
			            case ("islands"):{
			                if (args.length == 1) {
			                    sender.sendMessage(Messages.bool_format);
			                    return true;
			                }
			                if (args[1].equals("true") || args[1].equals("false")) {
			                    settings().island_for_new_players = Boolean.valueOf(args[1]);
			                    config.set("island_for_new_players", settings().island_for_new_players);
			                    sender.sendMessage(ChatColor.GREEN + "Island_for_new_players = " + settings().island_for_new_players);
			                    return true;
			                }
			                if (args[1].equals("set_my_by_def")) {
			                	if (legacy) {
			                		sender.sendMessage(ChatColor.RED + "Not supported in legacy versions!");
			                		return true;
			                	}
			                	if (!(sender instanceof Player)) {
			                		sender.sendMessage(ChatColor.RED + "This subcommand can only be used by a player.");
			                		return true;
			                	}
			                	Player p = (Player) sender;
			                	UUID uuid = p.getUniqueId();
			                    if (PlayerInfo.GetId(uuid) != -1) {
			                        int result[] = plugin.getIslandCoordinates(PlayerInfo.GetId(uuid));
			                        Island.scan(getWorld(), result[0], getY(), result[1]);
			                        sender.sendMessage(ChatColor.GREEN + "A copy of your island has been successfully saved!");
			                        config.set("custom_island", Island.map());
			                    } else
			                        sender.sendMessage(ChatColor.RED + "You don't have an island!");
			                    return true;
			                }
			                if (args[1].equalsIgnoreCase("default")) {
			                	if (legacy) {
			                		sender.sendMessage(ChatColor.RED + "Not supported in legacy versions!");
			                		return true;
			                	}
			                    config.set("custom_island", Island.custom = null);
			                    sender.sendMessage(ChatColor.GREEN + "The default island is installed.");
			                    return true;
			                }
			                sender.sendMessage(Messages.bool_format);
			                return true;
			            }
			            case ("chest"):{
			            	if (args.length < 2) {
			            		if (ChestItems.getChestNames().isEmpty()) {
			            			sender.sendMessage(ChatColor.YELLOW + "No chest aliases configured. Define them in chests.yml as 'name: minecraft:chests/<loot_table>'.");
			            			return true;
			            		}
			            		for (String name : ChestItems.getChestNames()) {
			            			NamespacedKey k = ChestItems.resolve(name);
			            			sender.sendMessage(ChatColor.GREEN + name + ChatColor.GRAY + " -> " + ChatColor.WHITE + (k == null ? "<unset>" : k));
			            		}
			            		return true;
			            	}
			            	String chestName = args[1];
			            	if (args.length < 3) {
			            		NamespacedKey current = ChestItems.resolve(chestName);
			            		if (current == null)
			            			sender.sendMessage(ChatColor.YELLOW + "No loot-table mapping for '" + chestName + "'. Usage: /ob chest " + chestName + " set <namespaced_key>");
			            		else {
			            			sender.sendMessage(ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + current);
			            			sender.sendMessage(ChatColor.GRAY + "Usage: /ob chest " + chestName + " set <namespaced_key>");
			            		}
			            		return true;
			            	}
			            	if (!args[2].equalsIgnoreCase("set") || args.length < 4) {
			            		sender.sendMessage(ChatColor.RED + "Usage: /ob chest <name> [set <namespaced_key>]");
			            		return true;
			            	}
			            	NamespacedKey newKey = ChestItems.parseKey(args[3]);
			            	if (newKey == null) {
			            		sender.sendMessage(ChatColor.RED + "Invalid namespaced key '" + args[3] + "'.");
			            		return true;
			            	}
			            	ChestItems.setAlias(chestName, newKey);
			            	ChestItems.save();
			            	sender.sendMessage(ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + newKey);
			            	return true;
			            }
			        }
	        	}
	        	
	        	sender.sendMessage(
	        		    ChatColor.values()[rnd.nextInt(ChatColor.values().length)] + 
	        		    "\n▄▀▄ ██▄" +
	        		    "\n▀▄▀ █▄█  by MrMarL" +
	        		    "\nPlugin version: v" + plugin.version +
	        		    "\nServer version: " + (superlegacy ? "super legacy " : (legacy ? "legacy " : "")) + XMaterial.getVersionMajor() + "." + XMaterial.getVersionMinor() + ".X");
    		     return true;
		    }
	    }
    }
}