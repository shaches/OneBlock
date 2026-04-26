package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Security-critical coverage: {@link RewardManager#executeRewards} refuses to
 * dispatch any console command unless the player's in-game name matches the
 * private {@code SAFE_PLAYER_NAME} pattern. A regression that loosens this
 * regex would re-open the command-injection vector that this gate closes.
 *
 * <p>The rest of {@code executeRewards} depends on {@code Oneblock.plugin},
 * {@code Bukkit.getConsoleSender()}, and {@code Bukkit.dispatchCommand} which
 * require a live Bukkit server to exercise meaningfully. Those paths are left
 * for an integration test (outside the scope of the Phase 5 unit suite).
 */
class RewardManagerTest {

    @Test
    @DisplayName("SAFE_PLAYER_NAME accepts valid Mojang names and rejects everything else")
    void safePlayerNameRegex() throws Exception {
        Field f = RewardManager.class.getDeclaredField("SAFE_PLAYER_NAME");
        f.setAccessible(true);
        Pattern p = (Pattern) f.get(null);

        // accepted: [A-Za-z0-9_]{1,16}
        String[] ok = {
                "a", "A", "0", "_", "Notch",
                "Mojang_User123",
                "abcdefghijklmnop",       // 16 chars exactly
                "Dinnerbone"
        };
        for (String name : ok)
            assertThat(p.matcher(name).matches()).as("should accept: '" + name + "'").isTrue();

        // rejected: anything else (too long, wrong chars, injection attempts, empty)
        String[] bad = {
                "",
                "abcdefghijklmnopq",      // 17 chars
                "has space",
                "semi;colon",
                "pipe|command",
                "ampersand&bg",
                "`backtick`",
                "quote'name",
                "\"dquote",
                "öäü-unicode",
                "rm -rf /",
                "/op Attacker",
                "nick\nnewline"
        };
        for (String name : bad)
            assertThat(p.matcher(name).matches()).as("should reject: '" + name + "'").isFalse();
    }

    @Test
    @DisplayName("SAFE_PLAYER_NAME is anchored at both ends (no partial matches)")
    void anchoredRegex() throws Exception {
        Field f = RewardManager.class.getDeclaredField("SAFE_PLAYER_NAME");
        f.setAccessible(true);
        Pattern p = (Pattern) f.get(null);

        // A prefix that is valid should still be rejected if followed by anything invalid.
        assertThat(p.matcher("Notch;evil").matches()).isFalse();
        assertThat(p.matcher("evil;Notch").matches()).isFalse();
    }
}
