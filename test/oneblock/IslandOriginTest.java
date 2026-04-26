package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Record-semantics + helper coverage for {@link IslandOrigin}, the immutable
 * atomic-swap unit that replaced the four {@code Oneblock.x/y/z/offset}
 * static fields. Does not touch {@link Oneblock} itself so this suite remains
 * fast and has no Bukkit wiring cost.
 */
class IslandOriginTest {

    @Test
    @DisplayName("EMPTY is the (null, 0, 0, 0, 0) sentinel, shared singleton")
    void emptySentinel() {
        IslandOrigin e = IslandOrigin.EMPTY;
        assertThat(e.world()).isNull();
        assertThat(e.x()).isZero();
        assertThat(e.y()).isZero();
        assertThat(e.z()).isZero();
        assertThat(e.offset()).isZero();
        // Shared singleton: same reference on repeated reads.
        assertThat(IslandOrigin.EMPTY).isSameAs(e);
    }

    @Test
    @DisplayName("accessors return exactly what the canonical constructor took")
    void accessorsRoundTrip() {
        World mockWorld = Mockito.mock(World.class);
        IslandOrigin o = new IslandOrigin(mockWorld, 10, 64, -20, 256);
        assertThat(o.world()).isSameAs(mockWorld);
        assertThat(o.x()).isEqualTo(10);
        assertThat(o.y()).isEqualTo(64);
        assertThat(o.z()).isEqualTo(-20);
        assertThat(o.offset()).isEqualTo(256);
    }

    @Test
    @DisplayName("record equals/hashCode honour structural equality across instances")
    void equalsAndHashCode() {
        World mockWorld = Mockito.mock(World.class);
        IslandOrigin a = new IslandOrigin(mockWorld, 1, 2, 3, 4);
        IslandOrigin b = new IslandOrigin(mockWorld, 1, 2, 3, 4);
        IslandOrigin c = new IslandOrigin(mockWorld, 1, 2, 3, 5); // offset differs

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        // Identity: different instance, same content.
        assertThat(a).isNotSameAs(b);
    }

    @Test
    @DisplayName("withOffset returns a new instance with only offset changed")
    void withOffsetPreservesOthers() {
        World mockWorld = Mockito.mock(World.class);
        IslandOrigin base = new IslandOrigin(mockWorld, 7, 8, 9, 100);
        IslandOrigin updated = base.withOffset(200);

        assertThat(updated.offset()).isEqualTo(200);
        assertThat(updated.world()).isSameAs(base.world());
        assertThat(updated.x()).isEqualTo(base.x());
        assertThat(updated.y()).isEqualTo(base.y());
        assertThat(updated.z()).isEqualTo(base.z());
        // Immutability: the original must be untouched.
        assertThat(base.offset()).isEqualTo(100);
        assertThat(updated).isNotSameAs(base);
    }

    @Test
    @DisplayName("withPosition: non-null world replaces; other coords overwrite")
    void withPositionReplacesWorldAndCoords() {
        World originalWorld = Mockito.mock(World.class);
        World newWorld = Mockito.mock(World.class);
        IslandOrigin base = new IslandOrigin(originalWorld, 0, 0, 0, 50);
        IslandOrigin updated = base.withPosition(newWorld, 11, 22, 33);

        assertThat(updated.world()).isSameAs(newWorld);
        assertThat(updated.x()).isEqualTo(11);
        assertThat(updated.y()).isEqualTo(22);
        assertThat(updated.z()).isEqualTo(33);
        assertThat(updated.offset()).isEqualTo(50); // preserved
    }

    @Test
    @DisplayName("withPosition(null, ...): keeps current world — matches setPosition semantics")
    void withPositionNullWorldKeepsExisting() {
        World existing = Mockito.mock(World.class);
        IslandOrigin base = new IslandOrigin(existing, 0, 0, 0, 50);
        IslandOrigin updated = base.withPosition(null, 4, 5, 6);

        assertThat(updated.world()).isSameAs(existing);
        assertThat(updated.x()).isEqualTo(4);
        assertThat(updated.y()).isEqualTo(5);
        assertThat(updated.z()).isEqualTo(6);
    }

    @Test
    @DisplayName("withPosition starting from EMPTY leaves world null when given null world")
    void withPositionFromEmptyNullWorld() {
        IslandOrigin updated = IslandOrigin.EMPTY.withPosition(null, 1, 2, 3);
        assertThat(updated.world()).isNull();
        assertThat(updated.x()).isEqualTo(1);
        assertThat(updated.y()).isEqualTo(2);
        assertThat(updated.z()).isEqualTo(3);
    }
}
