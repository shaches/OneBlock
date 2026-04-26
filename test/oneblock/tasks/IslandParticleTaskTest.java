package oneblock.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import oneblock.PlayerCache;

/**
 * Unit coverage for {@link IslandParticleTask#collectSpawns} - the
 * async, pure-function half of the Phase 4.3 split. Exercises the
 * cache-iteration semantics (skip null coordinates, emit four corners
 * per registered player, base-Y offset of +0.5) without spinning up a
 * Bukkit server. The main-thread dispatch via
 * {@code Bukkit.getScheduler().runTask} is gated by Phase 5.6 once the
 * Mockito-Bukkit harness is available.
 *
 * <p>The {@link PlayerCache} is mocked so we control exactly which
 * coordinates each player resolves to. {@link World} is also mocked
 * because {@link Location} only stores a {@link World} reference and
 * never dereferences it during construction.
 */
class IslandParticleTaskTest {

    private static final int BASE_Y = 64;
    private static final double EXPECTED_BASE_Y = BASE_Y + 0.5;

    @Test
    @DisplayName("collectSpawns: empty player cache returns an empty list")
    void emptyCacheReturnsEmptyList() {
        PlayerCache cache = Mockito.mock(PlayerCache.class);
        when(cache.getPlayers()).thenReturn(Collections.emptyList());

        List<Location> spawns = IslandParticleTask.collectSpawns(cache, Mockito.mock(World.class), BASE_Y);

        assertThat(spawns).isEmpty();
    }

    @Test
    @DisplayName("collectSpawns: one player produces 4 corner locations at baseY + 0.5")
    void singlePlayerProducesFourCorners() {
        PlayerCache cache = Mockito.mock(PlayerCache.class);
        World world = Mockito.mock(World.class);
        Player player = Mockito.mock(Player.class);

        when(cache.getPlayers()).thenReturn(Collections.singleton(player));
        // Cache returns int[]{X, Z, plID}; the particle task only uses [0] and [1].
        when(cache.getIslandCoordinates(player)).thenReturn(new int[] {100, 200, 0});

        List<Location> spawns = IslandParticleTask.collectSpawns(cache, world, BASE_Y);

        assertThat(spawns).hasSize(4);
        // The 4 corners are (X+0,Z+0), (X+0,Z+1), (X+1,Z+0), (X+1,Z+1).
        assertThat(spawns)
                .extracting(Location::getX)
                .containsExactlyInAnyOrder(100.0, 100.0, 101.0, 101.0);
        assertThat(spawns)
                .extracting(Location::getZ)
                .containsExactlyInAnyOrder(200.0, 201.0, 200.0, 201.0);
        // Y is the integer base + 0.5 for every spawn.
        assertThat(spawns)
                .extracting(Location::getY)
                .containsOnly(EXPECTED_BASE_Y);
        // Every produced location references the same world the caller passed.
        assertThat(spawns)
                .extracting(Location::getWorld)
                .containsOnly(world);
    }

    @Test
    @DisplayName("collectSpawns: skips players whose cached coordinates are null")
    void skipsPlayersWithNullCoordinates() {
        PlayerCache cache = Mockito.mock(PlayerCache.class);
        World world = Mockito.mock(World.class);
        Player resolved = Mockito.mock(Player.class);
        Player unresolved = Mockito.mock(Player.class);

        when(cache.getPlayers()).thenReturn(Arrays.asList(resolved, unresolved));
        when(cache.getIslandCoordinates(resolved)).thenReturn(new int[] {0, 0, 0});
        when(cache.getIslandCoordinates(unresolved)).thenReturn(null);

        List<Location> spawns = IslandParticleTask.collectSpawns(cache, world, BASE_Y);

        // Only the resolved player contributed, so we expect 4 corner locations
        // (the unresolved player would have produced 0 because the early-skip
        // check in the loop body kicks in before any add()).
        assertThat(spawns).hasSize(4);
    }

    @Test
    @DisplayName("collectSpawns: multiple players emit 4 corners each, ordered per cache iteration")
    void twoPlayersProduceEightLocations() {
        PlayerCache cache = Mockito.mock(PlayerCache.class);
        World world = Mockito.mock(World.class);
        Player a = Mockito.mock(Player.class);
        Player b = Mockito.mock(Player.class);

        when(cache.getPlayers()).thenReturn(Arrays.asList(a, b));
        when(cache.getIslandCoordinates(a)).thenReturn(new int[] {0, 0, 0});
        when(cache.getIslandCoordinates(b)).thenReturn(new int[] {500, -300, 1});

        List<Location> spawns = IslandParticleTask.collectSpawns(cache, world, BASE_Y);

        assertThat(spawns).hasSize(8);
        // Two distinct (x, z) clusters, four points each: a around (0,0), b around (500,-300).
        long aCluster = spawns.stream()
                .filter(l -> l.getX() >= 0 && l.getX() <= 1)
                .filter(l -> l.getZ() >= 0 && l.getZ() <= 1)
                .count();
        long bCluster = spawns.stream()
                .filter(l -> l.getX() >= 500 && l.getX() <= 501)
                .filter(l -> l.getZ() >= -300 && l.getZ() <= -299)
                .count();
        assertThat(aCluster).isEqualTo(4);
        assertThat(bCluster).isEqualTo(4);
    }
}
