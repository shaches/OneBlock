package oneblock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Defaults + volatility coverage for {@link Settings}, the Phase 3 typed
 * bag that replaced ~17 {@code public static volatile} fields on
 * {@link oneblock.Oneblock}. {@code Settings} is intentionally Bukkit-free
 * (no {@code Plugin}/{@code World}/etc. dependencies) so this suite stays
 * fast and a fresh instance can be exercised without spinning up a server.
 */
class SettingsTest {

    @Test
    @DisplayName("Default values match the legacy Oneblock static-field defaults")
    void defaultsMatchLegacyOneblockStatics() {
        // These exact defaults were copy-paste-verified against the
        // pre-Phase-3 declarations in Oneblock.java. If a config-default
        // ever needs to change, that's a behaviour change the operator
        // will see on first start (a key absent from config.yml will be
        // (re)written with the new default by ConfigManager.loadMainConfig).
        Settings s = new Settings();
        assertThat(s.max_players_team).isZero();
        assertThat(s.mob_spawn_chance).isEqualTo(9);
        assertThat(s.island_for_new_players).isFalse();
        assertThat(s.rebirth).isFalse();
        assertThat(s.autojoin).isFalse();
        assertThat(s.droptossup).isTrue();
        assertThat(s.physics).isFalse();
        assertThat(s.lvl_bar_mode).isFalse();
        assertThat(s.particle).isTrue();
        assertThat(s.allow_nether).isTrue();
        assertThat(s.protection).isFalse();
        assertThat(s.saveplayerinventory).isFalse();
        assertThat(s.border).isFalse();
        assertThat(s.CircleMode).isTrue();
        assertThat(s.UseEmptyIslands).isTrue();
        assertThat(s.progress_bar).isFalse();
        assertThat(s.phText).isEmpty();
    }

    @Test
    @DisplayName("Every public field carries the volatile modifier")
    void everyFieldIsVolatile() {
        // The whole point of Phase 3.3 keeping volatile (rather than
        // collapsing to one immutable record + AtomicReference swap) is
        // that single-flag toggles from the main thread (`/ob particle
        // false`) become visible to the async TaskParticle / Task readers
        // without an unrelated synchronized block. If a field is added
        // here without volatile, this test breaks immediately rather than
        // silently regressing under load.
        for (Field f : Settings.class.getDeclaredFields()) {
            int mods = f.getModifiers();
            // Skip synthetic / static helper fields; the real config
            // surface is non-static instance fields only.
            if (Modifier.isStatic(mods) || f.isSynthetic()) continue;
            assertThat(Modifier.isVolatile(mods))
                    .as("Settings.%s must be declared volatile", f.getName())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Two Settings instances are independent (per-test isolation)")
    void instancesAreIndependent() {
        // Tests rely on being able to construct a fresh Settings without
        // picking up state from a prior test. If a future change adds a
        // static singleton inside Settings itself (instead of on Oneblock)
        // this test catches it.
        Settings a = new Settings();
        Settings b = new Settings();
        a.particle = false;
        a.max_players_team = 7;
        assertThat(b.particle).isTrue();
        assertThat(b.max_players_team).isZero();
    }

    @Test
    @DisplayName("Cross-thread write-then-read: a volatile flip is observed without sync")
    void crossThreadVisibility() throws InterruptedException {
        // Single-writer / single-reader handshake. The reader spins until
        // it sees the flag flip, with a hard timeout so we don't hang the
        // suite if visibility ever regresses. A 1-second budget is
        // generous; in practice a volatile write becomes visible inside
        // a handful of nanoseconds.
        final Settings s = new Settings();
        final AtomicBoolean observed = new AtomicBoolean(false);
        final CountDownLatch readerStarted = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            readerStarted.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (System.nanoTime() < deadline) {
                if (s.protection) { observed.set(true); return; }
            }
        }, "settings-reader");
        reader.start();

        readerStarted.await();
        // Tiny pause so the reader has spun at least once observing the
        // initial false; not strictly required for correctness.
        Thread.yield();
        s.protection = true;

        reader.join(TimeUnit.SECONDS.toMillis(2));
        assertThat(observed.get())
                .as("reader should observe the volatile write to s.protection")
                .isTrue();
    }
}
