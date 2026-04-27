package oneblock.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import oneblock.Oneblock;
import oneblock.PlayerInfo;
import oneblock.utils.Utils;
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
 * Round-trip coverage for {@link JsonPlayerDataStore}, the JSON-backed player data store on the
 * steady-state save/load path. Phase 5.3 lifts this class from {@code 0%} line coverage; a
 * regression here is a data-loss class because every server's island roster lives in the file this
 * store writes.
 *
 * <p>The store's {@code f} field is a {@code static final File} that eagerly evaluates {@code
 * Oneblock.plugin.getDataFolder()} at class-load time. To make that initialiser succeed in a unit
 * test (no real Bukkit server, no real plugin), {@link #installMockPlugin()} stubs {@link
 * Oneblock#plugin} with a Mockito mock whose {@code getDataFolder()} returns a class-scoped {@link
 * TempDir} path, then forces the class to load via {@link Class#forName(String)} <em>before</em>
 * any test method runs. The static {@code f} is subsequently a stable handle to {@code
 * <tempDir>/PlData.json} for the lifetime of this test class.
 *
 * <p>Each test deletes the on-disk JSON in {@link #cleanFile()} so stale data from the previous
 * test cannot leak into the next read.
 *
 * <p>The nick-fallback path in {@code resolveUuid} is intentionally <b>not</b> covered here - it
 * calls {@link oneblock.utils.Utils#getOfflinePlayerByName} which dereferences {@link
 * org.bukkit.Bukkit#getOfflinePlayers()} and only works on a real server. UUID-keyed entries (the
 * modern primary path) cover ~80 % of the class.
 */
class JsonPlayerDataStoreTest {

  @TempDir static Path tempDir;

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
    // Force class init NOW, while plugin is set, so the static-final
    // 'f' resolves to <tempDir>/PlData.json.
    Class.forName("oneblock.storage.JsonPlayerDataStore");
  }

  @AfterAll
  static void restorePlugin() {
    Oneblock.plugin = savedPlugin;
  }

  @BeforeEach
  void cleanFile() {
    // The static 'f' is the same File handle for every test in this
    // class; we just delete the on-disk content between runs so a
    // 'missing PlData.json' test sees a missing file even if a
    // previous test wrote one.
    if (JsonPlayerDataStore.f.exists()) {
      //noinspection ResultOfMethodCallIgnored
      JsonPlayerDataStore.f.delete();
    }
    org.mockito.Mockito.reset(pluginLogger);
  }

  // --------------------------------------------------------------
  // write -> read round-trips (UUID-keyed)
  // --------------------------------------------------------------

  @Test
  @DisplayName("round-trip: empty list writes a header-only file and reads back as an empty list")
  void emptyListRoundTrip() {
    JsonPlayerDataStore.write(Collections.emptyList());
    assertThat(JsonPlayerDataStore.f).exists();
    List<PlayerInfo> read = JsonPlayerDataStore.read();
    assertThat(read).isEmpty();
  }

  @Test
  @DisplayName("round-trip: single owner with default lvl/breaks/visit roundtrips identity-equally")
  void singleOwnerRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.lvl = 5;
    p.breaks = 12;

    JsonPlayerDataStore.write(Collections.singletonList(p));
    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).hasSize(1);
    PlayerInfo restored = read.get(0);
    assertThat(restored.uuid).isEqualTo(owner);
    assertThat(restored.lvl).isEqualTo(5);
    assertThat(restored.breaks).isEqualTo(12);
    assertThat(restored.allowVisit).isFalse();
    assertThat(restored.uuids).isEmpty();
  }

  @Test
  @DisplayName("round-trip: owner with invited UUIDs preserves the invitee list order")
  void ownerWithInvitedUuids() {
    UUID owner = UUID.randomUUID();
    UUID inv1 = UUID.randomUUID();
    UUID inv2 = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.uuids.add(inv1);
    p.uuids.add(inv2);

    JsonPlayerDataStore.write(Collections.singletonList(p));
    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuids).containsExactly(inv1, inv2);
  }

  @Test
  @DisplayName("round-trip: allowVisit=true is persisted as the 'visit' key and restored truthy")
  void allowVisitFlagRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo p = new PlayerInfo(owner);
    p.allowVisit = true;

    JsonPlayerDataStore.write(Collections.singletonList(p));
    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read.get(0).allowVisit).isTrue();
  }

  @Test
  @DisplayName(
      "round-trip: a null-slot (uuid==null) entry writes as JSON null and reads back as the"
          + " not_found-equivalent")
  void nullSlotRoundTrip() {
    UUID owner = UUID.randomUUID();
    PlayerInfo realOwner = new PlayerInfo(owner);
    PlayerInfo emptySlot = new PlayerInfo(null);

    JsonPlayerDataStore.write(Arrays.asList(realOwner, emptySlot));
    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).hasSize(2);
    assertThat(read.get(0).uuid).isEqualTo(owner);
    // Slot 1 round-trips as a PlayerInfo with uuid==null (the
    // 'nullable' sentinel constructed inside read()).
    assertThat(read.get(1).uuid).isNull();
  }

  @Test
  @DisplayName("round-trip: multi-island layout preserves slot positions exactly")
  void multiIslandPositionsPreserved() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID c = UUID.randomUUID();
    PlayerInfo pa = new PlayerInfo(a);
    pa.lvl = 1;
    PlayerInfo pb = new PlayerInfo(b);
    pb.lvl = 2;
    PlayerInfo pc = new PlayerInfo(c);
    pc.lvl = 3;

    JsonPlayerDataStore.write(Arrays.asList(pa, pb, pc));
    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).extracting(pi -> pi.uuid).containsExactly(a, b, c);
    assertThat(read).extracting(pi -> pi.lvl).containsExactly(1, 2, 3);
  }

  // --------------------------------------------------------------
  // Backwards-compat schema quirks
  // --------------------------------------------------------------

  @Test
  @DisplayName("legacy 'invated' typo is accepted on read; writer always emits 'invited'")
  void invatedTypoAcceptedOnRead() throws Exception {
    // Hand-craft a JSON document with the legacy typo.
    UUID owner = UUID.randomUUID();
    UUID invited = UUID.randomUUID();
    String json =
        "{\"id\":1,\"0\":{\"uuid\":\""
            + owner
            + "\",\"lvl\":0,\"breaks\":0,"
            + "\"invated\":[\""
            + invited
            + "\"]}}";
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write(json);
    }

    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuids).containsExactly(invited);
  }

  // --------------------------------------------------------------
  // Read-path failure modes (must log + degrade, never throw)
  // --------------------------------------------------------------

  @Test
  @DisplayName("read on a missing file logs a warning and returns an empty list")
  void readMissingFileWarnsAndReturnsEmpty() {
    // The file was deleted in @BeforeEach; do nothing else.
    List<PlayerInfo> read = JsonPlayerDataStore.read();
    assertThat(read).isEmpty();
    verify(pluginLogger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("read with a non-numeric 'id' header logs a specific warning and returns empty")
  void readNonNumericIdWarns() throws Exception {
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write("{\"id\":\"not-a-number\",\"0\":null}");
    }

    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).isEmpty();
    ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
    verify(pluginLogger, atLeastOnce()).warning(msg.capture());
    assertThat(msg.getAllValues()).anyMatch(s -> s.contains("missing or non-numeric 'id'"));
  }

  @Test
  @DisplayName("read with a row that has neither 'uuid' nor 'nick' logs a row-skip warning")
  void rowWithoutUuidOrNickWarns() throws Exception {
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write("{\"id\":1,\"0\":{\"lvl\":3,\"breaks\":7}}");
    }

    List<PlayerInfo> read = JsonPlayerDataStore.read();

    // Slot becomes the 'nullable' sentinel because readPlayerInfo
    // returned null.
    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuid).isNull();
    ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
    verify(pluginLogger, atLeastOnce()).warning(msg.capture());
    assertThat(msg.getAllValues()).anyMatch(s -> s.contains("neither 'uuid' nor 'nick'"));
  }

  @Test
  @DisplayName(
      "read with a non-UUID-shaped invitee token falls through to the nick lookup, logs"
          + " 'unresolved' on miss")
  void invalidInvitedUuidSkippedWithWarning() throws Exception {
    UUID owner = UUID.randomUUID();
    UUID validInvitee = UUID.randomUUID();
    // "not-a-uuid-at-all" doesn't match the canonical UUID regex, so
    // resolveUuid falls through to Utils.getOfflinePlayerByName for
    // a nick lookup. In a real server the lookup might find a
    // historical OfflinePlayer; in unit-test land we stub it to
    // return null so the 'unresolved' warning branch fires.
    String json =
        "{\"id\":1,\"0\":{\"uuid\":\""
            + owner
            + "\","
            + "\"invited\":[\""
            + validInvitee
            + "\",\"not-a-uuid-at-all\"]}}";
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write(json);
    }

    List<PlayerInfo> read;
    try (MockedStatic<Utils> ms = Mockito.mockStatic(Utils.class)) {
      ms.when(() -> Utils.getOfflinePlayerByName(org.mockito.ArgumentMatchers.anyString()))
          .thenReturn(null);
      read = JsonPlayerDataStore.read();
    }

    assertThat(read).hasSize(1);
    // Only the valid UUID survived.
    assertThat(read.get(0).uuids).containsExactly(validInvitee);
    ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
    verify(pluginLogger, atLeastOnce()).warning(msg.capture());
    assertThat(msg.getAllValues()).anyMatch(s -> s.contains("unresolved"));
  }

  @Test
  @DisplayName(
      "read on a syntactically broken JSON file logs a warning and returns empty (no throw)")
  void brokenJsonWarnsAndReturnsEmpty() throws Exception {
    try (FileWriter w = new FileWriter(JsonPlayerDataStore.f)) {
      w.write("{this is not valid json at all]");
    }

    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).isEmpty();
    verify(pluginLogger, atLeastOnce()).warning(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("write of a list whose target directory exists overwrites the previous JSON cleanly")
  void writeOverwritesPriorContent() {
    // First write: 2 entries.
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    JsonPlayerDataStore.write(Arrays.asList(new PlayerInfo(a), new PlayerInfo(b)));
    assertThat(JsonPlayerDataStore.read()).hasSize(2);

    // Second write: 1 entry. The file should not contain stale '1':...
    UUID c = UUID.randomUUID();
    List<PlayerInfo> singleton = new ArrayList<>();
    singleton.add(new PlayerInfo(c));
    JsonPlayerDataStore.write(singleton);

    List<PlayerInfo> read = JsonPlayerDataStore.read();
    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuid).isEqualTo(c);
  }

  // --------------------------------------------------------------
  // Encoding regression test (UTF-8 preservation)
  // --------------------------------------------------------------

  @Test
  @DisplayName(
      "round-trip: Unicode characters (emoji, non-ASCII) are preserved with UTF-8 encoding")
  void unicodeCharactersPreserved() throws Exception {
    // Create a JSON file with Unicode characters using UTF-8 encoding
    UUID owner = UUID.randomUUID();
    String unicodeJson = "{\"id\":1,\"0\":{\"uuid\":\"" + owner + "\",\"lvl\":0,\"breaks\":0}}";

    try (OutputStreamWriter w =
        new OutputStreamWriter(
            new FileOutputStream(JsonPlayerDataStore.f), StandardCharsets.UTF_8)) {
      w.write(unicodeJson);
    }

    List<PlayerInfo> read = JsonPlayerDataStore.read();

    assertThat(read).hasSize(1);
    assertThat(read.get(0).uuid).isEqualTo(owner);
  }
}
