package oneblock.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression net for the Phase 4.2 swap of {@link Guest#list} and
 * {@link Invitation#list} from {@code ArrayList} to
 * {@link java.util.concurrent.CopyOnWriteArrayList}: a reader iterating
 * the list while a writer concurrently {@code add}s and {@code remove}s
 * entries must not throw
 * {@link java.util.ConcurrentModificationException}.
 *
 * <p>If a future maintainer reverts either field to a plain
 * {@code ArrayList}, the concurrent iteration here will trip the
 * fail-fast iterator within the first few hundred operations.
 *
 * <p>The tests do not exercise correctness of {@link Invitation#add(UUID, UUID)}
 * (which schedules a delayed Bukkit task that needs a live server) -
 * they only exercise the storage list directly, which is the surface
 * this slice changes.
 */
class GuestInvitationConcurrencyTest {

    @BeforeEach
    void clearLists() {
        Guest.list.clear();
        Invitation.list.clear();
    }

    @AfterEach
    void cleanup() {
        Guest.list.clear();
        Invitation.list.clear();
    }

    @Test
    @DisplayName("Guest.list: concurrent iterate + add/remove never throws CME")
    void guestListIsCmeFree() throws InterruptedException {
        runConcurrentAddRemoveAgainstReader(
                () -> {
                    UUID inviting = UUID.randomUUID();
                    UUID invited = UUID.randomUUID();
                    Guest g = new Guest(inviting, invited);
                    Guest.list.add(g);
                    Guest.list.remove(g);
                },
                () -> {
                    int count = 0;
                    for (Guest g : Guest.list) {
                        // Touch a field so the JIT cannot optimise the loop away.
                        if (g != null && g.Invited != null) count++;
                    }
                    return count;
                });
    }

    @Test
    @DisplayName("Invitation.list: concurrent iterate + add/remove never throws CME")
    void invitationListIsCmeFree() throws InterruptedException {
        runConcurrentAddRemoveAgainstReader(
                () -> {
                    UUID inviting = UUID.randomUUID();
                    UUID invited = UUID.randomUUID();
                    Invitation inv = new Invitation(inviting, invited);
                    Invitation.list.add(inv);
                    Invitation.list.remove(inv);
                },
                () -> {
                    int count = 0;
                    for (Invitation inv : Invitation.list) {
                        if (inv != null && inv.Invited != null) count++;
                    }
                    return count;
                });
    }

    /**
     * Spin one writer thread doing back-to-back {@code add} + {@code remove}
     * for a fixed iteration budget against one reader thread that
     * iterates the list and counts non-null entries. Both threads run
     * concurrently and any thrown exception is captured into
     * {@code failure} for the test to assert on.
     */
    private static void runConcurrentAddRemoveAgainstReader(Runnable writeOnce, java.util.function.IntSupplier readOnce)
            throws InterruptedException {
        final int writeIterations = 5_000;
        AtomicBoolean stopReader = new AtomicBoolean(false);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                while (!stopReader.get()) readOnce.getAsInt();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < writeIterations; i++) writeOnce.run();
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

        assertThat(failure.get()).as("concurrent add/remove vs iterate").isNull();
    }
}
