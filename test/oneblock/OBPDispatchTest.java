package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Coverage for {@link OBP#onRequest(OfflinePlayer, String)}, the
 * PlaceholderAPI hook used by every server's sidebar / scoreboard /
 * chat formatter to render {@code %OB_*%} tokens. Phase 5.4 lifts
 * the class from {@code 0%} line coverage; a regression here is a
 * silent empty-string class - PAPI swallows {@code null} returns,
 * so a misrouted dispatch wouldn't even log a warning.
 *
 * <p>Three mock layers cooperate per test:
 *
 * <ul>
 *   <li>{@link Oneblock#plugin} is a Mockito mock with the
 *       {@code cache} field assigned to a mock {@link PlayerCache}
 *       and {@code findNearestRegionId(Location)} stubbed when the
 *       {@code _by_position} dispatch is exercised. Restored in
 *       {@link #restorePlugin}.</li>
 *   <li>{@link PlayerInfo#list} is populated per test via
 *       {@link PlayerInfo#replaceAll(List)} and restored in
 *       {@link #restorePlayerInfo}. {@link Level#max} and
 *       {@link Level#snapshot()} are likewise snapshotted so per-test
 *       level setups don't leak.</li>
 *   <li>Tests that exercise {@code owner_name},
 *       {@code owner_online}, {@code top_<n>_name} or any
 *       {@code _by_position} branch open a
 *       {@link MockedStatic Mockito.mockStatic(Bukkit.class)} and
 *       stub {@link Bukkit#getOfflinePlayer(UUID)} /
 *       {@link Bukkit#getPlayer(UUID)} to return a fake
 *       {@link OfflinePlayer} or {@link Player}. The stubbed lookup
 *       never reaches the real Bukkit server which would NPE on
 *       {@code Bukkit.server == null}.</li>
 * </ul>
 */
class OBPDispatchTest {

    private static Oneblock savedPlugin;
    private static List<Level> savedLevels;
    private List<PlayerInfo> savedPlayerInfoList;

    private OBP obp;
    private PlayerCache mockCache;

    @BeforeAll
    static void installMockPlugin() {
        savedPlugin = Oneblock.plugin;
        savedLevels = new ArrayList<>(Level.snapshot());
    }

    @AfterAll
    static void restorePlugin() {
        Oneblock.plugin = savedPlugin;
        Level.replaceAll(savedLevels);
    }

    @BeforeEach
    void prepareFixture() throws Exception {
        // Each test gets a fresh plugin mock; some tests rewrite stubs.
        // The mock plugin's getLogger() must return a non-null logger
        // since the Phase 6.1 deprecation-warning path dereferences it.
        Oneblock mockPlugin = mock(Oneblock.class);
        mockCache = mock(PlayerCache.class);
        mockPlugin.cache = mockCache;
        when(mockPlugin.getLogger()).thenReturn(mock(java.util.logging.Logger.class));
        Oneblock.plugin = mockPlugin;

        savedPlayerInfoList = new ArrayList<>(PlayerInfo.list);
        PlayerInfo.replaceAll(new ArrayList<>());

        // Two levels: id 0 ("Bronze", length 16) and id 1 ("Silver",
        // length 32). Level.get(lvl) returns Level.max for any lvl that
        // overshoots the published list, which is the same fallback the
        // production code uses; the level lengths here are picked so
        // need_to_lvl_up has a deterministic non-zero result.
        Level bronze = new Level("Bronze");
        bronze.length = 16;
        Level silver = new Level("Silver");
        silver.length = 32;
        Level.replaceAll(Arrays.asList(bronze, silver));

        // Phase 6.1: clear the once-per-session deprecation-warning set
        // so each test starts fresh and the once-flag is exercised
        // independently of test ordering.
        java.lang.reflect.Field warned = OBP.class.getDeclaredField("WARNED_DEPRECATED_PLACEHOLDERS");
        warned.setAccessible(true);
        ((java.util.Set<?>) warned.get(null)).clear();

        obp = new OBP();
    }

    @AfterEach
    void restorePlayerInfo() {
        PlayerInfo.replaceAll(savedPlayerInfoList);
    }

    /** Build an {@link OfflinePlayer} mock whose {@code getUniqueId()} returns the given UUID. */
    private static OfflinePlayer offline(UUID uuid) {
        OfflinePlayer p = mock(OfflinePlayer.class);
        when(p.getUniqueId()).thenReturn(uuid);
        return p;
    }

    /** Add a single {@link PlayerInfo} slot at id {@code 0} with the given owner / lvl / breaks. */
    private static PlayerInfo putPlayer(UUID owner, int lvl, int breaks) {
        PlayerInfo info = new PlayerInfo(owner);
        info.lvl = lvl;
        info.breaks = breaks;
        PlayerInfo.set(0, info);
        return info;
    }

    // --------------------------------------------------------------
    // Null guard + unknown identifier fall-through
    // --------------------------------------------------------------

    @Test
    @DisplayName("onRequest(null, ...) returns null without dereferencing the identifier")
    void nullPlayerReturnsNull() {
        assertThat(obp.onRequest(null, "lvl")).isNull();
        assertThat(obp.onRequest(null, "completely_unknown")).isNull();
    }

    @Test
    @DisplayName("unknown identifier with no special suffix returns null (PAPI fall-through)")
    void unknownIdentifierReturnsNull() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 1, 5);
        assertThat(obp.onRequest(offline(owner), "totally_made_up_token")).isNull();
    }

    // --------------------------------------------------------------
    // Numeric / level-resolving placeholders
    // --------------------------------------------------------------

    @Test
    @DisplayName("%OB_lvl% returns the player's current level as a string")
    void lvlReturnsCurrentLevel() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 1, 5);
        assertThat(obp.onRequest(offline(owner), "lvl")).isEqualTo("1");
    }

    @Test
    @DisplayName("%OB_lvl_name% resolves through Level.get(lvl)")
    void lvlNameResolvesViaLevelGet() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 1, 5);
        assertThat(obp.onRequest(offline(owner), "lvl_name")).isEqualTo("Silver");
    }

    @Test
    @DisplayName("%OB_next_lvl% returns lvl+1 as a string")
    void nextLvlReturnsLvlPlusOne() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 5);
        assertThat(obp.onRequest(offline(owner), "next_lvl")).isEqualTo("1");
    }

    @Test
    @DisplayName("%OB_break_on_this_lvl% returns the breaks counter")
    void breakOnThisLvlReturnsBreaks() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 7);
        assertThat(obp.onRequest(offline(owner), "break_on_this_lvl")).isEqualTo("7");
    }

    @Test
    @DisplayName("%OB_lvl_length% (canonical, Phase 6.1) returns the current level's length")
    void lvlLengthCanonicalReturnsLevelLength() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);
        // Level 0 = Bronze, length 16 (set in @BeforeEach).
        assertThat(obp.onRequest(offline(owner), "lvl_length")).isEqualTo("16");
        // Canonical name MUST NOT trigger the deprecation warning.
        org.mockito.Mockito.verify(Oneblock.plugin.getLogger(), org.mockito.Mockito.never())
            .warning(org.mockito.ArgumentMatchers.contains("deprecated"));
    }

    @Test
    @DisplayName("%OB_lvl_lenght% (legacy typo preserved as alias) returns the current level's length")
    void lvlLenghtLegacyAliasReturnsLevelLength() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);
        // The original (typo'd) placeholder name remains accepted so
        // existing servers' scoreboards keep working unchanged.
        assertThat(obp.onRequest(offline(owner), "lvl_lenght")).isEqualTo("16");
    }

    @Test
    @DisplayName("%OB_lvl_lenght% legacy spelling logs a deprecation warning exactly once per session")
    void lvlLenghtLegacyAliasLogsDeprecationOnce() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);

        // First dispatch: the legacy alias is consumed AND the deprecation
        // warning is emitted.
        obp.onRequest(offline(owner), "lvl_lenght");
        org.mockito.ArgumentCaptor<String> msg = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(Oneblock.plugin.getLogger())
            .warning(msg.capture());
        assertThat(msg.getValue())
            .contains("%OB_lvl_lenght%")
            .contains("deprecated")
            .contains("%OB_lvl_length%");

        // Second dispatch (same session): the result is still served,
        // but no second warning fires - the once-per-session contract.
        obp.onRequest(offline(owner), "lvl_lenght");
        obp.onRequest(offline(owner), "lvl_lenght");
        org.mockito.Mockito.verify(Oneblock.plugin.getLogger(), org.mockito.Mockito.times(1))
            .warning(org.mockito.ArgumentMatchers.contains("deprecated"));
    }

    @Test
    @DisplayName("%OB_need_to_lvl_up% returns length-minus-breaks of the current level")
    void needToLvlUpReturnsRemaining() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 5);
        // Bronze length 16, breaks 5 -> 11 remaining.
        assertThat(obp.onRequest(offline(owner), "need_to_lvl_up")).isEqualTo("11");
    }

    // --------------------------------------------------------------
    // Visit / count / scale
    // --------------------------------------------------------------

    @Test
    @DisplayName("%OB_player_count% delegates to PlayerCache.getPlayers().size()")
    void playerCountDelegatesToCache() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);
        when(mockCache.getPlayers()).thenReturn(Arrays.asList(mock(Player.class), mock(Player.class), mock(Player.class)));

        assertThat(obp.onRequest(offline(owner), "player_count")).isEqualTo("3");
    }

    @Test
    @DisplayName("%OB_visit_allowed% returns 'true' or 'false' from PlayerInfo.allow_visit")
    void visitAllowedRoundTrip() {
        UUID owner = UUID.randomUUID();
        PlayerInfo info = putPlayer(owner, 0, 0);
        info.allow_visit = false;
        assertThat(obp.onRequest(offline(owner), "visit_allowed")).isEqualTo("false");
        info.allow_visit = true;
        assertThat(obp.onRequest(offline(owner), "visit_allowed")).isEqualTo("true");
    }

    @Test
    @DisplayName("%OB_percent% returns 'NN%' computed from breaks/length")
    void percentFormatsAsPercentInt() {
        UUID owner = UUID.randomUUID();
        // Bronze length 16, breaks 4 -> 25%.
        putPlayer(owner, 0, 4);
        assertThat(obp.onRequest(offline(owner), "percent")).isEqualTo("25%");
    }

    @Test
    @DisplayName("%OB_scale% returns a non-empty progress bar with the SCALE_CHAR substituted")
    void scaleReturnsSubstitutedBar() {
        UUID owner = UUID.randomUUID();
        // Bronze length 16, breaks 8 -> 50% progress.
        putPlayer(owner, 0, 8);
        String scale = obp.onRequest(offline(owner), "scale");
        assertThat(scale).isNotNull();
        // SCALE_CHAR is the unicode block. The original SCALE map uses
        // ╍ (placeholder) which is .replace()'d to the block char.
        assertThat(scale).contains("\u2588");
        assertThat(scale).doesNotContain("\u2509");
    }

    @Test
    @DisplayName("%OB_number_of_invited% returns the size of PlayerInfo.uuids")
    void numberOfInvitedReturnsSize() {
        UUID owner = UUID.randomUUID();
        PlayerInfo info = putPlayer(owner, 0, 0);
        info.uuids.add(UUID.randomUUID());
        info.uuids.add(UUID.randomUUID());
        assertThat(obp.onRequest(offline(owner), "number_of_invited")).isEqualTo("2");
    }

    // --------------------------------------------------------------
    // Owner name / online (Bukkit-stubbed)
    // --------------------------------------------------------------

    @Test
    @DisplayName("%OB_owner_name% returns the owner's Bukkit name when getOfflinePlayer resolves")
    void ownerNameReturnsBukkitName() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);
        OfflinePlayer ownerOff = mock(OfflinePlayer.class);
        when(ownerOff.getName()).thenReturn("Alice");

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getOfflinePlayer(owner)).thenReturn(ownerOff);
            assertThat(obp.onRequest(offline(owner), "owner_name")).isEqualTo("Alice");
        }
    }

    @Test
    @DisplayName("%OB_owner_name% returns the [None] sentinel when the slot has no owner UUID")
    void ownerNameReturnsNoneWhenSlotEmpty() {
        UUID viewer = UUID.randomUUID();
        // Empty slot at id 0 (uuid==null), and the viewer UUID isn't
        // registered, so PlayerInfo.get(viewer) returns the not_found
        // sentinel which also has uuid==null. Either branch yields the
        // [None] sentinel.
        PlayerInfo.set(0, new PlayerInfo(null));
        assertThat(obp.onRequest(offline(viewer), "owner_name")).isEqualTo("[None]");
    }

    @Test
    @DisplayName("%OB_owner_online% returns 'online' when Bukkit.getPlayer resolves a non-null Player")
    void ownerOnlineWhenPlayerResolves() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);
        Player onlinePlayer = mock(Player.class);

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getPlayer(owner)).thenReturn(onlinePlayer);
            assertThat(obp.onRequest(offline(owner), "owner_online")).isEqualTo("online");
        }
    }

    @Test
    @DisplayName("%OB_owner_online% returns 'offline' when Bukkit.getPlayer returns null")
    void ownerOnlineWhenPlayerNull() {
        UUID owner = UUID.randomUUID();
        putPlayer(owner, 0, 0);

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getPlayer(owner)).thenReturn(null);
            assertThat(obp.onRequest(offline(owner), "owner_online")).isEqualTo("offline");
        }
    }

    // --------------------------------------------------------------
    // top_position + top_<n>_<field>
    // --------------------------------------------------------------

    @Test
    @DisplayName("%OB_top_position% returns the 1-based rank of the queried PlayerInfo")
    void topPositionOneBasedRank() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 7;
        PlayerInfo pb = new PlayerInfo(b); pb.lvl = 3;
        PlayerInfo.replaceAll(Arrays.asList(pa, pb));

        // Top-list sorts by lvl desc -> a is rank 1, b is rank 2.
        assertThat(obp.onRequest(offline(a), "top_position")).isEqualTo("1");
        assertThat(obp.onRequest(offline(b), "top_position")).isEqualTo("2");
    }

    @Test
    @DisplayName("%OB_top_position% returns the [None] sentinel for an unranked viewer")
    void topPositionUnrankedReturnsNone() {
        // A UUID not registered in PlayerInfo.list -> getId returns -1
        // -> PlayerInfo.get(uuid) returns the not_found sentinel whose
        // uuid is null -> Oneblock.getTopPosition returns -1 -> the
        // [None] sentinel is rendered.
        UUID stranger = UUID.randomUUID();
        putPlayer(UUID.randomUUID(), 5, 0);
        assertThat(obp.onRequest(offline(stranger), "top_position")).isEqualTo("[None]");
    }

    @Test
    @DisplayName("%OB_top_1_lvl% returns the top player's lvl as a string")
    void topNLvlReturnsLevel() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 9;
        PlayerInfo pb = new PlayerInfo(b); pb.lvl = 4;
        PlayerInfo.replaceAll(Arrays.asList(pa, pb));

        assertThat(obp.onRequest(offline(a), "top_1_lvl")).isEqualTo("9");
        assertThat(obp.onRequest(offline(a), "top_2_lvl")).isEqualTo("4");
    }

    @Test
    @DisplayName("%OB_top_1_name% delegates to Bukkit.getOfflinePlayer for the leader's nick")
    void topNNameDelegatesToBukkit() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        OfflinePlayer leader = mock(OfflinePlayer.class);
        when(leader.getName()).thenReturn("Leader");

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getOfflinePlayer(a)).thenReturn(leader);
            assertThat(obp.onRequest(offline(a), "top_1_name")).isEqualTo("Leader");
        }
    }

    @Test
    @DisplayName("%OB_top_<n>_name% returns the [None] sentinel when the slot is unfilled")
    void topNUnfilledReturnsNone() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        // top_2 doesn't exist (only 1 island registered) -> Oneblock.getTop
        // returns the not_found sentinel whose uuid is null -> [None].
        assertThat(obp.onRequest(offline(a), "top_2_name")).isEqualTo("[None]");
    }

    @Test
    @DisplayName("%OB_top_X_name% with a non-numeric position returns null (NumberFormatException caught)")
    void topNonNumericReturnsNull() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        assertThat(obp.onRequest(offline(a), "top_X_name")).isNull();
    }

    @Test
    @DisplayName("%OB_top_0_name% (out of bounds: 0 -> -1 after the -1 shift) returns null")
    void topZeroOutOfBoundsReturnsNull() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        assertThat(obp.onRequest(offline(a), "top_0_name")).isNull();
    }

    @Test
    @DisplayName("%OB_top_11_name% (out of bounds: only top-10 supported) returns null")
    void topElevenOutOfBoundsReturnsNull() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        assertThat(obp.onRequest(offline(a), "top_11_name")).isNull();
    }

    @Test
    @DisplayName("%OB_top_1_garbage% with an unknown sub-field returns null")
    void topUnknownSubFieldReturnsNull() {
        UUID a = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 5;
        PlayerInfo.replaceAll(Collections.singletonList(pa));

        assertThat(obp.onRequest(offline(a), "top_1_garbage")).isNull();
    }

    // --------------------------------------------------------------
    // _by_position recursion
    // --------------------------------------------------------------

    @Test
    @DisplayName("%OB_..._by_position% returns the [None] sentinel for a non-Player OfflinePlayer")
    void byPositionNonPlayerReturnsNone() {
        UUID viewer = UUID.randomUUID();
        putPlayer(UUID.randomUUID(), 5, 0);
        // offline(viewer) is an OfflinePlayer mock; it is NOT an
        // instance of Player, so the early-return branch fires.
        assertThat(obp.onRequest(offline(viewer), "lvl_by_position")).isEqualTo("[None]");
    }

    @Test
    @DisplayName("%OB_lvl_by_position% routes to the nearest island's owner's lvl via Bukkit.getOfflinePlayer recursion")
    void byPositionRoutesToOwnerLvl() {
        UUID owner = UUID.randomUUID();
        // Island 0 has owner=owner, lvl=4.
        putPlayer(owner, 4, 0);

        // Player viewing the placeholder is at some location; the mock
        // plugin's findNearestRegionId returns 0 -> island 0's owner is
        // resolved. The recursive onRequest call hits the 'lvl' case
        // and returns "4".
        Player viewer = mock(Player.class);
        World world = mock(World.class);
        Location loc = new Location(world, 0, 64, 0);
        when(viewer.getLocation()).thenReturn(loc);
        when(viewer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(Oneblock.plugin.findNearestRegionId(loc)).thenReturn(0);

        OfflinePlayer ownerAsOffline = offline(owner);

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getOfflinePlayer(owner)).thenReturn(ownerAsOffline);
            assertThat(obp.onRequest(viewer, "lvl_by_position")).isEqualTo("4");
        }
    }
}
