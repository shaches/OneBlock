package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JMM regression tests for the Phase 2 thread-safety refactor: the four
 * former {@code Oneblock.x/y/z/offset} static fields were folded into a
 * single {@link IslandOrigin} reference swapped via
 * {@link java.util.concurrent.atomic.AtomicReference#updateAndGet}. These
 * tests hammer {@code Oneblock.ORIGIN} from competing threads and assert
 * that readers only ever observe self-consistent snapshots.
 *
 * <p>The concurrency test is timing-sensitive but avoids flakiness by
 * hammering for a fixed iteration budget rather than a wall-clock deadline,
 * and by using a latch so the reader cannot race ahead of the writer's
 * first swap. No Bukkit / Plugin instantiation is required because only the
 * static {@code ORIGIN} AtomicReference is touched.
 */
class OneblockOriginConcurrencyTest {

    private IslandOrigin saved;

    @BeforeEach
    void snapshot() {
        // Preserve whatever state other tests (or class init) left behind so
        // we can restore it post-test. Package-private access via the shared
        // package.
        saved = Oneblock.ORIGIN.get();
    }

    @AfterEach
    void restore() {
        Oneblock.ORIGIN.set(saved);
    }

    @Test
    @DisplayName("Reader never observes a torn ORIGIN while writer swaps")
    void readerSeesOnlyConsistentSnapshots() throws InterruptedException {
        // The invariant: every snapshot the writer publishes has
        //     x == y == z  AND  offset == 10 * x.
        // If a reader ever sees, say, x=10 and offset=200, that would be
        // proof of a torn read (two fields from different writes).
        final IslandOrigin snapA = new IslandOrigin(null, 10, 10, 10, 100);
        final IslandOrigin snapB = new IslandOrigin(null, 20, 20, 20, 200);
        Oneblock.ORIGIN.set(snapA);

        final int iterations = 200_000;
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicInteger tornReads = new AtomicInteger();
        final CountDownLatch ready = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            ready.countDown();
            boolean flip = false;
            for (int i = 0; i < iterations && !stop.get(); i++) {
                Oneblock.ORIGIN.set(flip ? snapA : snapB);
                flip = !flip;
            }
        }, "origin-writer");

        Thread reader = new Thread(() -> {
            try { ready.await(); } catch (InterruptedException ignored) {}
            for (int i = 0; i < iterations * 2; i++) {
                IslandOrigin o = Oneblock.ORIGIN.get();
                // Any combination that isn't (_, n, n, n, 10*n) for n in
                // {10, 20} is a torn read.
                if (!(o.x() == o.y() && o.y() == o.z()
                        && o.offset() == 10 * o.x()
                        && (o.x() == 10 || o.x() == 20))) {
                    tornReads.incrementAndGet();
                }
            }
        }, "origin-reader");

        writer.start();
        reader.start();
        writer.join(TimeUnit.SECONDS.toMillis(10));
        reader.join(TimeUnit.SECONDS.toMillis(10));
        stop.set(true);

        assertThat(tornReads.get())
                .as("Reader should never see a torn ORIGIN snapshot")
                .isZero();
    }

    @Test
    @DisplayName("updateAndGet publishes a snapshot that any later reader sees in full")
    void updateAndGetVisibility() throws InterruptedException {
        Oneblock.ORIGIN.set(IslandOrigin.EMPTY);

        final CountDownLatch done = new CountDownLatch(1);
        final int[] read = new int[5]; // world-null-marker, x, y, z, offset

        Thread writer = new Thread(() -> {
            Oneblock.ORIGIN.updateAndGet(prev -> new IslandOrigin(null, 42, 64, 99, 128));
            done.countDown();
        }, "origin-writer-visibility");

        Thread reader = new Thread(() -> {
            try { done.await(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            IslandOrigin o = Oneblock.ORIGIN.get();
            read[0] = (o.world() == null) ? 1 : 0;
            read[1] = o.x();
            read[2] = o.y();
            read[3] = o.z();
            read[4] = o.offset();
        }, "origin-reader-visibility");

        writer.start();
        reader.start();
        writer.join(TimeUnit.SECONDS.toMillis(2));
        reader.join(TimeUnit.SECONDS.toMillis(2));

        assertThat(read[0]).as("world should still be null").isEqualTo(1);
        assertThat(read[1]).as("x").isEqualTo(42);
        assertThat(read[2]).as("y").isEqualTo(64);
        assertThat(read[3]).as("z").isEqualTo(99);
        assertThat(read[4]).as("offset").isEqualTo(128);
    }

    @Test
    @DisplayName("Concurrent writers: final ORIGIN equals one of the contending swaps")
    void concurrentWritersLeaveConsistentState() throws InterruptedException {
        final IslandOrigin snapA = new IslandOrigin(null, 1, 1, 1, 10);
        final IslandOrigin snapB = new IslandOrigin(null, 2, 2, 2, 20);
        Oneblock.ORIGIN.set(IslandOrigin.EMPTY);

        final int iterations = 50_000;
        final CountDownLatch ready = new CountDownLatch(2);

        Thread wA = new Thread(() -> {
            ready.countDown();
            for (int i = 0; i < iterations; i++) Oneblock.ORIGIN.set(snapA);
        });
        Thread wB = new Thread(() -> {
            ready.countDown();
            for (int i = 0; i < iterations; i++) Oneblock.ORIGIN.set(snapB);
        });

        wA.start();
        wB.start();
        wA.join();
        wB.join();

        IslandOrigin finalState = Oneblock.ORIGIN.get();
        // Post-condition: the result must be exactly one of the two snaps,
        // never a hybrid. Structural equals is enough because records
        // compare componentwise.
        assertThat(finalState).isIn(snapA, snapB);
    }
}
