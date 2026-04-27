package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the five {@link ConfigManager#readOrDefault} overloads
 * (String / int / double / boolean / List&lt;String&gt;). The contract:
 *
 * <ol>
 *   <li>If the key is absent or is the wrong YAML type, write the
 *       default into {@link Oneblock#config} and return that default.</li>
 *   <li>If the key is present and the right type, return the stored
 *       value without touching the config.</li>
 * </ol>
 *
 * <p>Phase 6 will rename a number of YAML keys; this test pins the
 * contract so a regression in the read-or-write fallback (for instance
 * forgetting the {@code config.set(...)} call, or missing the
 * {@code isString}/{@code isInt} type guard) shows up immediately.
 */
class ConfigManagerReadOrDefaultTest {

    private static YamlConfiguration savedConfig;

    @BeforeAll
    static void snapshot() {
        savedConfig = Oneblock.config;
    }

    @AfterAll
    static void restore() {
        Oneblock.config = savedConfig;
    }

    private ConfigManager cm;

    @BeforeEach
    void freshConfig() {
        cm = new ConfigManager();
        Oneblock.config = new YamlConfiguration();
    }

    // --------------------------------------------------------------
    // String overload
    // --------------------------------------------------------------

    @Test
    @DisplayName("readOrDefault(String): missing key writes the default, returns it")
    void stringMissingKeyWritesDefault() {
        String result = cm.readOrDefault("world", "world_default");
        assertThat(result).isEqualTo("world_default");
        assertThat(Oneblock.config.getString("world")).isEqualTo("world_default");
    }

    @Test
    @DisplayName("readOrDefault(String): present key returns stored value, leaves config alone")
    void stringPresentKeyReturnsStored() {
        Oneblock.config.set("world", "the_end");
        String result = cm.readOrDefault("world", "world_default");
        assertThat(result).isEqualTo("the_end");
        // Default must NOT have overwritten the stored value.
        assertThat(Oneblock.config.getString("world")).isEqualTo("the_end");
    }

    @Test
    @DisplayName("readOrDefault(String): wrong-type stored value is treated as missing")
    void stringWrongTypeFallsBackToDefault() {
        // YamlConfiguration.isString returns false for numeric values.
        Oneblock.config.set("world", 42);
        String result = cm.readOrDefault("world", "fallback");
        assertThat(result).isEqualTo("fallback");
    }

    // --------------------------------------------------------------
    // int overload
    // --------------------------------------------------------------

    @Test
    @DisplayName("readOrDefault(int): missing key writes the default, returns it")
    void intMissingKeyWritesDefault() {
        int result = cm.readOrDefault("set", 100);
        assertThat(result).isEqualTo(100);
        assertThat(Oneblock.config.getInt("set")).isEqualTo(100);
    }

    @Test
    @DisplayName("readOrDefault(int): present key returns stored value")
    void intPresentKeyReturnsStored() {
        Oneblock.config.set("set", 250);
        int result = cm.readOrDefault("set", 100);
        assertThat(result).isEqualTo(250);
    }

    // --------------------------------------------------------------
    // double overload
    // --------------------------------------------------------------

    @Test
    @DisplayName("readOrDefault(double): missing key writes the default, returns it")
    void doubleMissingKeyWritesDefault() {
        double result = cm.readOrDefault("xleave", 12.5);
        assertThat(result).isEqualTo(12.5);
        assertThat(Oneblock.config.getDouble("xleave")).isEqualTo(12.5);
    }

    @Test
    @DisplayName("readOrDefault(double): present key returns stored value")
    void doublePresentKeyReturnsStored() {
        Oneblock.config.set("xleave", -7.25);
        double result = cm.readOrDefault("xleave", 12.5);
        assertThat(result).isEqualTo(-7.25);
    }

    // --------------------------------------------------------------
    // boolean overload
    // --------------------------------------------------------------

    @Test
    @DisplayName("readOrDefault(boolean): missing key writes the default, returns it")
    void booleanMissingKeyWritesDefault() {
        boolean result = cm.readOrDefault("autojoin", true);
        assertThat(result).isTrue();
        assertThat(Oneblock.config.getBoolean("autojoin")).isTrue();
    }

    @Test
    @DisplayName("readOrDefault(boolean): present false key is read as false")
    void booleanPresentFalseReadAsFalse() {
        Oneblock.config.set("autojoin", false);
        boolean result = cm.readOrDefault("autojoin", true);
        assertThat(result).isFalse();
    }

    // --------------------------------------------------------------
    // List<String> overload
    // --------------------------------------------------------------

    @Test
    @DisplayName("readOrDefault(List<String>): missing key writes the default, returns its YAML readback")
    void listMissingKeyWritesDefault() {
        List<String> defaults = Arrays.asList("a", "b", "c");
        List<String> result = cm.readOrDefault("wgflags", defaults);
        assertThat(result).containsExactly("a", "b", "c");
        assertThat(Oneblock.config.getStringList("wgflags")).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("readOrDefault(List<String>): present list is returned, default ignored")
    void listPresentKeyReturnsStored() {
        Oneblock.config.set("wgflags", Arrays.asList("x", "y"));
        List<String> result = cm.readOrDefault("wgflags", Collections.singletonList("never"));
        assertThat(result).containsExactly("x", "y");
    }
}
