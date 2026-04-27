package oneblock.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.utils.Utils;

/**
 * Coverage for {@link LegacyYamlPlayerDataStore}, the one-shot
 * pre-1.x YAML player-data migrator. Phase 5.3 lifts this class from
 * {@code 0%} line coverage. The migrator runs only on first start
 * when no {@code PlData.json} exists yet, but a regression here
 * silently drops every legacy server's player roster on its
 * post-upgrade reboot - the precise data-loss class Phase 5 was
 * sequenced to fence.
 *
 * <p>The class-load-time evaluation of the {@code static File f}
 * field is handled by the same trick used in
 * {@link JsonPlayerDataStoreTest}: install a Mockito-mocked
 * {@link Oneblock#plugin} whose {@code getDataFolder()} returns a
 * class-scoped {@link TempDir}, then force the class to initialise
 * via {@link Class#forName} before any test method runs.
 *
 * <p>Nick-to-UUID resolution funnels through
 * {@link Utils#getOfflinePlayerByName} which dereferences
 * {@link org.bukkit.Bukkit#getOfflinePlayers()} and only works on a
 * real server. Each test that exercises a populated legacy file
 * therefore wraps the {@code read()} call in
 * {@link MockedStatic Mockito.mockStatic(Utils.class)} and stubs the
 * resolver to return either a fake {@link OfflinePlayer} (for resolved
 * nicks) or {@code null} (for the unresolved-nick warning branch).
 */
class LegacyYamlPlayerDataStoreTest {

    @TempDir
    static Path tempDir;

    private static Oneblock savedPlugin;
    private static Logger pluginLogger;

    @BeforeAll
    static void installMockPlugin() throws Exception {
        savedPlugin = Oneblock.plugin;
        pluginLogger = mock(Logger.class);
        Oneblock mockPlugin = mock(Oneblock.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getLogger()).thenReturn(pluginLogger);
        Oneblock.plugin = mockPlugin;
        Class.forName("oneblock.storage.LegacyYamlPlayerDataStore");
    }

    @AfterAll
    static void restorePlugin() {
        Oneblock.plugin = savedPlugin;
    }

    @BeforeEach
    void cleanFile() {
        if (LegacyYamlPlayerDataStore.f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            LegacyYamlPlayerDataStore.f.delete();
        }
        Mockito.reset(pluginLogger);
    }

    private static void writeYaml(String content) throws Exception {
        try (FileWriter w = new FileWriter(LegacyYamlPlayerDataStore.f)) {
            w.write(content);
        }
    }

    /** Build a fake {@link OfflinePlayer} that just yields {@code uuid} for {@code getUniqueId()}. */
    private static OfflinePlayer fakeOfflinePlayer(UUID uuid) {
        OfflinePlayer p = mock(OfflinePlayer.class);
        when(p.getUniqueId()).thenReturn(uuid);
        return p;
    }

    // --------------------------------------------------------------
    // Early-return guards
    // --------------------------------------------------------------

    @Test
    @DisplayName("missing PlData.yml -> empty list (no warning, no throw)")
    void missingFileReturnsEmpty() {
        ArrayList<PlayerInfo> read = LegacyYamlPlayerDataStore.read();
        assertThat(read).isEmpty();
        // The file-missing path is the most common case (a server with
        // no legacy data at all) and intentionally stays silent.
        Mockito.verifyNoInteractions(pluginLogger);
    }

    @Test
    @DisplayName("YAML missing 'id' header -> empty list, no throw")
    void missingIdHeaderReturnsEmpty() throws Exception {
        // No id key, no nick markers. The early return at
        // !data.isInt("id") fires.
        writeYaml("foo: bar\n_unused: 0\n");

        ArrayList<PlayerInfo> read = LegacyYamlPlayerDataStore.read();

        assertThat(read).isEmpty();
    }

    @Test
    @DisplayName("YAML with id=0 (no islands) -> empty list")
    void zeroIslandsReturnsEmpty() throws Exception {
        writeYaml("id: 0\n");

        ArrayList<PlayerInfo> read = LegacyYamlPlayerDataStore.read();

        assertThat(read).isEmpty();
    }

    // --------------------------------------------------------------
    // Happy-path: nick resolves -> PlayerInfo populated
    // --------------------------------------------------------------

    @Test
    @DisplayName("single legacy entry: nick resolves to UUID, lvl + breaks parsed")
    void singleEntryHappyPath() throws Exception {
        UUID aliceUuid = UUID.randomUUID();
        writeYaml(
            "id: 1\n" +
            "_alice: 0\n" +
            "Score_0: 7\n" +
            "ScSlom_0: 23\n");

        // Pre-build the fake OfflinePlayer OUTSIDE the MockedStatic
        // block: the inner when(p.getUniqueId()).thenReturn(...) chain
        // collides with Mockito's currently-active static stubbing
        // session and triggers UnfinishedStubbingException.
        OfflinePlayer alice = fakeOfflinePlayer(aliceUuid);
        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("alice")).thenReturn(alice);
            read = LegacyYamlPlayerDataStore.read();
        }

        assertThat(read).hasSize(1);
        PlayerInfo p = read.get(0);
        assertThat(p.uuid).isEqualTo(aliceUuid);
        assertThat(p.lvl).isEqualTo(7);
        assertThat(p.breaks).isEqualTo(23);
    }

    @Test
    @DisplayName("multi-island legacy file: each island gets its own PlayerInfo with the right lvl/breaks")
    void multipleIslandsResolveIndependently() throws Exception {
        UUID aliceUuid = UUID.randomUUID();
        UUID bobUuid = UUID.randomUUID();
        writeYaml(
            "id: 2\n" +
            "_alice: 0\n" +
            "Score_0: 1\n" +
            "ScSlom_0: 5\n" +
            "_bob: 1\n" +
            "Score_1: 3\n" +
            "ScSlom_1: 17\n");

        OfflinePlayer alice = fakeOfflinePlayer(aliceUuid);
        OfflinePlayer bob = fakeOfflinePlayer(bobUuid);
        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("alice")).thenReturn(alice);
            ms.when(() -> Utils.getOfflinePlayerByName("bob")).thenReturn(bob);
            read = LegacyYamlPlayerDataStore.read();
        }

        assertThat(read).hasSize(2);
        assertThat(read).extracting(p -> p.uuid)
                        .containsExactly(aliceUuid, bobUuid);
        assertThat(read).extracting(p -> p.lvl).containsExactly(1, 3);
        assertThat(read).extracting(p -> p.breaks).containsExactly(5, 17);
    }

    @Test
    @DisplayName("legacy entry with missing Score_/ScSlom_ keys defaults lvl=0 / breaks=0")
    void missingScoreKeysDefaultToZero() throws Exception {
        UUID aliceUuid = UUID.randomUUID();
        // No Score_0 / ScSlom_0 keys: the data.isInt(...) guards fall
        // through and the freshly-constructed PlayerInfo keeps its
        // default 0 fields.
        writeYaml(
            "id: 1\n" +
            "_alice: 0\n");

        OfflinePlayer alice = fakeOfflinePlayer(aliceUuid);
        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("alice")).thenReturn(alice);
            read = LegacyYamlPlayerDataStore.read();
        }

        assertThat(read).hasSize(1);
        PlayerInfo p = read.get(0);
        assertThat(p.uuid).isEqualTo(aliceUuid);
        assertThat(p.lvl).isZero();
        assertThat(p.breaks).isZero();
    }

    // --------------------------------------------------------------
    // Failure paths: unresolved nicks, gaps in nick coverage
    // --------------------------------------------------------------

    @Test
    @DisplayName("unresolved nick logs a row-skip warning and the row is dropped")
    void unresolvedNickIsLoggedAndSkipped() throws Exception {
        writeYaml(
            "id: 1\n" +
            "_ghost: 0\n");

        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("ghost"))
              .thenReturn(null);
            read = LegacyYamlPlayerDataStore.read();
        }

        assertThat(read).isEmpty();
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger, atLeastOnce()).warning(msg.capture());
        assertThat(msg.getAllValues())
            .anyMatch(s -> s.contains("unresolved nick") && s.contains("ghost"));
    }

    @Test
    @DisplayName("OfflinePlayer with null UUID is treated as unresolved")
    void offlinePlayerWithNullUuidIsUnresolved() throws Exception {
        writeYaml(
            "id: 1\n" +
            "_alice: 0\n");

        OfflinePlayer ghost = mock(OfflinePlayer.class);
        when(ghost.getUniqueId()).thenReturn(null);

        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("alice")).thenReturn(ghost);
            read = LegacyYamlPlayerDataStore.read();
        }

        assertThat(read).isEmpty();
        verify(pluginLogger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("island id with no matching nick (gap) is silently skipped without a warning")
    void islandWithoutMatchingNickIsSilentlySkipped() throws Exception {
        // id=2 but only _alice maps to slot 0 - slot 1 has no nick. The
        // _nick.equals(\"\") branch fires for slot 1 and 'continue's
        // without a warning (legacy quirk: the original ReadOldData
        // did the same).
        UUID aliceUuid = UUID.randomUUID();
        writeYaml(
            "id: 2\n" +
            "_alice: 0\n" +
            "Score_0: 9\n");

        OfflinePlayer alice = fakeOfflinePlayer(aliceUuid);
        ArrayList<PlayerInfo> read;
        try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
            ms.when(() -> Utils.getOfflinePlayerByName("alice")).thenReturn(alice);
            read = LegacyYamlPlayerDataStore.read();
        }

        // Only the resolved island is in the result; no warning was
        // emitted for the gap.
        assertThat(read).hasSize(1);
        assertThat(read.get(0).uuid).isEqualTo(aliceUuid);
        Mockito.verifyNoInteractions(pluginLogger);
    }
}
