package oneblock;

<<<<<<< HEAD
import static oneblock.Oneblock.*;

import java.io.File;
=======
import java.io.File;
import java.util.ArrayList;
>>>>>>> origin/main
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
<<<<<<< HEAD
=======
import org.bukkit.NamespacedKey;
>>>>>>> origin/main
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;
import com.nexomc.nexo.api.NexoBlocks;

import oneblock.gui.GUI;
import oneblock.migration.LegacyBlocksMigrator;
<<<<<<< HEAD
import oneblock.pldata.DatabaseManager;
=======
import oneblock.storage.DatabaseManager;
>>>>>>> origin/main
import oneblock.utils.LowerCaseYaml;
import oneblock.utils.Utils;
import oneblock.worldguard.OBWorldGuard;
import dev.lone.itemsadder.api.CustomBlock;
import io.th0rgal.oraxen.api.OraxenItems;
<<<<<<< HEAD
import net.momirealms.craftengine.core.util.Key;

public class ConfigManager {
=======

public final class ConfigManager {
>>>>>>> origin/main
	public YamlConfiguration config_temp;
	public RewardManager reward = new RewardManager();
	
    public void loadConfigFiles() {
<<<<<<< HEAD
        Configfile();
=======
        loadMainConfig();
>>>>>>> origin/main
        loadAdditionalConfigFiles();
    }
    
    public void loadAdditionalConfigFiles() {
<<<<<<< HEAD
    	Chestfile();
        Blockfile();
        Flowerfile();
        Messagefile();
        reward.loadRewards();
    }
	
    public void Configfile() {
    	File con = getFile("config.yml");
        config = LowerCaseYaml.loadAndFixConfig(con);
        
        plugin.setPosition(
        		Bukkit.getWorld(Check("world", "world")),
        		(int)Check("x", (double) getX()), 
        		(int)Check("y", (double) getY()), 
        		(int)Check("z", (double) getZ()));
        
        plugin.setLeave(
        		Bukkit.getWorld(Check("leaveworld", "world")), 
        		Check("xleave", .0), 
        		Check("yleave", .0), 
        		Check("zleave", .0), 
        		(float)Check("yawleave", .0));
        
        if (!superlegacy) {
        	progress_bar = Check("progress_bar", true);
        	Level.max.color = BarColor.valueOf(Check("progress_bar_color", "GREEN"));
        	Level.max.style = BarStyle.valueOf(Check("progress_bar_style", "SOLID"));
        	phText = Utils.translateColorCodes(Check("progress_bar_text", "level"));
	        lvl_bar_mode = phText.equals("level");
        }
        island_for_new_players = Check("island_for_new_players", true);
        Level.multiplier = Check("level_multiplier", Level.multiplier);
        max_players_team = Check("max_players_team", max_players_team);
        mob_spawn_chance = Check("mob_spawn_chance", mob_spawn_chance);
        mob_spawn_chance = mob_spawn_chance < 2 ? 9 : mob_spawn_chance;
        updateBoolParameters();
        OBWorldGuard.setEnabled(Check("worldguard", OBWorldGuard.canUse));
        OBWorldGuard.flags = Check("wgflags", OBWorldGuard.flags);
        plugin.setOffset(Check("set", 100));
        if (config.isSet("custom_island") && !legacy)
        	Island.read(config);
        
        DatabaseConfig();
        
        LegacyConfigSaver.Save(config, con);
    }
	 
    public void updateBoolParameters() {
    	CircleMode = Check("circlemode", CircleMode);
    	UseEmptyIslands = Check("useemptyislands", UseEmptyIslands);
    	saveplayerinventory = Check("saveplayerinventory", saveplayerinventory);
        protection = Check("protection", protection);
        autojoin = Check("autojoin", autojoin);
        droptossup = Check("droptossup", droptossup);
        physics = Check("physics", physics);
        particle = Check("particle", particle);
        allow_nether = Check("allow_nether", allow_nether);
        GUI.enabled = Check("gui", GUI.enabled);
        rebirth = Check("rebirth_on_the_island", rebirth);
        if (isBorderSupported) border = Check("border", border);
    }
    
    private void DatabaseConfig() {
        DatabaseManager.dbType = Check("database.type", DatabaseManager.dbType).toLowerCase();
        DatabaseManager.host = Check("database.host", DatabaseManager.host);
        DatabaseManager.port = Check("database.port", DatabaseManager.port);
        DatabaseManager.database = Check("database.name", DatabaseManager.database);
        DatabaseManager.username = Check("database.username",  DatabaseManager.username);
        DatabaseManager.password = Check("database.password", DatabaseManager.password);
        DatabaseManager.useSSL = Check("database.useSSL", DatabaseManager.useSSL);
        DatabaseManager.autoReconnect = Check("database.autoReconnect", DatabaseManager.autoReconnect);
    }
        
	public void Blockfile() {
    	Level.levels.clear();
    	PoolRegistry.clear();
=======
    	loadChests();
        loadBlocks();
        loadFlowers();
        loadMessages();
        reward.loadRewards();
    }
	
    public void loadMainConfig() {
    	File con = getFile("config.yml");
        Oneblock.config = LowerCaseYaml.loadAndFixConfig(con);
        
        Oneblock.plugin.setPosition(
        		Bukkit.getWorld(readOrDefault("world", "world")),
        		(int)readOrDefault("x", (double) Oneblock.getX()), 
        		(int)readOrDefault("y", (double) Oneblock.getY()), 
        		(int)readOrDefault("z", (double) Oneblock.getZ()));
        
        Oneblock.plugin.setLeave(
        		Bukkit.getWorld(readOrDefault("leaveworld", "world")), 
        		readOrDefault("xleave", .0), 
        		readOrDefault("yleave", .0), 
        		readOrDefault("zleave", .0), 
        		(float)readOrDefault("yawleave", .0));
        
        // Single-call cache so we don't dereference Oneblock.settings() once per field.
        oneblock.config.Settings s = Oneblock.settings();
        if (!Oneblock.superlegacy) {
        	s.progress_bar = readOrDefault("progress_bar", true);
        	Level.max.color = BarColor.valueOf(readOrDefault("progress_bar_color", "GREEN"));
        	Level.max.style = BarStyle.valueOf(readOrDefault("progress_bar_style", "SOLID"));
        	s.phText = Utils.translateColorCodes(readOrDefault("progress_bar_text", "level"));
	        s.lvl_bar_mode = s.phText.equals("level");
        }
        s.island_for_new_players = readOrDefault("island_for_new_players", true);
        Level.multiplier = readOrDefault("level_multiplier", Level.multiplier);
        s.max_players_team = readOrDefault("max_players_team", s.max_players_team);
        s.mob_spawn_chance = readOrDefault("mob_spawn_chance", s.mob_spawn_chance);
        s.mob_spawn_chance = s.mob_spawn_chance < 2 ? 9 : s.mob_spawn_chance;
        updateBoolParameters();
        OBWorldGuard.setEnabled(readOrDefault("worldguard", OBWorldGuard.canUse));
        OBWorldGuard.flags = readOrDefault("wgflags", OBWorldGuard.flags);
        Oneblock.plugin.setOffset(readOrDefault("set", 100));
        if (Oneblock.config.isSet("custom_island") && !Oneblock.legacy)
        	Island.read(Oneblock.config);
        
        DatabaseConfig();
        
        LegacyConfigSaver.save(Oneblock.config, con);
    }
	 
    public void updateBoolParameters() {
    	oneblock.config.Settings s = Oneblock.settings();
    	s.circleMode = readOrDefault("circlemode", s.circleMode);
    	s.useEmptyIslands = readOrDefault("useemptyislands", s.useEmptyIslands);
    	s.saveplayerinventory = readOrDefault("saveplayerinventory", s.saveplayerinventory);
        s.protection = readOrDefault("protection", s.protection);
        s.autojoin = readOrDefault("autojoin", s.autojoin);
        s.droptossup = readOrDefault("droptossup", s.droptossup);
        s.physics = readOrDefault("physics", s.physics);
        s.particle = readOrDefault("particle", s.particle);
        s.allow_nether = readOrDefault("allow_nether", s.allow_nether);
        GUI.enabled = readOrDefault("gui", GUI.enabled);
        s.rebirth = readOrDefault("rebirth_on_the_island", s.rebirth);
        if (Oneblock.isBorderSupported) s.border = readOrDefault("border", s.border);
    }
    
    private void DatabaseConfig() {
        DatabaseManager.dbType = readOrDefault("database.type", DatabaseManager.dbType).toLowerCase();
        DatabaseManager.host = readOrDefault("database.host", DatabaseManager.host);
        DatabaseManager.port = readOrDefault("database.port", DatabaseManager.port);
        DatabaseManager.database = readOrDefault("database.name", DatabaseManager.database);
        DatabaseManager.username = readOrDefault("database.username",  DatabaseManager.username);
        DatabaseManager.password = readOrDefault("database.password", DatabaseManager.password);
        DatabaseManager.useSSL = readOrDefault("database.useSSL", DatabaseManager.useSSL);
        DatabaseManager.autoReconnect = readOrDefault("database.autoReconnect", DatabaseManager.autoReconnect);
    }
        
	public void loadBlocks() {
    	Level.max.resetPools();
>>>>>>> origin/main
        File block = getFile("blocks.yml");
        
        // Migrate legacy (cumulative-list) blocks.yml in-place before parsing.
        // Uses ChestItems.chest (already-loaded or legacy) to map old chest-alias
        // tokens to vanilla loot-table keys during flattening.
        LegacyBlocksMigrator.migrateBlocks(block, ChestItems.chest);
        
        config_temp = YamlConfiguration.loadConfiguration(block);
        
        // MaxLevel: may be scalar (name-only, legacy passthrough) or a full list entry.
<<<<<<< HEAD
        if (config_temp.isString("MaxLevel"))
        	Level.max.name = Utils.translateColorCodes(config_temp.getString("MaxLevel"));
        else if (config_temp.isList("MaxLevel"))
        	parseLevelFromList(config_temp.getList("MaxLevel"), Level.max);
        
=======
        // For the max parser we pass idx=1 because Level.max is never a member of
        // the published levels list - this preserves the legacy length-default of
        // 16 + 1*Level.multiplier that the pre-Phase-4.1 level.getId() fallback
        // produced for max.
        if (config_temp.isString("MaxLevel"))
        	Level.max.name = Utils.translateColorCodes(config_temp.getString("MaxLevel"));
        else if (config_temp.isList("MaxLevel"))
        	parseLevelFromList(config_temp.getList("MaxLevel"), Level.max, 1);
        
        // Phase 4.1: stage levels in a fresh local list and only publish via
        // Level.replaceAll once the parse loop completed without throwing. If a
        // parse step throws (rare - most YAML failures are tolerated with a
        // warning) the previous Level.levels stays visible to async readers.
        // parseLevelFromList receives the loop index `i` so its length-default
        // fallback no longer depends on level.getId() against the published
        // list (the staged level isn't a member of Level.levels yet).
        List<Level> stagedLevels = new ArrayList<>();
>>>>>>> origin/main
        for (int i = 0; config_temp.isList(String.format("%d", i)); i++) {
        	List<?> bl_temp = config_temp.getList(String.format("%d", i));
        	if (bl_temp == null || bl_temp.isEmpty()) continue;
        	Object first = bl_temp.get(0);
        	Level level = new Level(first instanceof String
        			? Utils.translateColorCodes((String) first)
        			: "Level " + i);
<<<<<<< HEAD
        	Level.levels.add(level);
        	parseLevelFromList(bl_temp, level);
        }
        
        if (PoolRegistry.totalMobs() == 0) 
        	plugin.getLogger().warning("Mobs are not set in the blocks.yml");
        
        Level.max.blocks = PoolRegistry.totalBlocks();
        Level.max.mobs = PoolRegistry.totalMobs();
        
        SetupProgressBar();
=======
        	stagedLevels.add(level);
        	parseLevelFromList(bl_temp, level, i);
        }
        
        if (Level.max.mobPoolSize() == 0) {
        	int totalMobs = 0;
        	for (Level lvl : stagedLevels) totalMobs += lvl.mobPoolSize();
        	if (totalMobs == 0)
        		Oneblock.plugin.getLogger().warning("Mobs are not set in the blocks.yml");
        }
        
        Level.replaceAll(stagedLevels);
        
        setupProgressBar();
>>>>>>> origin/main
    }
    
    /**
     * Parse a level list (header + pool entries) into the given {@link Level}.
     * Header positions 0..3 are name/color/style/length (best-effort, same semantics
     * as the legacy parser). Pool entries beyond the header may be plain strings
     * (legacy, weight=1) or maps with {@code block|mob|loot_table|command} + optional
     * {@code weight}. Unresolved / malformed entries are skipped with a warning.
     */
<<<<<<< HEAD
    private void parseLevelFromList(List<?> bl_temp, Level level) {
=======
    private void parseLevelFromList(List<?> bl_temp, Level level, int idx) {
>>>>>>> origin/main
    	if (bl_temp == null || bl_temp.isEmpty()) return;
    	int q = 0;
    	if (q < bl_temp.size() && bl_temp.get(q) instanceof String) {
    		level.name = Utils.translateColorCodes((String) bl_temp.get(q));
    		q++;
    	}
    	// Duck-type probe: the string at position q may be a BarColor, a BarStyle
    	// OR the next header field (length) OR the first pool entry. We attempt
    	// each shape in turn; on failure we DO NOT advance q, leaving the string
    	// for the next parser. This is legacy config compatibility, not a bug.
<<<<<<< HEAD
    	if (!superlegacy) {
    		level.color = Level.max.color;
    		if (q < bl_temp.size() && bl_temp.get(q) instanceof String) try {
    			level.color = BarColor.valueOf(((String) bl_temp.get(q)).toUpperCase());
    			q++;
    		} catch (Exception e) {}
    		level.style = Level.max.style;
    		if (q < bl_temp.size() && bl_temp.get(q) instanceof String)  try {
    			level.style = BarStyle.valueOf(((String) bl_temp.get(q)).toUpperCase());
    			q++;
    		} catch (Exception e) {}
=======
    	if (!Oneblock.superlegacy && q < bl_temp.size() && bl_temp.get(q) instanceof String) {
    		try {
    			level.color = BarColor.valueOf(((String) bl_temp.get(q)).toUpperCase());
    			q++;
    		} catch (Exception e) { level.color = Level.max.color; }
    		if (q < bl_temp.size() && bl_temp.get(q) instanceof String) {
    			try {
    				level.style = BarStyle.valueOf(((String) bl_temp.get(q)).toUpperCase());
    				q++;
    			} catch (Exception e) { level.style = Level.max.style; }
    		}
>>>>>>> origin/main
    	}
    	if (q < bl_temp.size()) {
    		Object lenItem = bl_temp.get(q);
    		if (lenItem instanceof Number) {
    			level.length = Math.max(1, ((Number) lenItem).intValue());
    			q++;
    		} else if (lenItem instanceof String) {
    			// Duck-type probe (see above): if the string can't be parsed as an
    			// int, it's the first pool-entry token; leave q unchanged so the
    			// pool-entry loop below picks it up.
    			try {
    				level.length = Math.max(1, Integer.parseInt((String) lenItem));
    				q++;
<<<<<<< HEAD
    			} catch (Exception e) { level.length = 16 + level.getId() * Level.multiplier; }
    		} else {
    			level.length = 16 + level.getId() * Level.multiplier;
=======
    			} catch (Exception e) { level.length = 16 + idx * Level.multiplier; }
    		} else {
    			level.length = 16 + idx * Level.multiplier;
>>>>>>> origin/main
    		}
    	}
    	while (q < bl_temp.size()) {
    		Object raw = bl_temp.get(q++);
    		parsePoolEntry(raw, level);
    	}
<<<<<<< HEAD
    	level.blocks = PoolRegistry.totalBlocks();
    	level.mobs = PoolRegistry.totalMobs();
=======
>>>>>>> origin/main
    }
    
    @SuppressWarnings("unchecked")
    private void parsePoolEntry(Object raw, Level level) {
    	if (raw == null) return;
    	int weight = 1;
    	String kind;
    	Object payload;
    	
    	if (raw instanceof Map) {
    		Map<String, Object> m = (Map<String, Object>) raw;
    		if (m.containsKey("weight")) {
    			Object w = m.get("weight");
    			if (w instanceof Number) weight = Math.max(1, ((Number) w).intValue());
    			else if (w != null) {
    				try { weight = Math.max(1, Integer.parseInt(w.toString())); }
    				catch (NumberFormatException nfe) {
<<<<<<< HEAD
    					plugin.getLogger().warning("blocks.yml: non-numeric weight '" + w + "' in entry " + m + "; defaulting to 1.");
=======
    					Oneblock.plugin.getLogger().warning("[Oneblock] blocks.yml: non-numeric weight '" + w + "' in entry " + m + "; defaulting to 1.");
>>>>>>> origin/main
    				}
    			}
    		}
    		if      (m.containsKey("block"))      { kind = "block";      payload = m.get("block"); }
    		else if (m.containsKey("mob"))        { kind = "mob";        payload = m.get("mob"); }
<<<<<<< HEAD
    		else if (m.containsKey("chest"))      { kind = "chest";      payload = m.get("chest"); }
    		else if (m.containsKey("command"))    { kind = "command";    payload = m.get("command"); }
    		else {
    			plugin.getLogger().warning("blocks.yml: entry has no recognized kind (expected one of block/mob/loot_table/command): " + m);
=======
    		else if (m.containsKey("loot_table")) { kind = "loot_table"; payload = m.get("loot_table"); }
    		else if (m.containsKey("command"))    { kind = "command";    payload = m.get("command"); }
    		else {
    			Oneblock.plugin.getLogger().warning("[Oneblock] blocks.yml: entry has no recognized kind (expected one of block/mob/loot_table/command): " + m);
>>>>>>> origin/main
    			return;
    		}
    	} else if (raw instanceof String) {
    		String text = (String) raw;
    		if (text.isEmpty()) return;
    		if (text.charAt(0) == '/') { kind = "command"; payload = text; }
    		else {
    			try { EntityType.valueOf(text.toUpperCase()); kind = "mob"; payload = text.toUpperCase(); }
    			catch (Exception ignore) { kind = "block"; payload = text; }
    		}
    	} else {
    		return;
    	}
    	
    	if (payload == null) return;
    	switch (kind) {
    		case "block":
<<<<<<< HEAD
    			PoolRegistry.addBlock(resolveBlock(payload.toString()), weight);
=======
    			level.blockPool.add(resolveBlock(payload.toString()), weight);
>>>>>>> origin/main
    			break;
    		case "mob":
    			EntityType et;
    			try { et = EntityType.valueOf(payload.toString().toUpperCase()); }
    			catch (Exception e) {
<<<<<<< HEAD
    				plugin.getLogger().warning("blocks.yml: unknown mob '" + payload + "'");
    				return;
    			}
    			PoolRegistry.addMob(et, weight);
    			break;
    		case "chest":
    			String chest_name = payload.toString();
    			if (!ChestItems.hasChest(chest_name)) {
    				plugin.getLogger().warning("blocks.yml: chest name '" + payload + "' not found in chests.yml");
    				return;
    			}
    			PoolRegistry.addBlock(PoolEntry.chest(chest_name), weight);
    			break;
    		case "command":
    			String str = payload.toString();
    			try { String.format(str.substring(1), 99, 64, 99); } 
    			catch (Exception e) 
    			{
    				plugin.getLogger().warning("blocks.yml: invalid command '" + payload + "'"); 				
    				return;
    			}
    			PoolRegistry.addBlock(PoolEntry.command(str), weight);
=======
    				Oneblock.plugin.getLogger().warning("[Oneblock] blocks.yml: unknown mob '" + payload + "'");
    				return;
    			}
    			level.mobPool.add(et, weight);
    			break;
    		case "loot_table":
    			NamespacedKey key = ChestItems.parseKey(payload.toString());
    			if (key == null) {
    				Oneblock.plugin.getLogger().warning("[Oneblock] blocks.yml: invalid loot table key '" + payload + "'");
    				return;
    			}
    			level.blockPool.add(PoolEntry.lootTable(key), weight);
    			break;
    		case "command":
    			level.blockPool.add(PoolEntry.command(payload.toString()), weight);
>>>>>>> origin/main
    			break;
    	}
    }
    
    /**
     * Resolve a block-name string to a {@link PoolEntry}. Mirrors the legacy resolver
     * chain: Material → custom block (ItemsAdder / Oraxen / Nexo / CraftEngine) →
     * XMaterial (legacy servers). Unresolved names fall back to {@link PoolEntry#GRASS}
     * which renders as grass + chance of flower at runtime.
     */
    private PoolEntry resolveBlock(String text) {
    	if (text == null || text.isEmpty()) return PoolEntry.GRASS;
    	Object mt = Material.matchMaterial(text);
<<<<<<< HEAD
    	if (mt == null || mt == GRASS_BLOCK || !((Material) mt).isBlock())
    		mt = getCustomBlock(text);
    	if (legacy && mt == null) {
    		mt = XMaterial.matchXMaterial(text)
    				.map(xmt -> xmt == GRASS_BLOCK ? null : xmt)
=======
    	if (mt == null || mt == Oneblock.GRASS_BLOCK || !((Material) mt).isBlock())
    		mt = getCustomBlock(text);
    	if (Oneblock.legacy && mt == null) {
    		mt = XMaterial.matchXMaterial(text)
    				.map(xmt -> xmt == Oneblock.GRASS_BLOCK ? null : xmt)
>>>>>>> origin/main
    				.orElse(null);
    	}
    	if (mt == null) return PoolEntry.GRASS;
    	return PoolEntry.block(mt);
    }
	
	private Object getCustomBlock(String text) {
<<<<<<< HEAD
	    switch (plugin.placetype) {
=======
	    switch (Oneblock.plugin.placetype) {
>>>>>>> origin/main
	        case ItemsAdder: return CustomBlock.getInstance(text);
	        case Oraxen: return OraxenItems.exists(text) ? text : null;
	        case Nexo: return NexoBlocks.isCustomBlock(text) ? text : null;
	        case CraftEngine:
	            String[] pcid = text.split(":", 2);
<<<<<<< HEAD
	            return pcid.length == 2 ? Key.of(pcid) : null;
=======
	            return pcid.length == 2 ? text : null;
>>>>>>> origin/main
	        default: return null;
	    }
	}
	
<<<<<<< HEAD
    public void SetupProgressBar() {
		if (superlegacy) return;
=======
    public void setupProgressBar() {
		if (Oneblock.superlegacy) return;
>>>>>>> origin/main
		if (PlayerInfo.size() == 0) return;
		
		if (Level.max.color == null) Level.max.color = BarColor.GREEN;
		if (Level.max.style == null) Level.max.style = BarStyle.SOLID;
		
		PlayerInfo.list.forEach(inf -> {if (inf.uuid != null){
			Player p = Bukkit.getPlayer(inf.uuid);
			if (p == null)
				inf.createBar();
			else
<<<<<<< HEAD
				inf.createBar(getBarTitle(p, inf.lvl));
        	        	
			inf.bar.setVisible(progress_bar);
        }});
	}
	
    private void Messagefile() {
        File message = getFile("messages.yml");
        config_temp = YamlConfiguration.loadConfiguration(message);
        
        Messages.help = MessageCheck("help", Messages.help);
        Messages.help_adm = MessageCheck("help_adm", Messages.help_adm);
        Messages.invite_usage = MessageCheck("invite_usage", Messages.invite_usage);
        Messages.invite_yourself = MessageCheck("invite_yourself", Messages.invite_yourself);
        Messages.invite_no_island = MessageCheck("invite_no_island", Messages.invite_no_island);
        Messages.invite_team = MessageCheck("invite_team", Messages.invite_team);
        Messages.invited = MessageCheck("invited", Messages.invited);
        Messages.invited_success = MessageCheck("invited_success", Messages.invited_success);
        Messages.kicked = MessageCheck("kicked", Messages.kicked);
        Messages.kick_usage = MessageCheck("kick_usage", Messages.kick_usage);
        Messages.kick_yourself = MessageCheck("kick_yourself", Messages.kick_yourself);
        Messages.accept_success = MessageCheck("accept_success", Messages.accept_success);
        Messages.accept_none = MessageCheck("accept_none", Messages.accept_none);
        Messages.idreset = MessageCheck("idreset", Messages.idreset);
        Messages.protection = MessageCheck("protection", Messages.protection);
        Messages.leave_not_set = MessageCheck("leave_not_set", Messages.leave_not_set);
        Messages.not_allow_visit = MessageCheck("not_allow_visit", Messages.not_allow_visit);
        Messages.allowed_visit = MessageCheck("allowed_visit", Messages.allowed_visit);
        Messages.forbidden_visit = MessageCheck("forbidden_visit", Messages.forbidden_visit);
=======
				inf.createBar(Oneblock.getBarTitle(p, inf.lvl));
        	        	
			inf.bar.setVisible(Oneblock.settings().progress_bar);
        }});
	}
	
    private void loadMessages() {
        File message = getFile("messages.yml");
        config_temp = YamlConfiguration.loadConfiguration(message);
        
        Messages.help = checkMessage("help", Messages.help);
        Messages.help_adm = checkMessage("help_adm", Messages.help_adm);
        Messages.invite_usage = checkMessage("invite_usage", Messages.invite_usage);
        Messages.invite_yourself = checkMessage("invite_yourself", Messages.invite_yourself);
        Messages.invite_no_island = checkMessage("invite_no_island", Messages.invite_no_island);
        Messages.invite_team = checkMessage("invite_team", Messages.invite_team);
        Messages.invited = checkMessage("invited", Messages.invited);
        Messages.invited_success = checkMessage("invited_success", Messages.invited_success);
        Messages.kicked = checkMessage("kicked", Messages.kicked);
        Messages.kick_usage = checkMessage("kick_usage", Messages.kick_usage);
        Messages.kick_yourself = checkMessage("kick_yourself", Messages.kick_yourself);
        Messages.accept_success = checkMessage("accept_success", Messages.accept_success);
        Messages.accept_none = checkMessage("accept_none", Messages.accept_none);
        Messages.idreset = checkMessage("idreset", Messages.idreset);
        Messages.protection = checkMessage("protection", Messages.protection);
        Messages.leave_not_set = checkMessage("leave_not_set", Messages.leave_not_set);
        Messages.not_allow_visit = checkMessage("not_allow_visit", Messages.not_allow_visit);
        Messages.allowed_visit = checkMessage("allowed_visit", Messages.allowed_visit);
        Messages.forbidden_visit = checkMessage("forbidden_visit", Messages.forbidden_visit);
>>>>>>> origin/main
        
        File gui = getFile("gui.yml");
        config_temp = YamlConfiguration.loadConfiguration(gui);
        
<<<<<<< HEAD
        Messages.baseGUI = MessageCheck("baseGUI", Messages.baseGUI);
        Messages.acceptGUI = MessageCheck("acceptGUI", Messages.acceptGUI);
        Messages.acceptGUIignore = MessageCheck("acceptGUIignore", Messages.acceptGUIignore);
        Messages.acceptGUIjoin = MessageCheck("acceptGUIjoin", Messages.acceptGUIjoin);
        Messages.topGUI = MessageCheck("topGUI", Messages.topGUI);
        Messages.visitGUI = MessageCheck("visitGUI", Messages.visitGUI);
        Messages.idresetGUI = MessageCheck("idresetGUI", Messages.idresetGUI);
    }
    
    private String MessageCheck(String name, String def_message) {
=======
        Messages.baseGUI = checkMessage("baseGUI", Messages.baseGUI);
        Messages.acceptGUI = checkMessage("acceptGUI", Messages.acceptGUI);
        Messages.acceptGUIignore = checkMessage("acceptGUIignore", Messages.acceptGUIignore);
        Messages.acceptGUIjoin = checkMessage("acceptGUIjoin", Messages.acceptGUIjoin);
        Messages.topGUI = checkMessage("topGUI", Messages.topGUI);
        Messages.visitGUI = checkMessage("visitGUI", Messages.visitGUI);
        Messages.idresetGUI = checkMessage("idresetGUI", Messages.idresetGUI);
    }
    
    private String checkMessage(String name, String def_message) {
>>>>>>> origin/main
    	if (config_temp.isString(name))
        	return Utils.translateColorCodes(config_temp.getString(name));
    	return def_message;
    }
    
<<<<<<< HEAD
    private void Flowerfile() {
        plugin.flowers.clear();
        File flower = getFile("flowers.yml");
        config_temp = YamlConfiguration.loadConfiguration(flower);
        plugin.flowers.add(GRASS);
        for(String list:config_temp.getStringList("flowers"))
        	plugin.flowers.add(XMaterial.matchXMaterial(list).orElse(GRASS));
    }
    
    private void Chestfile() {
        ChestItems.chest = getFile("chests.yml");
=======
    private void loadFlowers() {
        Oneblock.plugin.flowers.clear();
        File flower = getFile("flowers.yml");
        config_temp = YamlConfiguration.loadConfiguration(flower);
        Oneblock.plugin.flowers.add(Oneblock.GRASS);
        for(String list:config_temp.getStringList("flowers"))
        	Oneblock.plugin.flowers.add(XMaterial.matchXMaterial(list).orElse(Oneblock.GRASS));
    }
    
    private void loadChests() {
        ChestItems.chest = getFile("chests.yml");
        LegacyBlocksMigrator.migrateChests(ChestItems.chest);
>>>>>>> origin/main
        ChestItems.load();
    }
    
    File getFile(String name) {
<<<<<<< HEAD
    	File file = new File(plugin.getDataFolder(), name);
        if (!file.exists())
        	plugin.saveResource(name, false);
        return file;
    }
    
    String Check(String type, String data) {
    	if (!config.isString(type))
            config.set(type, data);
    	return config.getString(type);
    }
    int Check(String type, int data) {
    	if (!config.isInt(type))
            config.set(type, data);
    	return config.getInt(type);
    }
    double Check(String type, double data) {
    	if (!config.isDouble(type))
            config.set(type, data);
    	return config.getDouble(type);
    }
    boolean Check(String type, boolean data) {
    	if (!config.isBoolean(type))
            config.set(type, data);
    	return config.getBoolean(type);
    }
    List<String> Check(String type, List<String> data) {
    	if (!config.isList(type))
            config.set(type, data);
    	return config.getStringList(type);
=======
    	File file = new File(Oneblock.plugin.getDataFolder(), name);
        if (!file.exists())
        	Oneblock.plugin.saveResource(name, false);
        return file;
    }

    /**
     * The canonical {@code config.yml} {@link File} inside the plugin data
     * folder. Callers that need to persist changes via
     * {@link LegacyConfigSaver#save} (e.g. the admin-command path in
     * {@code CommandHandler}) use this instead of the hidden static
     * {@code LegacyConfigSaver.file} field that Phase 3 removed.
     */
    public File getMainConfigFile() { return getFile("config.yml"); }
    
    String readOrDefault(String type, String data) {
    	if (!Oneblock.config.isString(type))
            Oneblock.config.set(type, data);
    	return Oneblock.config.getString(type);
    }
    int readOrDefault(String type, int data) {
    	if (!Oneblock.config.isInt(type))
            Oneblock.config.set(type, data);
    	return Oneblock.config.getInt(type);
    }
    double readOrDefault(String type, double data) {
    	if (!Oneblock.config.isDouble(type))
            Oneblock.config.set(type, data);
    	return Oneblock.config.getDouble(type);
    }
    boolean readOrDefault(String type, boolean data) {
    	if (!Oneblock.config.isBoolean(type))
            Oneblock.config.set(type, data);
    	return Oneblock.config.getBoolean(type);
    }
    List<String> readOrDefault(String type, List<String> data) {
    	if (!Oneblock.config.isList(type))
            Oneblock.config.set(type, data);
    	return Oneblock.config.getStringList(type);
>>>>>>> origin/main
    }
}
