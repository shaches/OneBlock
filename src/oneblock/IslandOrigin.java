package oneblock;

import org.bukkit.World;

/**
 * Immutable snapshot of the island-layout anchor:
 * {@code (world, x, y, z, offset)} treated as a single atomically-replaceable
 * unit. Writers mutate the origin via
 * {@link java.util.concurrent.atomic.AtomicReference#updateAndGet} on
 * {@code Oneblock.ORIGIN}; readers call {@link Oneblock#origin()} once and use
 * the captured snapshot so that a concurrent {@code /ob set} cannot produce a
 * mixed (torn) view in which, say, {@code x} is new but {@code offset} is old.
 *
 * <p>The zero/null sentinel {@link #EMPTY} is used both as the initial
 * ORIGIN state (before {@code config.yml} load) and as an uninitialised
 * marker; callers that need to distinguish "not set yet" from "set to
 * (0,0,0,0)" guard on {@code world() == null} (the typical shape of
 * {@code EMPTY}) or on {@code offset() == 0} (the legacy convention that
 * {@code /ob set} rejects {@code offset == 0}).
 */
public record IslandOrigin(World world, int x, int y, int z, int offset) {

    /** Uninitialised / pre-config sentinel: null world, all ints zero. */
    public static final IslandOrigin EMPTY = new IslandOrigin(null, 0, 0, 0, 0);

    /**
     * Returns a copy with the world / x / y / z replaced and the current
     * {@code offset} preserved. A null {@code newWorld} keeps the existing
     * world reference (matches the legacy {@code setPosition} semantics).
     */
    public IslandOrigin withPosition(World newWorld, int newX, int newY, int newZ) {
        return new IslandOrigin(newWorld != null ? newWorld : this.world,
                                newX, newY, newZ, this.offset);
    }

    /** Returns a copy with only the offset replaced; world / x / y / z kept. */
    public IslandOrigin withOffset(int newOffset) {
        return new IslandOrigin(this.world, this.x, this.y, this.z, newOffset);
    }
}
