package oneblock.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import oneblock.Oneblock;
import oneblock.PlayerInfo;

/**
 * Round-trip coverage for {@link DatabaseManager}, the SQL-backed
 * storage path used when {@code database.type} is set to {@code h2}
 * or {@code mysql} in {@code config.yml}. Phase 5.6 lifts the class
 * from {@code 0%} line coverage; a regression here drops every
 * SQL-backed server's roster on the post-upgrade reboot - same
 * data-loss class as Phase 5.3.
 *
 * <p>The tests use H2 in <b>file mode</b> via the production
 * {@link DatabaseManager#initialize()} path, not an in-memory shim.
 * H2 is on the runtime classpath of the project (pom.xml: {@code
 * com.h2database:h2:2.4.240} at runtime scope), and the production
 * code already builds the H2 JDBC URL from
 * {@code Oneblock.plugin.getDataFolder()}. We make
 * {@code getDataFolder()} return a per-test {@link TempDir} so each
 * test starts from a clean H2 database file.
 *
 * <p>Per-test isolation:
 * <ul>
 *   <li>{@link #installPerTestPlugin} installs a fresh Mockito mock of
 *       {@link Oneblock} with {@code getDataFolder()} -> {@code @TempDir}
 *       and {@code getLogger()} -> mock {@link Logger}, then resets
 *       every {@code DatabaseManager} static config field to its
 *       documented default and calls {@link DatabaseManager#initialize()}.</li>
 *   <li>{@link #closePool} releases the Hikari pool (and therefore
 *       H2's file lock) so the next test can use a fresh tempdir.</li>
 *   <li>Class-level {@link #snapshotStatics} / {@link #restoreStatics}
 *       captures and restores the original static values so this test
 *       class doesn't leak DB config into other test classes that
 *       run later in the same JVM.</li>
 * </ul>
 *
 * <p>Validation-error paths ({@code dbType=mysql} with malformed host /
 * port / database name) exercise the production allow-list patterns
 * without needing a real MySQL: the {@code IllegalArgumentException}
 * is thrown inside the {@code initialize()} try/catch, which logs at
 * {@code SEVERE} and sets {@code dataSource = null}. Tests assert
 * post-conditions ({@link DatabaseManager#isConnected()} is false)
 * and verify the warning text via the mock logger.
 */
class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    private static Oneblock savedPlugin;
    private static String savedDbType;
    private static String savedHost;
    private static int savedPort;
    private static String savedDatabase;
    private static String savedUsername;
    private static String savedPassword;
    private static boolean savedUseSSL;
    private static boolean savedAutoReconnect;

    private Logger pluginLogger;

    @BeforeAll
    static void snapshotStatics() {
        savedPlugin = Oneblock.plugin;
        savedDbType = DatabaseManager.dbType;
        savedHost = DatabaseManager.host;
        savedPort = DatabaseManager.port;
        savedDatabase = DatabaseManager.database;
        savedUsername = DatabaseManager.username;
        savedPassword = DatabaseManager.password;
        savedUseSSL = DatabaseManager.useSSL;
        savedAutoReconnect = DatabaseManager.autoReconnect;
    }

    @AfterAll
    static void restoreStatics() {
        Oneblock.plugin = savedPlugin;
        DatabaseManager.dbType = savedDbType;
        DatabaseManager.host = savedHost;
        DatabaseManager.port = savedPort;
        DatabaseManager.database = savedDatabase;
        DatabaseManager.username = savedUsername;
        DatabaseManager.password = savedPassword;
        DatabaseManager.useSSL = savedUseSSL;
        DatabaseManager.autoReconnect = savedAutoReconnect;
    }

    @BeforeEach
    void installPerTestPlugin() {
        pluginLogger = mock(Logger.class);
        Oneblock mockPlugin = mock(Oneblock.class);
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(mockPlugin.getLogger()).thenReturn(pluginLogger);
        Oneblock.plugin = mockPlugin;

        // Reset to the per-test default (h2). Specific tests override
        // dbType to "mysql" or "json" before calling initialize().
        DatabaseManager.dbType = "h2";
        DatabaseManager.host = "localhost";
        DatabaseManager.port = 3306;
        DatabaseManager.database = "oneblock";
        DatabaseManager.username = "root";
        DatabaseManager.password = "";
        DatabaseManager.useSSL = false;
        DatabaseManager.autoReconnect = true;
    }

    @AfterEach
    void closePool() {
        DatabaseManager.close();
    }

    // --------------------------------------------------------------
    // initialize: dbType = json / h2 / mysql
    // --------------------------------------------------------------

    @Test
    @DisplayName("initialize with dbType=json is a no-op (isConnected stays false)")
    void initializeJsonIsNoOp() {
        DatabaseManager.dbType = "json";
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();
        verify(pluginLogger, atLeastOnce()).info(
            org.mockito.ArgumentMatchers.contains("Using JSON storage"));
    }

    @Test
    @DisplayName("initialize with dbType=h2 opens the pool (isConnected is true)")
    void initializeH2OpensPool() {
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isTrue();
        verify(pluginLogger, atLeastOnce()).info(
            org.mockito.ArgumentMatchers.contains("Database initialized successfully"));
    }

    @Test
    @DisplayName("initialize with dbType=mysql + invalid host fails the allow-list and falls back (isConnected false)")
    void initializeMysqlInvalidHost() {
        DatabaseManager.dbType = "mysql";
        DatabaseManager.host = "evil host;DROP TABLE foo;--"; // contains spaces and semicolons
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();
        // The IllegalArgumentException is logged at SEVERE through
        // logger.log(level, msg, throwable) - we verify any log call
        // happened and that the fallback "Using JSON storage" message
        // is present.
        verify(pluginLogger, atLeastOnce()).info(
            org.mockito.ArgumentMatchers.contains("Using JSON storage"));
    }

    @Test
    @DisplayName("initialize with dbType=mysql + out-of-range port falls back")
    void initializeMysqlInvalidPort() {
        DatabaseManager.dbType = "mysql";
        DatabaseManager.port = 70000;
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();
        verify(pluginLogger, atLeastOnce()).info(
            org.mockito.ArgumentMatchers.contains("Using JSON storage"));
    }

    @Test
    @DisplayName("initialize with dbType=mysql + invalid database name falls back")
    void initializeMysqlInvalidDatabase() {
        DatabaseManager.dbType = "mysql";
        DatabaseManager.database = "schema; DROP TABLE foo";
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();
    }

    // --------------------------------------------------------------
    // load + save round-trip on H2
    // --------------------------------------------------------------

    @Test
    @DisplayName("h2 round-trip: empty database produces an empty list")
    void emptyDatabaseLoadsEmptyList() {
        DatabaseManager.initialize();
        List<PlayerInfo> loaded = DatabaseManager.load();
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("h2 round-trip: single owner with lvl/breaks roundtrips identity-equally")
    void singleOwnerRoundTrip() {
        DatabaseManager.initialize();

        UUID owner = UUID.randomUUID();
        PlayerInfo p = new PlayerInfo(owner);
        p.lvl = 7;
        p.breaks = 23;

        boolean saved = DatabaseManager.save(Collections.singletonList(p));
        assertThat(saved).isTrue();

        List<PlayerInfo> loaded = DatabaseManager.load();
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).uuid).isEqualTo(owner);
        assertThat(loaded.get(0).lvl).isEqualTo(7);
        assertThat(loaded.get(0).breaks).isEqualTo(23);
        assertThat(loaded.get(0).allowVisit).isFalse();
        assertThat(loaded.get(0).uuids).isEmpty();
    }

    @Test
    @DisplayName("h2 round-trip: allowVisit=true is persisted and restored truthy")
    void allowVisitRoundTrip() {
        DatabaseManager.initialize();

        UUID owner = UUID.randomUUID();
        PlayerInfo p = new PlayerInfo(owner);
        p.allowVisit = true;

        DatabaseManager.save(Collections.singletonList(p));
        List<PlayerInfo> loaded = DatabaseManager.load();

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).allowVisit).isTrue();
    }

    @Test
    @DisplayName("h2 round-trip: invited UUIDs are persisted as CSV and round-tripped in order")
    void invitedUuidsCsvRoundTrip() {
        DatabaseManager.initialize();

        UUID owner = UUID.randomUUID();
        UUID inv1 = UUID.randomUUID();
        UUID inv2 = UUID.randomUUID();
        PlayerInfo p = new PlayerInfo(owner);
        p.uuids.add(inv1);
        p.uuids.add(inv2);

        DatabaseManager.save(Collections.singletonList(p));
        List<PlayerInfo> loaded = DatabaseManager.load();

        assertThat(loaded.get(0).uuids).containsExactly(inv1, inv2);
    }

    @Test
    @DisplayName("h2 round-trip: a null-slot (uuid==null) entry is preserved at its position")
    void nullSlotRoundTrip() {
        DatabaseManager.initialize();

        UUID owner = UUID.randomUUID();
        PlayerInfo realOwner = new PlayerInfo(owner);
        PlayerInfo emptySlot = new PlayerInfo(null);

        DatabaseManager.save(Arrays.asList(realOwner, emptySlot));
        List<PlayerInfo> loaded = DatabaseManager.load();

        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).uuid).isEqualTo(owner);
        assertThat(loaded.get(1).uuid).isNull();
    }

    @Test
    @DisplayName("h2 round-trip: multi-island layout preserves slot positions")
    void multiIslandPositionsPreserved() {
        DatabaseManager.initialize();

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        PlayerInfo pa = new PlayerInfo(a); pa.lvl = 1;
        PlayerInfo pb = new PlayerInfo(b); pb.lvl = 2;
        PlayerInfo pc = new PlayerInfo(c); pc.lvl = 3;

        DatabaseManager.save(Arrays.asList(pa, pb, pc));
        List<PlayerInfo> loaded = DatabaseManager.load();

        assertThat(loaded).extracting(p -> p.uuid).containsExactly(a, b, c);
        assertThat(loaded).extracting(p -> p.lvl).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("h2 UPSERT: a second save with updated lvl overwrites the previous row")
    void saveUpsertsExistingRow() {
        DatabaseManager.initialize();

        UUID owner = UUID.randomUUID();
        PlayerInfo p1 = new PlayerInfo(owner); p1.lvl = 1;
        DatabaseManager.save(Collections.singletonList(p1));
        assertThat(DatabaseManager.load().get(0).lvl).isEqualTo(1);

        // Same UUID at the same slot, lvl bumped.
        PlayerInfo p2 = new PlayerInfo(owner); p2.lvl = 9;
        DatabaseManager.save(Collections.singletonList(p2));

        List<PlayerInfo> loaded = DatabaseManager.load();
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).lvl).isEqualTo(9);
    }

    // --------------------------------------------------------------
    // load + save guards
    // --------------------------------------------------------------

    @Test
    @DisplayName("save returns false when the pool is not connected (json mode)")
    void saveReturnsFalseWhenDisconnected() {
        DatabaseManager.dbType = "json";
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();

        UUID owner = UUID.randomUUID();
        boolean saved = DatabaseManager.save(Collections.singletonList(new PlayerInfo(owner)));
        assertThat(saved).isFalse();
    }

    @Test
    @DisplayName("save returns false when the player list is null")
    void saveReturnsFalseOnNullList() {
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isTrue();
        assertThat(DatabaseManager.save(null)).isFalse();
    }

    @Test
    @DisplayName("load on a disconnected pool returns an empty list")
    void loadOnDisconnectedReturnsEmpty() {
        DatabaseManager.dbType = "json";
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isFalse();
        assertThat(DatabaseManager.load()).isEmpty();
    }

    @Test
    @DisplayName("save accepts an empty list, commits an empty transaction and returns true")
    void emptyListSavesAsTrue() {
        DatabaseManager.initialize();
        assertThat(DatabaseManager.save(new ArrayList<>())).isTrue();
        assertThat(DatabaseManager.load()).isEmpty();
    }

    @Test
    @DisplayName("close releases the pool (isConnected becomes false)")
    void closeReleasesPool() {
        DatabaseManager.initialize();
        assertThat(DatabaseManager.isConnected()).isTrue();
        DatabaseManager.close();
        assertThat(DatabaseManager.isConnected()).isFalse();
    }

    @Test
    @DisplayName("save tolerates null PlayerInfo entries inside the list (skips them, returns true)")
    void saveSkipsNullEntries() {
        DatabaseManager.initialize();

        UUID a = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        List<PlayerInfo> mixed = new ArrayList<>();
        mixed.add(new PlayerInfo(a));
        mixed.add(null); // gap in the slot list
        mixed.add(new PlayerInfo(c));

        boolean saved = DatabaseManager.save(mixed);
        assertThat(saved).isTrue();

        List<PlayerInfo> loaded = DatabaseManager.load();
        // Only the non-null slots were written; index 1 is absent.
        assertThat(loaded).extracting(p -> p.uuid)
                          .containsExactlyInAnyOrder(a, c);
    }

    // --------------------------------------------------------------
    // Validation-message capture
    // --------------------------------------------------------------

    @Test
    @DisplayName("initialize logs the validation-error type when MySQL config is malformed")
    void mysqlValidationErrorLogged() {
        DatabaseManager.dbType = "mysql";
        DatabaseManager.host = "evil host"; // contains a space
        DatabaseManager.initialize();

        // The logger.log(SEVERE, "Failed to initialize Database", throwable)
        // call records the IllegalArgumentException. Capture the
        // throwable parameter to confirm the right validation fired.
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(pluginLogger, atLeastOnce())
            .log(org.mockito.ArgumentMatchers.eq(java.util.logging.Level.SEVERE),
                 org.mockito.ArgumentMatchers.eq("Failed to initialize Database"),
                 captor.capture());
        Throwable cause = captor.getValue();
        assertThat(cause).isInstanceOf(IllegalArgumentException.class);
        assertThat(cause.getMessage()).contains("Invalid database.host");
    }
}
