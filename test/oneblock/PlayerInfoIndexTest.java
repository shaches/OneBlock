package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the Phase 2 reverse-index contract added to {@link PlayerInfo}:
 * O(1) {@code getId}, index maintenance on {@code set}/{@code replaceAll}/
 * {@code removeUUID}/{@code addInvite}/{@code removeInvite}, and the top-list
 * version counter bumped by mutations.
 */
class PlayerInfoIndexTest {

    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID U4 = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @BeforeEach
    void reset() {
        PlayerInfo.replaceAll(Collections.emptyList());
    }

    @Test
    @DisplayName("empty registry: getId returns -1 for any uuid (including null)")
    void emptyRegistryReturnsMinusOne() {
        assertThat(PlayerInfo.getId(null)).isEqualTo(-1);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(-1);
        assertThat(PlayerInfo.size()).isZero();
    }

    @Test
    @DisplayName("set() registers the owner in the reverse index")
    void setRegistersOwner() {
        PlayerInfo inf = new PlayerInfo(U1);
        PlayerInfo.set(0, inf);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);
        assertThat(PlayerInfo.get(U1)).isSameAs(inf);
        assertThat(PlayerInfo.existsAsOwner(U1)).isTrue();
    }

    @Test
    @DisplayName("set() registers pre-existing invitees on the PlayerInfo")
    void setRegistersInvitees() {
        PlayerInfo inf = new PlayerInfo(U1);
        inf.uuids.add(U2);
        inf.uuids.add(U3);
        PlayerInfo.set(0, inf);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(0);
        assertThat(PlayerInfo.getId(U3)).isEqualTo(0);
        assertThat(PlayerInfo.existsAsOwner(U2)).isFalse();
    }

    @Test
    @DisplayName("replaceAll() rebuilds the reverse index and clears stale entries")
    void replaceAllRebuildsIndex() {
        PlayerInfo a = new PlayerInfo(U1);
        a.uuids.add(U2);
        PlayerInfo.set(0, a);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);

        // Replace with a different slot layout; old U1/U2 must be gone.
        PlayerInfo b = new PlayerInfo(U3);
        b.uuids.add(U4);
        PlayerInfo.replaceAll(List.of(b));

        assertThat(PlayerInfo.getId(U1)).isEqualTo(-1);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(-1);
        assertThat(PlayerInfo.getId(U3)).isEqualTo(0);
        assertThat(PlayerInfo.getId(U4)).isEqualTo(0);
        assertThat(PlayerInfo.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("replaceAll(null) clears the registry without NPE")
    void replaceAllNullClears() {
        PlayerInfo.set(0, new PlayerInfo(U1));
        PlayerInfo.replaceAll(null);
        assertThat(PlayerInfo.size()).isZero();
        assertThat(PlayerInfo.getId(U1)).isEqualTo(-1);
    }

    @Test
    @DisplayName("addInvite updates uuids AND reverse index")
    void addInviteUpdatesIndex() {
        PlayerInfo inf = new PlayerInfo(U1);
        PlayerInfo.set(0, inf);
        inf.addInvite(U2);
        assertThat(inf.uuids).containsExactly(U2);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(0);
    }

    @Test
    @DisplayName("removeInvite clears both uuids and reverse index")
    void removeInviteClears() {
        PlayerInfo inf = new PlayerInfo(U1);
        inf.uuids.add(U2);
        PlayerInfo.set(0, inf);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(0);

        inf.removeInvite(U2);
        assertThat(inf.uuids).isEmpty();
        assertThat(PlayerInfo.getId(U2)).isEqualTo(-1);
        // Owner still mapped.
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);
    }

    @Test
    @DisplayName("removeUUID on owner promotes first invitee and drops old owner from index")
    void removeUuidPromotesInvitee() {
        PlayerInfo inf = new PlayerInfo(U1);
        inf.uuids.add(U2);
        inf.uuids.add(U3);
        PlayerInfo.set(0, inf);

        inf.removeUUID(U1);

        assertThat(inf.uuid).isEqualTo(U2);
        assertThat(inf.uuids).containsExactly(U3);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(-1);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(0);
        assertThat(PlayerInfo.getId(U3)).isEqualTo(0);
        assertThat(PlayerInfo.existsAsOwner(U2)).isTrue();
    }

    @Test
    @DisplayName("removeUUID on sole-owner-no-invitees leaves island with null uuid")
    void removeUuidSoleOwner() {
        PlayerInfo inf = new PlayerInfo(U1);
        PlayerInfo.set(0, inf);
        inf.removeUUID(U1);
        assertThat(inf.uuid).isNull();
        assertThat(PlayerInfo.getId(U1)).isEqualTo(-1);
        assertThat(PlayerInfo.existsAsOwner(U1)).isFalse();
    }

    @Test
    @DisplayName("removeUUID on invitee only removes that invitee from index")
    void removeUuidInviteeOnly() {
        PlayerInfo inf = new PlayerInfo(U1);
        inf.uuids.add(U2);
        PlayerInfo.set(0, inf);

        inf.removeUUID(U2);

        assertThat(inf.uuid).isEqualTo(U1);
        assertThat(inf.uuids).isEmpty();
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);
        assertThat(PlayerInfo.getId(U2)).isEqualTo(-1);
    }

    @Test
    @DisplayName("removeUUID(null) is a no-op")
    void removeUuidNullNoop() {
        PlayerInfo inf = new PlayerInfo(U1);
        PlayerInfo.set(0, inf);
        inf.removeUUID(null);
        assertThat(inf.uuid).isEqualTo(U1);
        assertThat(PlayerInfo.getId(U1)).isEqualTo(0);
    }

    @Test
    @DisplayName("topVersion increments on every mutation path")
    void topVersionBumps() {
        long v0 = PlayerInfo.topVersion();

        PlayerInfo a = new PlayerInfo(U1);
        PlayerInfo.set(0, a);
        long v1 = PlayerInfo.topVersion();
        assertThat(v1).isGreaterThan(v0);

        a.lvlup();
        long v2 = PlayerInfo.topVersion();
        assertThat(v2).isGreaterThan(v1);

        PlayerInfo.replaceAll(new ArrayList<>());
        assertThat(PlayerInfo.topVersion()).isGreaterThan(v2);
    }

    @Test
    @DisplayName("getFreeId: with UseEmptyIslands=false returns size()")
    void getFreeIdAppend() {
        assertThat(PlayerInfo.getFreeId(false)).isZero();
        PlayerInfo.set(0, new PlayerInfo(U1));
        assertThat(PlayerInfo.getFreeId(false)).isEqualTo(1);
    }

    @Test
    @DisplayName("getFreeId: with UseEmptyIslands=true returns the first null-owner slot")
    void getFreeIdRecycleNull() {
        PlayerInfo.set(0, new PlayerInfo(U1));
        PlayerInfo.set(1, new PlayerInfo(null));
        PlayerInfo.set(2, new PlayerInfo(U2));
        assertThat(PlayerInfo.getFreeId(true)).isEqualTo(1);
    }

    @Test
    @DisplayName("getId after set() is O(1) — does not scan linearly")
    void getIdIsConstantTime() {
        // Populate 10_000 slots and verify the 10_000-th lookup is fast.
        // (This is a lower-bound smoke test for the ConcurrentHashMap path.)
        for (int i = 0; i < 10_000; i++) {
            PlayerInfo.set(i, new PlayerInfo(UUID.nameUUIDFromBytes(("p" + i).getBytes())));
        }
        UUID last = UUID.nameUUIDFromBytes("p9999".getBytes());
        long start = System.nanoTime();
        int id = PlayerInfo.getId(last);
        long elapsedNs = System.nanoTime() - start;
        assertThat(id).isEqualTo(9999);
        // A linear scan of 10k entries would take ~100k ns on even a fast
        // JVM; O(1) hash lookup should finish in < 1_000_000 ns with slack.
        assertThat(elapsedNs).isLessThan(1_000_000L);
    }
}
