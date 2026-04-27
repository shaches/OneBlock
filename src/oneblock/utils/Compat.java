package oneblock.utils;

import org.bukkit.Bukkit;

import com.cryptomorin.xseries.XMaterial;

import oneblock.VoidChunkGenerator;

/**
 * Static-only namespace for runtime-version capability probes and the
 * frequently-referenced {@link XMaterial} constants. Phase 6.10 extracted
 * these from {@link oneblock.Oneblock} so callers (and especially tests)
 * can ask "are we on legacy?" without forcing the {@code Oneblock} class
 * to load.
 *
 * <p>Pre-Phase-6.10 these were {@code public static final} fields on
 * {@code Oneblock} itself; that conflated "the plugin singleton" (a
 * Bukkit {@code JavaPlugin} instance with a real onEnable lifecycle)
 * with stateless capability probes. The conflation made tests that
 * needed only the probe values pay the cost of {@code Oneblock}'s
 * full set of static initialisers.
 *
 * <p>The probe initialisers themselves are safe to run in a unit test:
 * {@link XMaterial#supports(int, int)} and
 * {@link Utils#findMethod(Class, String)} both fall back gracefully
 * when running outside a real Bukkit server.
 *
 * <p>Renamed callers should reach the values through {@code Compat.<name>},
 * e.g. {@code Compat.legacy}, {@code Compat.GRASS_BLOCK}.
 */
public final class Compat {
    private Compat() {}

    /**
     * Sentinel {@link XMaterial} for the modern grass-block material.
     * Used by the {@code resolveBlock} routing rule
     * (see {@code ConfigManager.resolveBlock}) and the
     * {@code Oneblock.generateBlock} placement path.
     */
    public static final XMaterial GRASS_BLOCK = XMaterial.GRASS_BLOCK;

    /**
     * Modern short-grass material. The "flower" decoration that
     * {@code generateBlock} sprinkles on top of the default-grass
     * pool entry defaults to this if no flower list is configured.
     * (Note: name preserved as {@code GRASS} for compatibility with
     * existing call sites; the underlying XMaterial constant is
     * {@link XMaterial#SHORT_GRASS}.)
     */
    public static final XMaterial GRASS = XMaterial.SHORT_GRASS;

    /** {@code true} on Bukkit 1.18.2+ where {@code Bukkit.createWorldBorder()} exists. */
    public static final boolean isBorderSupported =
            Utils.findMethod(Bukkit.class, "createWorldBorder");

    /** {@code true} on pre-1.13 servers (no flat-ID material API, no namespaced keys). */
    public static final boolean legacy = !XMaterial.supports(1, 13);

    /** {@code true} on pre-1.9 servers (no BossBar API, no progress-bar placeholder). */
    public static final boolean superlegacy = !XMaterial.supports(1, 9);

    /**
     * {@code true} on 1.21+ where the item-spawn velocity API was
     * reworked - {@code BlockEvent.ItemStackSpawn} uses the new
     * {@code copy().setVelocity()} path instead of the legacy
     * teleport when this is set.
     */
    public static final boolean needDropFix = XMaterial.supports(1, 21);

    /**
     * Single shared {@link VoidChunkGenerator} for the plugin's
     * island world. Returned by
     * {@code Oneblock.getDefaultWorldGenerator(...)} and never mutated.
     */
    public static final VoidChunkGenerator GENERATOR = new VoidChunkGenerator();
}
