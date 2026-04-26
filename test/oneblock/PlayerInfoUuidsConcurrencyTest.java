package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression net for the Phase 4.4 swap of {@link PlayerInfo#uuids} from
 * a plain {@code ArrayList} to a
 * {@link java.util.concurrent.CopyOnWriteArrayList}: the async
 * {@code PlayerDataSaveTask} - which calls
 * {@link oneblock.storage.JsonPlayerDataStore#write} and
 * {@link oneblock.storage.DatabaseManager#save} - iterates every island's
 * {@code uuids} list while the main thread can concurrently mutate it via
 * {@link PlayerInfo#addInvite(UUID)}, {@link PlayerInfo#removeInvite(UUID)}
 * or the invitee-promotion branch of {@link PlayerInfo#removeUUID(UUID)}.
 *
 * <p>Pre-Phase-4.4 this was a documented latent race that the codebase
 * happened to dodge because the save runs every 6000 ticks (5 minutes)
 * and the kick/accept/idreset commands are rare; an unlucky overlap
 * would surface as a {@link java.util.ConcurrentModificationException}
 * thrown out of an enhanced {@code for} on the async save thread,
 * losing the entire save tick's worth of data.
 *
 * <p>The test exercises the same access pattern - a reader iterating
 * the list while a writer alternates {@code add} + {@code remove} - and
 * asserts no exception was captured.
 */
class PlayerInfoUuidsConcurrencyTest {

    @Test
    @DisplayName("PlayerInfo.uuids: concurrent iterate + add/remove never throws CME")
    void uuidsListIsCmeFree() throws InterruptedException {
        final int writeIterations = 5_000;
        PlayerInfo inf = new PlayerInfo(UUID.randomUUID());

        AtomicBoolean stopReader = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                while (!stopReader.get()) {
                    int hashAccumulator = 0;
                    for (UUID u : inf.uuids) {
                        // Touch a field so the JIT cannot elide the loop.
                        if (u != null) hashAccumulator ^= u.hashCode();
                    }
                    // No-op assertion; we just need the loop to complete without CME.
                    if (hashAccumulator == Integer.MIN_VALUE) hashAccumulator++;
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < writeIterations; i++) {
                    UUID u = UUID.randomUUID();
                    inf.uuids.add(u);
                    inf.uuids.remove(u);
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                stopReader.set(true);
            }
        });

        reader.start();
        writer.start();
        writer.join();
        reader.join(5000);
        stopReader.set(true);

        assertThat(failure.get()).as("concurrent uuids add/remove vs iterate").isNull();
    }

    @Test
    @DisplayName("PlayerInfo.uuids: stream() snapshot never observes a partial state")
    void uuidsStreamReadsStableSnapshot() throws InterruptedException {
        // Mirrors the DatabaseManager.save pattern:
        //     player.uuids.stream().map(UUID::toString).collect(Collectors.joining(","))
        // The stream walks an internal snapshot; the writer's add/remove cannot
        // perturb it mid-stream.
        PlayerInfo inf = new PlayerInfo(UUID.randomUUID());
        for (int i = 0; i < 10; i++) inf.uuids.add(UUID.randomUUID());

        final int writeIterations = 2_000;
        AtomicBoolean stopReader = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                while (!stopReader.get()) {
                    String joined = inf.uuids.stream()
                            .map(UUID::toString)
                            .reduce("", (a, b) -> a + "," + b);
                    if (joined.contains("null")) {
                        failure.compareAndSet(null,
                                new IllegalStateException("stream produced 'null' substring: " + joined));
                        return;
                    }
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < writeIterations; i++) {
                    UUID u = UUID.randomUUID();
                    inf.uuids.add(u);
                    inf.uuids.remove(u);
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                stopReader.set(true);
            }
        });

        reader.start();
        writer.start();
        writer.join();
        reader.join(5000);
        stopReader.set(true);

        assertThat(failure.get()).as("stream-based read concurrent with add/remove").isNull();
    }
}
