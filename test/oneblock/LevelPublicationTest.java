package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression net for the Phase 4.1 publication-safety contract on
 * {@link Level#levels}: writers stage a fresh list and call
 * {@link Level#replaceAll(List)} once, so concurrent readers either see
 * the entire pre-call snapshot or the entire post-call snapshot - never a
 * half-populated mid-state. Pre-Phase-4.1 the field was a public
 * {@link ArrayList} that {@code ConfigManager.loadBlocks} cleared and
 * refilled in place, exposing a
 * {@link java.util.ConcurrentModificationException} window for the async
 * {@code IslandParticleTask} / {@code OBP} placeholder consumers that
 * indirectly read {@link Level#get(int)}.
 *
 * <p>If a future maintainer reverts to in-place mutation
 * ({@code clear() + addAll()}) instead of an atomic
 * {@link Level#replaceAll(List)}, the {@link #replaceAllAtomicConcurrent}
 * test will see a partial size or a partially-iterated snapshot and fail.
 */
class LevelPublicationTest {

    private List<Level> savedLevels;

    @BeforeEach
    void snapshot() {
        savedLevels = new ArrayList<>(Level.snapshot());
        Level.replaceAll(null);
    }

    @AfterEach
    void restore() {
        Level.replaceAll(savedLevels);
    }

    @Test
    @DisplayName("replaceAll publishes a defensively-copied unmodifiable snapshot")
    void replaceAllDefensiveCopy() {
        List<Level> input = new ArrayList<>();
        input.add(new Level("a"));
        input.add(new Level("b"));

        Level.replaceAll(input);

        List<Level> snap = Level.snapshot();
        assertThat(snap).hasSize(2);
        assertThat(snap).isUnmodifiable();

        // Mutating the input AFTER publication does not affect the snapshot:
        // replaceAll defensively copies before the volatile write.
        input.add(new Level("c"));
        assertThat(Level.snapshot()).hasSize(2);
    }

    @Test
    @DisplayName("replaceAll(null) and replaceAll(empty) both publish an empty list")
    void replaceAllNullAndEmpty() {
        Level.replaceAll(Arrays.asList(new Level("x")));
        assertThat(Level.size()).isEqualTo(1);

        Level.replaceAll(null);
        assertThat(Level.size()).isZero();

        Level.replaceAll(Arrays.asList(new Level("y")));
        assertThat(Level.size()).isEqualTo(1);

        Level.replaceAll(Collections.emptyList());
        assertThat(Level.size()).isZero();
    }

    @Test
    @DisplayName("snapshot() returned before replaceAll keeps the pre-swap reference")
    void snapshotIsStable() {
        Level a = new Level("a");
        Level b = new Level("b");
        Level.replaceAll(Arrays.asList(a, b));

        // Capture a snapshot, then swap to an entirely different list.
        List<Level> pre = Level.snapshot();
        Level.replaceAll(Arrays.asList(new Level("z")));

        // The captured reference still walks the old list.
        assertThat(pre).hasSize(2);
        assertThat(pre.get(0)).isSameAs(a);
        assertThat(pre.get(1)).isSameAs(b);
    }

    @Test
    @DisplayName("concurrent readers never observe a partial size during alternating replaceAll calls")
    void replaceAllAtomicConcurrent() throws InterruptedException {
        final int writeIterations = 2000;
        final int populatedSize = 5;

        List<Level> populated = new ArrayList<>();
        for (int i = 0; i < populatedSize; i++) populated.add(new Level("L" + i));

        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicReference<String> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            while (!stop.get()) {
                int size = Level.size();
                if (size != 0 && size != populatedSize) {
                    failure.compareAndSet(null, "saw partial size " + size);
                    return;
                }
                // Iterating the captured snapshot must never produce a null
                // entry mid-write either; the captured reference is stable
                // even if the volatile field has since been swapped.
                List<Level> snap = Level.snapshot();
                for (Level lvl : snap) {
                    if (lvl == null) {
                        failure.compareAndSet(null, "saw null entry in snapshot of size " + snap.size());
                        return;
                    }
                }
            }
        });

        Thread writer = new Thread(() -> {
            for (int i = 0; i < writeIterations; i++) {
                Level.replaceAll(populated);
                Level.replaceAll(Collections.emptyList());
            }
            stop.set(true);
        });

        reader.start();
        writer.start();
        writer.join();
        reader.join(5000);
        stop.set(true);

        assertThat(failure.get()).as("concurrent observation").isNull();
    }
}
