package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import oneblock.placement.Place;
import oneblock.utils.Compat;

/**
 * Regression test for the {@code GRASS_BLOCK} type-confusion bug in
 * {@link ConfigManager#resolveBlock(String)}. The buggy line read:
 *
 * <pre>
 *   Object mt = Material.matchMaterial(text);
 *   if (mt == null || mt == Oneblock.GRASS_BLOCK || !((Material) mt).isBlock())
 *       mt = getCustomBlock(text);
 * </pre>
 *
 * <p>{@code Oneblock.GRASS_BLOCK} is an {@link com.cryptomorin.xseries.XMaterial}
 * enum constant. {@code mt} is the {@link Material} returned by
 * {@code Material.matchMaterial(text)}. Comparing them with {@code ==}
 * after widening to {@link Object} compiles cleanly but always
 * evaluates {@code false} at runtime - the values are different enum
 * types and can never share an identity.
 *
 * <p><b>Effect on a real server.</b> A {@code blocks.yml} entry of
 * {@code "GRASS_BLOCK"} resolves on every modern (1.13+) server to
 * {@code Material.GRASS_BLOCK}. The type-confused compare leaves
 * {@code mt} unchanged, so the parser publishes
 * {@code PoolEntry(BLOCK, Material.GRASS_BLOCK)} into the level pool.
 * Compare to the {@link PoolEntry#GRASS} sentinel
 * ({@code Kind.DECORATED_BLOCK}) which the placement code at
 * {@code Oneblock.java:248} treats specially: grass + a 1/3 chance of
 * a flower decoration. The bug silently suppresses the flower
 * decoration on every modern server's {@code GRASS_BLOCK} pool entry.
 *
 * <p>The XMaterial fallback two lines down ({@code xmt ==
 * Oneblock.GRASS_BLOCK}) is correctly XMaterial-to-XMaterial and is
 * not affected.
 *
 * <h2>Test strategy</h2>
 *
 * <p>{@link Material#matchMaterial(String)} dereferences
 * {@link org.bukkit.Registry} whose {@code <clinit>} cannot run
 * outside a real server (NoClassDefFoundError). We use
 * {@link Mockito#mockStatic(Class, org.mockito.stubbing.Answer)} with
 * {@link Mockito#CALLS_REAL_METHODS} to stub
 * {@code matchMaterial("GRASS_BLOCK")} so the test can drive
 * {@link ConfigManager#resolveBlock} directly.
 *
 * <p>The Material instance method {@link Material#isBlock()} also
 * touches {@code Registry} on Spigot 1.20.6 (NoClassDefFoundError
 * confirmed by an earlier probe), so the test cannot afford to reach
 * that call. Java's left-to-right short-circuit on {@code ||} works
 * in our favour:
 *
 * <ul>
 *   <li><b>Pre-fix.</b> The buggy compare {@code mt ==
 *       Oneblock.GRASS_BLOCK} (Material vs XMaterial) is {@code false},
 *       so the third operand {@code !((Material) mt).isBlock()} is
 *       evaluated, the {@code isBlock()} call hits {@code Registry},
 *       and the test crashes with
 *       {@link ExceptionInInitializerError}. <em>That crash is the
 *       proof the bug exists.</em></li>
 *   <li><b>Post-fix.</b> The corrected compare {@code mt ==
 *       Material.GRASS_BLOCK} is {@code true}, the {@code ||}
 *       short-circuits, {@code isBlock()} is never called, and the
 *       test passes asserting {@link PoolEntry#GRASS} is
 *       returned.</li>
 * </ul>
 *
 * <p>{@link Oneblock#plugin} is a Mockito mock with
 * {@code placetype = Place.Type.basic} so the {@code getCustomBlock}
 * fallback returns {@code null} (the {@code default:} branch of its
 * switch) and the result short-circuits to
 * {@link PoolEntry#GRASS}.
 */
class ResolveBlockGrassBlockBugTest {

    private static Oneblock savedPlugin;

    @BeforeAll
    static void installMockPlugin() {
        savedPlugin = Oneblock.plugin;
        Oneblock mockPlugin = mock(Oneblock.class);
        when(mockPlugin.getLogger()).thenReturn(mock(Logger.class));
        // Direct field write on a Mockito mock instance: Mockito mocks
        // subclass the original via ByteBuddy, so the public field
        // 'placetype' exists on the instance and is freely assignable.
        // 'basic' falls into the default branch of getCustomBlock's
        // switch and returns null - exactly the path the fix takes.
        mockPlugin.placetype = Place.Type.basic;
        Oneblock.plugin = mockPlugin;
    }

    @AfterAll
    static void restorePlugin() {
        Oneblock.plugin = savedPlugin;
    }

    @Test
    @DisplayName("\"GRASS_BLOCK\" YAML entry resolves to the DECORATED_BLOCK sentinel (preserves flower decoration)")
    void grassBlockYamlEntryReturnsDefaultGrassSentinel() {
        ConfigManager cm = new ConfigManager();

        try (MockedStatic<Material> ms = Mockito.mockStatic(Material.class, Mockito.CALLS_REAL_METHODS)) {
            ms.when(() -> Material.matchMaterial("GRASS_BLOCK"))
              .thenReturn(Material.GRASS_BLOCK);

            PoolEntry entry = cm.resolveBlock("GRASS_BLOCK");

            assertThat(entry).isSameAs(PoolEntry.GRASS);
            assertThat(entry.kind).isEqualTo(PoolEntry.Kind.DECORATED_BLOCK);
            assertThat(entry.value).isInstanceOf(DecoratedBlock.class);
            DecoratedBlock d = (DecoratedBlock) entry.value;
            assertThat(d.base()).isSameAs(Compat.GRASS_BLOCK);
        }
    }
}
