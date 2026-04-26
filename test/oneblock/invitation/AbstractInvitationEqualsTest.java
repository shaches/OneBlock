package oneblock.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Phase 3.7 added {@link AbstractInvitation#hashCode()} matching the
 * existing {@link AbstractInvitation#equals(Object)} contract (pair of
 * inviter + invitee UUIDs). This suite pins the contract so the two
 * methods can never silently diverge.
 *
 * <p>{@code AbstractInvitation} is abstract so the tests use the
 * concrete {@link Invitation} subclass which adds a TTL but does not
 * override equals/hashCode.
 */
class AbstractInvitationEqualsTest {

    @Test
    @DisplayName("Same (inviting, invited) pair -> equal + same hashCode")
    void samePairEqual() {
        UUID inviter = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        Invitation a = new Invitation(inviter, invitee);
        Invitation b = new Invitation(inviter, invitee);
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    @DisplayName("Different inviter -> not equal")
    void differentInviterNotEqual() {
        UUID invitee = UUID.randomUUID();
        Invitation a = new Invitation(UUID.randomUUID(), invitee);
        Invitation b = new Invitation(UUID.randomUUID(), invitee);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Swapped inviter/invitee -> not equal (order matters)")
    void swappedNotEqual() {
        UUID x = UUID.randomUUID();
        UUID y = UUID.randomUUID();
        Invitation a = new Invitation(x, y);
        Invitation b = new Invitation(y, x);
        assertThat(a).isNotEqualTo(b);
    }
}
