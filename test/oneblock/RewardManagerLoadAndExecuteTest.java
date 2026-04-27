package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Coverage for {@link RewardManager#loadRewards()} and the
 * dispatch-side of {@link RewardManager#executeRewards(Player, int, String)}.
 * The existing {@link RewardManagerTest} already pins the
 * {@code SAFE_PLAYER_NAME} regex contract; this slice fills the
 * surrounding parse + placeholder substitution gap (~92% of the
 * remaining miss budget on the class).
 *
 * <h2>Plugin mock + tempdir</h2>
 *
 * <p>{@code loadRewards()} reads
 * {@code new File(Oneblock.plugin.getDataFolder(), "rewards.yml")}.
 * The fixture installs a Mockito-mocked {@link Oneblock} whose
 * {@code getDataFolder()} returns the class-scoped {@link TempDir} so
 * each test can stage its own {@code rewards.yml} on disk. The mock
 * is restored in {@link #restorePlugin}.
 *
 * <h2>Bukkit dispatch capture</h2>
 *
 * <p>{@code executeRewards} ultimately calls
 * {@link Bukkit#dispatchCommand(org.bukkit.command.CommandSender, String)}
 * which would NPE on {@code Bukkit.server == null}. Tests wrap the
 * call in {@link Mockito#mockStatic(Class) Mockito.mockStatic(Bukkit.class)}
 * and capture the dispatched command strings via an
 * {@link ArgumentCaptor}. The substitution contract
 * ({@code %nick%}, {@code %lvl_number%}, {@code %lvl_name%}) is
 * asserted on the captured strings; the actual Bukkit dispatch is a
 * harness concern and stays out of the unit boundary.
 */
class RewardManagerLoadAndExecuteTest {

    @TempDir
    static Path tempDir;

    private static Oneblock savedPlugin;
    private static Logger pluginLogger;

    @BeforeAll
    static void installMockPlugin() {
        savedPlugin = Oneblock.plugin;
        pluginLogger = mock(Logger.class);
        Oneblock mockPlugin = mock(Oneblock.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getLogger()).thenReturn(pluginLogger);
        Oneblock.plugin = mockPlugin;
    }

    @AfterAll
    static void restorePlugin() {
        Oneblock.plugin = savedPlugin;
    }

    @BeforeEach
    void cleanup() {
        // Drop any rewards.yml from the previous test so a missing-file
        // scenario sees a missing file.
        java.io.File f = new java.io.File(tempDir.toFile(), "rewards.yml");
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        Mockito.reset(pluginLogger);
    }

    private static void writeRewardsYaml(String body) throws Exception {
        try (FileWriter w = new FileWriter(new java.io.File(tempDir.toFile(), "rewards.yml"))) {
            w.write(body);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readAllRewards(RewardManager rm) throws Exception {
        Field f = RewardManager.class.getDeclaredField("allRewards");
        f.setAccessible(true);
        return (List<String>) f.get(rm);
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, List<String>> readLevelRewards(RewardManager rm) throws Exception {
        Field f = RewardManager.class.getDeclaredField("levelRewards");
        f.setAccessible(true);
        return (Map<Integer, List<String>>) f.get(rm);
    }

    // --------------------------------------------------------------
    // loadRewards: parse-side coverage
    // --------------------------------------------------------------

    @Test
    @DisplayName("loadRewards: 'all' list populates allRewards with colour-translated strings")
    void allListPopulatesAllRewards() throws Exception {
        writeRewardsYaml(
            "all:\n" +
            "  - \"&agive %nick% diamond\"\n" +
            "  - \"&bsay welcome %nick%\"\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();

        List<String> all = readAllRewards(rm);
        assertThat(all).hasSize(2);
        // & -> section sign after Utils.translateColorCodes
        assertThat(all.get(0)).startsWith("\u00a7a").contains("give %nick% diamond");
        assertThat(all.get(1)).startsWith("\u00a7b").contains("say welcome %nick%");
    }

    @Test
    @DisplayName("loadRewards: 'levels.<int>' map populates levelRewards keyed by parsed int")
    void levelsMapPopulatesLevelRewards() throws Exception {
        writeRewardsYaml(
            "levels:\n" +
            "  3:\n" +
            "    - \"give %nick% iron_ingot 5\"\n" +
            "  10:\n" +
            "    - \"give %nick% gold_ingot 10\"\n" +
            "    - \"broadcast %nick% reached level 10\"\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Map<Integer, List<String>> levels = readLevelRewards(rm);
        assertThat(levels).containsKeys(3, 10);
        assertThat(levels.get(3)).containsExactly("give %nick% iron_ingot 5");
        assertThat(levels.get(10)).containsExactly(
                "give %nick% gold_ingot 10",
                "broadcast %nick% reached level 10");
    }

    @Test
    @DisplayName("loadRewards: non-numeric level key is logged and skipped, valid keys keep loading")
    void nonNumericLevelKeyIsLoggedAndSkipped() throws Exception {
        writeRewardsYaml(
            "levels:\n" +
            "  foo:\n" +
            "    - \"this/should/never/run\"\n" +
            "  4:\n" +
            "    - \"give %nick% emerald\"\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Map<Integer, List<String>> levels = readLevelRewards(rm);
        assertThat(levels).containsOnlyKeys(4);
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger, atLeastOnce()).warning(msg.capture());
        assertThat(msg.getAllValues())
            .anyMatch(s -> s.contains("Invalid level number") && s.contains("foo"));
    }

    @Test
    @DisplayName("loadRewards: empty YAML produces empty maps without throwing")
    void emptyYamlProducesEmptyMaps() throws Exception {
        writeRewardsYaml("# nothing here\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();

        assertThat(readAllRewards(rm)).isEmpty();
        assertThat(readLevelRewards(rm)).isEmpty();
    }

    @Test
    @DisplayName("loadRewards: missing 'all' / 'levels' sections produce empty maps without throwing")
    void missingSectionsProducesEmptyMaps() throws Exception {
        // Some unrelated keys but no 'all' or 'levels' sections - the
        // isList / isConfigurationSection guards must short-circuit
        // cleanly without an NPE.
        writeRewardsYaml("unrelated_key: 42\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();

        assertThat(readAllRewards(rm)).isEmpty();
        assertThat(readLevelRewards(rm)).isEmpty();
    }

    @Test
    @DisplayName("loadRewards: a second call clears the previous state (reload semantics)")
    void reloadClearsPreviousState() throws Exception {
        // First load: 2 general + 1 level-specific.
        writeRewardsYaml(
            "all:\n" +
            "  - \"first1\"\n" +
            "  - \"first2\"\n" +
            "levels:\n" +
            "  5:\n" +
            "    - \"first_level5\"\n");

        RewardManager rm = new RewardManager();
        rm.loadRewards();
        assertThat(readAllRewards(rm)).hasSize(2);
        assertThat(readLevelRewards(rm)).containsOnlyKeys(5);

        // Second load: completely different shape.
        writeRewardsYaml(
            "all:\n" +
            "  - \"second_only\"\n");

        rm.reload();
        assertThat(readAllRewards(rm)).containsExactly("second_only");
        assertThat(readLevelRewards(rm)).isEmpty();
    }

    // --------------------------------------------------------------
    // executeRewards: dispatch-side coverage with Bukkit captured
    // --------------------------------------------------------------

    @Test
    @DisplayName("executeRewards: general 'all' rewards fire with %nick% / %lvl_number% / %lvl_name% substituted")
    void executeRewardsAllListWithPlaceholders() throws Exception {
        writeRewardsYaml(
            "all:\n" +
            "  - \"give %nick% diamond %lvl_number%\"\n" +
            "  - \"broadcast %nick% reached %lvl_name%\"\n");
        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Notch");

        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        List<String> dispatched = new ArrayList<>();

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(Bukkit::getConsoleSender).thenReturn(console);
            bk.when(() -> Bukkit.dispatchCommand(org.mockito.ArgumentMatchers.any(),
                                                 org.mockito.ArgumentMatchers.anyString()))
              .thenAnswer(inv -> { dispatched.add(inv.getArgument(1)); return true; });

            rm.executeRewards(player, 4, "Silver");
        }

        assertThat(dispatched).containsExactly(
            "give Notch diamond 4",
            "broadcast Notch reached Silver");
    }

    @Test
    @DisplayName("executeRewards: level-specific rewards fire only when the player's level matches")
    void executeRewardsLevelSpecificMatchesLevel() throws Exception {
        writeRewardsYaml(
            "levels:\n" +
            "  5:\n" +
            "    - \"give %nick% gold\"\n" +
            "  10:\n" +
            "    - \"give %nick% diamond\"\n");
        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Notch");

        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        List<String> dispatched = new ArrayList<>();

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(Bukkit::getConsoleSender).thenReturn(console);
            bk.when(() -> Bukkit.dispatchCommand(org.mockito.ArgumentMatchers.any(),
                                                 org.mockito.ArgumentMatchers.anyString()))
              .thenAnswer(inv -> { dispatched.add(inv.getArgument(1)); return true; });

            rm.executeRewards(player, 10, "Diamond");
        }

        // Only the level-10 reward fires; the level-5 one does not.
        assertThat(dispatched).containsExactly("give Notch diamond");
    }

    @Test
    @DisplayName("executeRewards: a level with no configured rewards dispatches nothing (and doesn't throw)")
    void executeRewardsWithNoMatchingLevelDispatchesNothing() throws Exception {
        writeRewardsYaml(
            "levels:\n" +
            "  5:\n" +
            "    - \"give %nick% gold\"\n");
        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Notch");

        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        List<String> dispatched = new ArrayList<>();

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(Bukkit::getConsoleSender).thenReturn(console);
            bk.when(() -> Bukkit.dispatchCommand(org.mockito.ArgumentMatchers.any(),
                                                 org.mockito.ArgumentMatchers.anyString()))
              .thenAnswer(inv -> { dispatched.add(inv.getArgument(1)); return true; });

            rm.executeRewards(player, 99, "Out-of-range");
        }

        assertThat(dispatched).isEmpty();
    }

    @Test
    @DisplayName("executeRewards: unsafe player name dispatches nothing and logs a warning")
    void executeRewardsUnsafeNameSkipped() throws Exception {
        writeRewardsYaml(
            "all:\n" +
            "  - \"give %nick% diamond\"\n");
        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Player player = mock(Player.class);
        // Semicolon-injection name - SAFE_PLAYER_NAME rejects it.
        when(player.getName()).thenReturn("evil;name");

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            // No dispatchCommand stubbing: any call would default to
            // returning false, but we'll verify it's never invoked.
            rm.executeRewards(player, 1, "Bronze");
            bk.verify(() -> Bukkit.dispatchCommand(org.mockito.ArgumentMatchers.any(),
                                                    org.mockito.ArgumentMatchers.anyString()),
                       never());
        }

        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
        verify(pluginLogger, atLeastOnce()).warning(msg.capture());
        assertThat(msg.getAllValues())
            .anyMatch(s -> s.contains("unsafe name") && s.contains("evil;name"));
    }

    @Test
    @DisplayName("executeRewards: null player name is treated as unsafe (skipped + logged)")
    void executeRewardsNullNameSkipped() throws Exception {
        writeRewardsYaml(
            "all:\n" +
            "  - \"give %nick% diamond\"\n");
        RewardManager rm = new RewardManager();
        rm.loadRewards();

        Player player = mock(Player.class);
        when(player.getName()).thenReturn(null);

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            rm.executeRewards(player, 1, "Bronze");
            bk.verify(() -> Bukkit.dispatchCommand(org.mockito.ArgumentMatchers.any(),
                                                    org.mockito.ArgumentMatchers.anyString()),
                       never());
        }
        verify(pluginLogger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.anyString());
    }
}
