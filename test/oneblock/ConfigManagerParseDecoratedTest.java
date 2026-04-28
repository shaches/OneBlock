package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import oneblock.utils.Compat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ConfigManager#parsePoolEntry(Object, Level)} and
 * {@link ConfigManager#resolveDecorated(Map)} for the new {@code decorated:} YAML syntax.
 */
class ConfigManagerParseDecoratedTest {

  @Test
  @DisplayName("parsePoolEntry adds a decorated entry with custom base, chance, offset and decorations")
  void decoratedMapEntryIsAddedToBlockPool() {
    ConfigManager cm = new ConfigManager();
    Level level = new Level("Test");

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("decorated", "STONE");
    m.put("chance", 4);
    m.put("offset_y", 2);
    List<String> decorations = new ArrayList<>();
    decorations.add("RED_MUSHROOM");
    decorations.add("BROWN_MUSHROOM");
    m.put("decorations", decorations);

    cm.parsePoolEntry(m, level);

    assertThat(level.blockPoolSize()).isEqualTo(1);
    PoolEntry entry = level.blockPool.pick(new java.util.Random(0));
    assertThat(entry.kind).isEqualTo(PoolEntry.Kind.DECORATED_BLOCK);
    assertThat(entry.value).isInstanceOf(DecoratedBlock.class);

    DecoratedBlock d = (DecoratedBlock) entry.value;
    assertThat(d.base()).isEqualTo(XMaterial.STONE);
    assertThat(d.chance()).isEqualTo(4);
    assertThat(d.offsetY()).isEqualTo(2);
    assertThat(d.decorations()).containsExactly(XMaterial.RED_MUSHROOM, XMaterial.BROWN_MUSHROOM);
  }

  @Test
  @DisplayName("parsePoolEntry adds decorated entry with defaults when optional keys are omitted")
  void decoratedDefaults() {
    ConfigManager cm = new ConfigManager();
    Level level = new Level("Test");

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("decorated", "GRASS_BLOCK");

    cm.parsePoolEntry(m, level);

    assertThat(level.blockPoolSize()).isEqualTo(1);
    PoolEntry entry = level.blockPool.pick(new java.util.Random(0));
    assertThat(entry.kind).isEqualTo(PoolEntry.Kind.DECORATED_BLOCK);

    DecoratedBlock d = (DecoratedBlock) entry.value;
    assertThat(d.base()).isEqualTo(Compat.GRASS_BLOCK);
    assertThat(d.chance()).isEqualTo(3);   // default
    assertThat(d.offsetY()).isEqualTo(1);  // default
    assertThat(d.decorations()).isNull();  // falls back to global flowers
  }

  @Test
  @DisplayName("resolveDecorated tolerates unknown base material and falls back to grass")
  void unknownBaseFallsBackToGrass() {
    ConfigManager cm = new ConfigManager();

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("decorated", "TOTALLY_FAKE_BLOCK");

    PoolEntry entry = cm.resolveDecorated(m);
    assertThat(entry.kind).isEqualTo(PoolEntry.Kind.DECORATED_BLOCK);
    assertThat(((DecoratedBlock) entry.value).base()).isEqualTo(Compat.GRASS_BLOCK);
  }

  @Test
  @DisplayName("decorated entry with weight > 1 is added with correct weight")
  void decoratedWithWeight() {
    ConfigManager cm = new ConfigManager();
    Level level = new Level("Test");

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("decorated", "SAND");
    m.put("weight", 5);

    cm.parsePoolEntry(m, level);

    assertThat(level.blockPoolSize()).isEqualTo(1);
    assertThat(level.blockPool.totalWeight()).isEqualTo(5);
  }
}
