// Copyright © 2026 MrMarL. The MIT License (MIT).
package oneblock;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;

import oneblock.config.Settings;
import oneblock.events.BlockEvent;
import oneblock.events.ItemsAdderEvent;
import oneblock.events.RespawnJoinEvent;
import oneblock.events.TeleportEvent;
import oneblock.events.TeleportNetherEvent;
import oneblock.gui.GUI;
import oneblock.gui.GUIListener;
import oneblock.loot.LootTableDispatcher;
import oneblock.storage.*;
import oneblock.placement.*;
import oneblock.tasks.IslandBlockGenTask;
import oneblock.tasks.IslandParticleTask;
import oneblock.tasks.PlayerCacheRefreshTask;
import oneblock.tasks.PlayerDataSaveTask;
import oneblock.tasks.WorldInitTask;
import oneblock.utils.*;
import oneblock.worldguard.*;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.meta.SkullMeta;

public class Oneblock extends JavaPlugin {
    public static Oneblock plugin;
    
    private static final int FLOWER_CHANCE = 3;
    private static final int BORDER_WARNING_DISTANCE = 2;
    private static final double BORDER_DAMAGE_AMOUNT = .2;
    private static final double BORDER_DAMAGE_BUFFER = 1;
    
    public static final Random rnd = new Random();
    public static final XMaterial GRASS_BLOCK = XMaterial.GRASS_BLOCK, GRASS = XMaterial.SHORT_GRASS;
    public static final VoidChunkGenerator GenVoid = new VoidChunkGenerator();
    public static final boolean isBorderSupported = Utils.findMethod(Bukkit.class, "createWorldBorder");// Is virtual border supported?;
    public static final boolean legacy = !XMaterial.supports(1,13);// Is version 1.13 supported?
    public static final boolean superlegacy = !XMaterial.supports(1,9);// Is version 1.9 supported?
    public static final boolean needDropFix = XMaterial.supports(1,21);// Is version 1.21 supported?
    
    public static ConfigManager configManager = new ConfigManager();
    
    /**
     * Island layout origin: {@code (world, x, y, z, offset)} treated as a single
     * atomically-replaceable unit. Writers mutate the origin via
     * {@link #setPosition} / {@link #setOffset} which perform one atomic swap
     * each; readers call {@link #origin()} once and use the captured snapshot
     * so that a concurrent {@code /ob set} cannot produce a mixed (torn) view
     * (e.g. new {@code x} with old {@code offset}).
     *
     * <p>Package-private so test code can reflect it; callers outside the
     * package use the getter helpers ({@link #getX()}, {@link #getOffset()},
     * ...) or {@link #origin()}.
     */
    static final AtomicReference<IslandOrigin> ORIGIN =
            new AtomicReference<>(IslandOrigin.EMPTY);

    /**
     * Singleton holder for every admin-toggleable flag the plugin owns.
     * Phase 3 extracted what used to be ≈17 {@code public static volatile}
     * fields on this class into {@link Settings} (a typed bag of {@code
     * volatile} instance fields). Callers reach the live values through
     * {@link #settings()} — e.g. {@code Oneblock.settings().particle} —
     * which preserves the per-field cross-thread visibility we already had
     * while letting tests instantiate a fresh {@code Settings} without
     * spinning up Bukkit.
     */
    private static final Settings SETTINGS = new Settings();

    /** The live, mutable {@link Settings} singleton. Never returns {@code null}. */
    public static Settings settings() { return SETTINGS; }
    
    public static volatile YamlConfiguration config;
    
    public final String version = getDescription().getVersion();
    public OBWorldGuard worldGuard = new OBWorldGuard();
    public Place.Type placetype = Place.Type.basic;
    private Place placer;
    
    /**
     * Leave-world reference. Written only by the main thread (
     * {@link Initialization} task's first read picks up a freshly-assigned
     * value. Separate from {@link #ORIGIN} because it belongs to a different
     * workflow (player teleport destination) and is updated independently.
     */
    public static volatile World leavewor;
    boolean PAPI = false;
    /**
     * True after the four steady-state runners ({@code PlayerCacheRefresh},
     * {@code PlayerDataSave}, {@code IslandParticle}, {@code IslandBlockGen})
     * have been scheduled by {@link #runMainTask()}. Read by {@code JoinCommand}
     * to decide whether the first {@code /ob j} after a fresh server start
     * needs to kick off the runners. Public for cross-package access from
     * {@code oneblock.command.sub.*} after the Phase 3.5 split.
     */
    public boolean enabled = false;

    public ArrayList <XMaterial> flowers = new ArrayList<>();
    public PlayerCache cache = new PlayerCache();

    /** Shorthand for {@code origin().world()}. May be {@code null} before the
     * configured world finishes loading; callers must guard. */
    public static World getWorld() { return ORIGIN.get().world(); }
    /** Snapshot of the current island origin. Always non-null. */
    public static IslandOrigin origin()  { return ORIGIN.get(); }
    /** Shorthand for {@code origin().x()}. */
    public static int getX()      { return ORIGIN.get().x(); }
    /** Shorthand for {@code origin().y()}. */
    public static int getY()      { return ORIGIN.get().y(); }
    /** Shorthand for {@code origin().z()}. */
    public static int getZ()      { return ORIGIN.get().z(); }
    /** Shorthand for {@code origin().offset()}; {@code 0} means "not configured". */
    public static int getOffset() { return ORIGIN.get().offset(); }
    public boolean isPAPIEnabled() { return PAPI; }
    public int[] getIslandCoordinates(final int id) {
    	IslandOrigin o = ORIGIN.get();
    	return IslandCoordinateCalculator.getById(id, o.x(), o.z(), o.offset(), SETTINGS.circleMode);
    }
    public int findNearestRegionId(final Location loc) { return IslandCoordinateCalculator.findNearestRegionId(loc); }
    
    @Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {return GenVoid;}
    
    public static String getBarTitle(Player p, int lvl) {
		if (SETTINGS.lvlBarMode) return Level.get(lvl).name;
		if (plugin.PAPI) return PlaceholderAPI.setPlaceholders(p, SETTINGS.phText);
        
		return SETTINGS.phText;
	}
    
    @Override
    public void onEnable() {
    	plugin = this;
        GUI.legacy = !Utils.findMethod(SkullMeta.class, "setOwningPlayer");
        final Metrics metrics = new Metrics(this, 14477);
        final PluginManager pluginManager = Bukkit.getPluginManager();
        
        getLogger().info(
        		"\n█▀█ ░░░░ ░░░ █▀▄ ░░░ ░░░ ░░░ ░░░" + 
        		"\n█░█ █▄░█ █▀▀ █▄▀ █░░ █▀█ █▀▀ █▄▀" + 
        		"\n█▄█ █░▀█ ██▄ █▄▀ █▄▄ █▄█ █▄▄ █░█\n" + 
        		"\nby MrMarL");
        if (PAPI = pluginManager.isPluginEnabled("PlaceholderAPI")) {
        	getLogger().info("PlaceholderAPI has been found!");
            new OBP().register();
        }
        
        placetype = determinePlaceType(pluginManager);
        placer = Place.GetPlacerByType(placetype);
        getLogger().info("Custom block support mode: " + placetype.name());
        
        configManager.loadMainConfig();
        loadPlayerData();
        configManager.loadAdditionalConfigFiles();
        
        setupMetrics(metrics);
        
        pluginManager.registerEvents(new RespawnJoinEvent(), this);
        if (!superlegacy) pluginManager.registerEvents(new TeleportEvent(), this);
        pluginManager.registerEvents(new BlockEvent(), this);
        pluginManager.registerEvents(new GUIListener(), this);
        pluginManager.registerEvents(new TeleportNetherEvent(), this);
        if (placetype == Place.Type.ItemsAdder) pluginManager.registerEvents(new ItemsAdderEvent(), this);
        getCommand("oneblock").setExecutor(new CommandHandler());
        getCommand("oneblock").setTabCompleter(new CommandTabCompleter());
        
        if (getOffset() == 0) return;
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new WorldInitTask(this), 32, 80);
    }
    
    private Place.Type determinePlaceType(PluginManager pluginManager) {
        if (pluginManager.isPluginEnabled("ItemsAdder")) return Place.Type.ItemsAdder;
        if (pluginManager.isPluginEnabled("Oraxen")) return Place.Type.Oraxen;
        if (pluginManager.isPluginEnabled("Nexo")) return Place.Type.Nexo;
        if (pluginManager.isPluginEnabled("CraftEngine")) return Place.Type.CraftEngine;
        return legacy ? Place.Type.legacy : Place.Type.basic;
    }
    
    public void reload() {
    	configManager.loadConfigFiles();
    	worldGuard.recreateRegions();
    	reloadBorders();
    }
    
    private void setupMetrics(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("premium", () -> String.valueOf(OBWorldGuard.canUse)));
        metrics.addCustomChart(new SimplePie("circle_mode", () -> String.valueOf(SETTINGS.circleMode)));
        metrics.addCustomChart(new SimplePie("use_empty_islands", () -> String.valueOf(SETTINGS.useEmptyIslands)));
        metrics.addCustomChart(new SimplePie("gui", () -> String.valueOf(GUI.enabled)));
        metrics.addCustomChart(new SimplePie("place_type", () -> String.valueOf(placetype)));
    }
    
    public void runMainTask() {
    	Bukkit.getScheduler().cancelTasks(this);
		if (getOffset() == 0) return;
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new PlayerCacheRefreshTask(this), 0, 120);
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new PlayerDataSaveTask(this), 200, 6000);
		if (!superlegacy) Bukkit.getScheduler().runTaskTimerAsynchronously(this, new IslandParticleTask(this), 40, 40);
		Bukkit.getScheduler().runTaskTimer(this, new IslandBlockGenTask(this), 40, 80);
		enabled = true;
		
    	if (OBWorldGuard.canUse && Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
        	getLogger().info("WorldGuard has been found!");
        	worldGuard.recreateRegions();
        }
        else OBWorldGuard.setEnabled(false);
    }
    
    public void generateBlock(final int playerX, final int playerZ, final int plID, final Player ponl, final Block block) {
    	final PlayerInfo inf = PlayerInfo.get(plID);
    	Level levelInfo = Level.get(inf.lvl); 
        if (++inf.breaks >= inf.getRequiredBreaks()) {
        	levelInfo = inf.lvlup();
        	if (SETTINGS.progressBar) inf.createBar();
        	configManager.reward.executeRewards(ponl, inf.lvl, levelInfo.name);
        }
        if (SETTINGS.progressBar) {
            inf.bar.setTitle(getBarTitle(ponl, inf.lvl));
            inf.bar.setProgress(inf.getPercent());
            inf.bar.addPlayer(ponl);
        }
        
        PoolEntry entry = levelInfo.blockPool.pick(rnd);
        if (entry == null || entry.kind == PoolEntry.Kind.DEFAULT_GRASS) {
            XBlock.setType(block, GRASS_BLOCK);
            if (rnd.nextInt(FLOWER_CHANCE) == 1)
                XBlock.setType(getWorld().getBlockAt(playerX, getY() + 1, playerZ), flowers.get(rnd.nextInt(flowers.size())));
        }
        else switch (entry.kind) {
            case BLOCK:
                placer.setType(block, entry.value, SETTINGS.physics);
                break;
            case LOOT_TABLE:
                LootTableDispatcher.populate(block, (NamespacedKey) entry.value, rnd);
                break;
            case COMMAND:
                Place.executeCommand(block, (String) entry.value);
                break;
            default:
                break;
        }

        if (rnd.nextInt(SETTINGS.mobSpawnChance) == 0) spawnRandomMob(playerX, playerZ, levelInfo);
	}
    
	public void spawnRandomMob(int posX, int posZ, Level level) {
		EntityType type = level.mobPool.pick(rnd);
		if (type == null) return;
		getWorld().spawnEntity(new Location(getWorld(), posX + .5, getY() + 1, posZ + .5), type);
	}
    
    public void updateBorderLocation(Player pl, Location loc) {
    	int plID = findNearestRegionId(loc);
		int result[] = getIslandCoordinates(plID);
        int playerX = result[0], playerZ = result[1];
		
		WorldBorder br = Bukkit.createWorldBorder();
    	br.setCenter(playerX+.5, playerZ+.5);
    	int off = getOffset();
    	br.setSize(off - 1 + (off & 1));
    	br.setWarningDistance(BORDER_WARNING_DISTANCE);
    	br.setDamageAmount(BORDER_DAMAGE_AMOUNT);
    	br.setDamageBuffer(BORDER_DAMAGE_BUFFER);
    	pl.setWorldBorder(br);
    }
    
    public void updateBorder(final Player pl) {
    	WorldBorder border = pl.getWorldBorder();
    	Bukkit.getScheduler().runTaskLaterAsynchronously(this, 
    		() -> { pl.setWorldBorder(border); }, 10L);
    }
    
    public void reloadBorders() {
    	if (!isBorderSupported) return;
    	World w = getWorld();
    	if (w == null) return;
    	if (SETTINGS.border) w.getPlayers().forEach(pl -> plugin.updateBorderLocation(pl, pl.getLocation()));
    	else w.getPlayers().forEach(pl -> pl.setWorldBorder(null));
    }
    
    public boolean isWithinIslandBounds(Location loc, int centerX, int centerZ) {
        int deltaX = loc.getBlockX() - centerX;
        int deltaZ = loc.getBlockZ() - centerZ;
        int radius = Math.abs(getOffset() >> 1) + 1;
        
        return Math.abs(deltaX) <= radius && Math.abs(deltaZ) <= radius;
    }
    
    @Override
    public void onDisable() {
    	saveData();
    	DatabaseManager.close();
    }
    
    public void saveData() {
    	if (DatabaseManager.save(PlayerInfo.list)) return;
    	JsonPlayerDataStore.write(PlayerInfo.list);
    }

    private void loadPlayerData() {
        DatabaseManager.initialize();
        PlayerInfo.replaceAll(DatabaseManager.load());

    	if (!PlayerInfo.list.isEmpty()) {
    		getLogger().info("Player data has been successfully obtained from the " + DatabaseManager.dbType + " database.");
    		return;
    	}

		if (JsonPlayerDataStore.f.exists())
			PlayerInfo.replaceAll(JsonPlayerDataStore.read());
		else
			PlayerInfo.replaceAll(LegacyYamlPlayerDataStore.read());
    }
    
    public void setPosition(Location loc) { setPosition(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()); }
    public void setPosition(World world, int x, int y, int z) {
    	// Single atomic swap: readers on async threads either see the pre-call
    	// origin in full, or the post-call origin in full — never a mix.
    	IslandOrigin next = ORIGIN.updateAndGet(prev -> new IslandOrigin(
    			world != null ? world : prev.world(), x, y, z, prev.offset()));
    	if (next.world() != null) config.set("world", next.world().getName());
        config.set("x", (double) x);
        config.set("y", (double) y);
        config.set("z", (double) z);
    }

    /**
     * Atomically set the cell-edge length ({@code offset}) that drives the
     * island grid. Also persists the new value to {@code config.yml} under
     * the key {@code set}. Safe to call only from the main thread (admin
     * {@code /ob set}) — config writes assume main-thread ownership.
     */
    public void setOffset(int off) {
    	ORIGIN.updateAndGet(prev -> prev.withOffset(off));
    	if (config != null) config.set("set", off);
    }

    /**
     * Atomically replace the world component of {@link #ORIGIN} while
     * preserving the loaded {@code x/y/z/offset}. Used by the async
     * {@code WorldInitTask} when a configured world becomes available
     * after a delayed Bukkit load. The swap is one CAS operation, so a
     * concurrent {@code /ob set} on the main thread either fully wins or
     * is fully overwritten - it cannot produce a torn snapshot.
     */
    public void updateOriginWorld(World w) {
    	ORIGIN.updateAndGet(prev -> new IslandOrigin(w, prev.x(), prev.y(), prev.z(), prev.offset()));
    }
    
    public Location getLeave() { return new Location(leavewor, config.getDouble("xleave"), config.getDouble("yleave"), config.getDouble("zleave"), (float)config.getDouble("yawleave"), 0f); }
    public void setLeave(Location loc) { setLeave(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw()); }
    public void setLeave(World world, double x, double y, double z, float yaw) {
    	if (world == null) return;
    	leavewor = world;
        config.set("leaveworld", leavewor.getName());
        config.set("xleave", x);
        config.set("yleave", y);
        config.set("zleave", z);
        config.set("yawleave", yaw);
    }
    
    public static int getLevel(UUID playerUuid) {
    	return PlayerInfo.get(playerUuid).lvl;
    }
    public static int getNextLevel(UUID playerUuid) {
    	return getLevel(playerUuid) + 1;
    }
    public static String getLevelName(UUID playerUuid) {
    	int lvl = getLevel(playerUuid);
    	return Level.get(lvl).name;
    }
    public static String getNextLevelName(UUID playerUuid) {
    	int lvl = getNextLevel(playerUuid);
    	return Level.get(lvl).name;
    }
    public static int getBroken(UUID playerUuid) {
        return PlayerInfo.get(playerUuid).breaks;
    }
    public static int getRemaining(UUID playerUuid) {
    	PlayerInfo inf = PlayerInfo.get(playerUuid);
    	return inf.getRequiredBreaks() - inf.breaks;
    }
    public static int getLevelLength(UUID playerUuid) {
    	return PlayerInfo.get(playerUuid).getRequiredBreaks();
    }
    public static boolean isVisitAllowed(UUID playerUuid) {
    	return PlayerInfo.get(playerUuid).allowVisit;
    }
    public static int countVisitors(UUID playerUuid) {
    	int count = 0;
    	int regionId = PlayerInfo.getId(playerUuid);
    	if (regionId != -1)
	    	for (Player ponl: plugin.cache.getPlayers())
	    		if (plugin.findNearestRegionId(ponl.getLocation()) == regionId)
	    			count++;
    	return count;
    }
    public static PlayerInfo getTop(int i) {
    	if (PlayerInfo.size() <= i) return PlayerInfo.not_found;
    	return getTop(i,getTopList());
    }
    public static PlayerInfo getTop(int i, List<PlayerInfo> sorted) {
    	if (sorted.size() <= i) return PlayerInfo.not_found;
    	return sorted.get(i).uuid == null ? PlayerInfo.not_found : sorted.get(i);
    }
    public static int getTopPosition(PlayerInfo player) {
        if (player == null || player.uuid == null) return -1;

        List<PlayerInfo> sorted = getTopList();
        for (int i = 0; i < sorted.size(); i++) {
            PlayerInfo entry = sorted.get(i);
            if (entry != null && player.uuid.equals(entry.uuid))
                return i;
        }

        return -1;
    }
    // Sorted top-list cache. Invalidated via PlayerInfo.topVersion() which bumps
    // on level-up, slot assignment, and bulk reload. Under light contention we
    // may re-sort twice from two threads simultaneously; that's benign duplicate
    // work and cheaper than a global lock in the hot placeholder path.
    private static volatile long topCacheVersion = -1;
    private static volatile List<PlayerInfo> topCache = java.util.Collections.emptyList();

    public static List<PlayerInfo> getTopList() {
    	long v = PlayerInfo.topVersion();
    	if (v != topCacheVersion) {
    		List<PlayerInfo> sorted = new ArrayList<>(PlayerInfo.list);
    		sorted.sort(PlayerInfo.COMPARE_BY_LVL);
    		topCache = java.util.Collections.unmodifiableList(sorted);
    		topCacheVersion = v;
    	}
    	return topCache;
    }
}