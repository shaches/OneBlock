package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that Bukkit's YamlConfiguration actually hands back Map-shaped objects
 * for the decorated YAML syntax, so that parsePoolEntry can recognise them.
 */
class ConfigManagerYamlDecoratedTest {

  @Test
  @DisplayName("Bukkit YamlConfiguration preserves decorated map entries in getList()")
  void bukitYamlReturnsMapForDecoratedEntry() {
    String yaml =
        "'0':\n"
            + "- 'Level: 0'\n"
            + "- GREEN\n"
            + "- SEGMENTED_20\n"
            + "- 16\n"
            + "- GRASS_BLOCK\n"
            + "- decorated: STONE\n"
            + "  chance: 4\n"
            + "  offset_y: 1\n"
            + "  decorations:\n"
            + "    - RED_MUSHROOM\n"
            + "    - BROWN_MUSHROOM\n"
            + "- CREEPER\n";

    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new StringReader(yaml));
    List<?> list = cfg.getList("0");
    assertThat(list).hasSize(7);

    Object decoratedRaw = list.get(5);
    assertThat(decoratedRaw).isInstanceOf(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> m = (Map<String, Object>) decoratedRaw;
    assertThat(m).containsKey("decorated");
    assertThat(m.get("decorated")).isEqualTo("STONE");
    assertThat(m.get("chance")).isEqualTo(4);
    assertThat(m.get("offset_y")).isEqualTo(1);
    assertThat(m.get("decorations")).isInstanceOf(List.class);
  }

  @Test
  @DisplayName("parsePoolEntry works on the Map produced by Bukkit YamlConfiguration")
  void parseBukkitYamlDecoratedEntry() {
    String yaml =
        "'0':\n"
            + "- 'Level: 0'\n"
            + "- GREEN\n"
            + "- SEGMENTED_20\n"
            + "- 16\n"
            + "- decorated: SAND\n"
            + "  chance: 10\n"
            + "  offset_y: 1\n"
            + "  decorations:\n"
            + "    - DEAD_BUSH\n"
            + "- CREEPER\n";

    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new StringReader(yaml));
    List<?> list = cfg.getList("0");

    ConfigManager cm = new ConfigManager();
    Level level = new Level("Test");

    // Parse only the decorated entry (index 4, after the 4 header items)
    cm.parsePoolEntry(list.get(4), level);

    assertThat(level.blockPoolSize()).isEqualTo(1);
    PoolEntry entry = level.blockPool.pick(new java.util.Random(0));
    assertThat(entry.kind).isEqualTo(PoolEntry.Kind.DECORATED_BLOCK);
    assertThat(entry.value).isInstanceOf(DecoratedBlock.class);
  }
}
