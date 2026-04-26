package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 3.7 added {@link PlayerInfo#equals(Object)} +
 * {@link PlayerInfo#hashCode()} keyed on the owner UUID. This suite
 * pins the contract so a future change to per-island mutable state
 * (level / breaks / member list) cannot accidentally tighten or
 * loosen the equality definition.
 */
class PlayerInfoEqualsTest {

    @Test
    @DisplayName("Same UUID -> equal regardless of mutable level/breaks state")
    void sameUuidEqual() {
        UUID u = UUID.randomUUID();
        PlayerInfo a = new PlayerInfo(u);
        PlayerInfo b = new PlayerInfo(u);
        a.lvl = 7; a.breaks = 42;
        b.lvl = 9; b.breaks = 0;
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("Different UUID -> not equal")
    void differentUuidNotEqual() {
        PlayerInfo a = new PlayerInfo(UUID.randomUUID());
        PlayerInfo b = new PlayerInfo(UUID.randomUUID());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("null UUID matches another null UUID (the not_found sentinel)")
    void nullUuidMatchesNullUuid() {
        PlayerInfo a = new PlayerInfo(null);
        PlayerInfo b = new PlayerInfo(null);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("PlayerInfo never equals a non-PlayerInfo object")
    void notEqualToOtherType() {
        PlayerInfo a = new PlayerInfo(UUID.randomUUID());
        assertThat(a).isNotEqualTo("some string");
        assertThat(a).isNotEqualTo(null);
    }
}
