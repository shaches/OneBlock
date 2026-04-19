package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ChestItems#parseKey(String)} is a tolerant parser for vanilla
 * loot-table identifiers. The contract:
 *
 *   - accepts {@code minecraft:path} and {@code ns:path}
 *   - falls back to {@code minecraft} when the colon is missing
 *   - lowercases the input
 *   - returns {@code null} on empty / malformed inputs instead of throwing
 */
class ChestItemsParseKeyTest {

    @Test
    @DisplayName("null / empty input -> null")
    void nullEmpty() {
        assertThat(ChestItems.parseKey(null)).isNull();
        assertThat(ChestItems.parseKey("")).isNull();
    }

    @Test
    @DisplayName("bare path -> minecraft namespace")
    void barePathFallsBackToMinecraft() {
        NamespacedKey key = ChestItems.parseKey("chests/simple_dungeon");
        assertThat(key).isNotNull();
        assertThat(key.getNamespace()).isEqualTo("minecraft");
        assertThat(key.getKey()).isEqualTo("chests/simple_dungeon");
    }

    @Test
    @DisplayName("explicit minecraft:path")
    void explicitMinecraftNamespace() {
        NamespacedKey key = ChestItems.parseKey("minecraft:chests/abandoned_mineshaft");
        assertThat(key).isNotNull();
        assertThat(key.getNamespace()).isEqualTo("minecraft");
        assertThat(key.getKey()).isEqualTo("chests/abandoned_mineshaft");
    }

    @Test
    @DisplayName("custom namespace preserved")
    void customNamespacePreserved() {
        NamespacedKey key = ChestItems.parseKey("myplugin:loot/weapons");
        assertThat(key).isNotNull();
        assertThat(key.getNamespace()).isEqualTo("myplugin");
        assertThat(key.getKey()).isEqualTo("loot/weapons");
    }

    @Test
    @DisplayName("input is lowercased")
    void inputIsLowercased() {
        NamespacedKey key = ChestItems.parseKey("MINECRAFT:Chests/End_City_Treasure");
        assertThat(key).isNotNull();
        assertThat(key.getNamespace()).isEqualTo("minecraft");
        assertThat(key.getKey()).isEqualTo("chests/end_city_treasure");
    }

    @Test
    @DisplayName("malformed input -> null (no exception)")
    void malformedYieldsNull() {
        // Whitespace / invalid character sets are rejected by NamespacedKey's regex.
        assertThat(ChestItems.parseKey("bad key with spaces")).isNull();
        assertThat(ChestItems.parseKey("bad:  spaces")).isNull();
        assertThat(ChestItems.parseKey("!!:bang")).isNull();
    }

    @Test
    @DisplayName("extra colons: first split is namespace, rest is path")
    void extraColonsKeepInPath() {
        // The parser uses split(":", 2) — everything after the first colon
        // becomes the path. NamespacedKey's regex forbids ':' in the path, so
        // this should be rejected (null), not swallowed silently.
        assertThat(ChestItems.parseKey("a:b:c")).isNull();
    }
}
