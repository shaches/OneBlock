package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Factory + {@code toString()} coverage for {@link PoolEntry}, the tagged union used by {@link
 * Level}'s block pool. {@code Kind.MOB} is intentionally absent because mobs live in a separate
 * {@code WeightedPool<EntityType>}.
 */
class PoolEntryTest {

  @Test
  @DisplayName("GRASS singleton: DEFAULT_GRASS kind, null value, stable toString")
  void grassSingleton() {
    assertThat(PoolEntry.GRASS.kind).isEqualTo(PoolEntry.Kind.DEFAULT_GRASS);
    assertThat(PoolEntry.GRASS.value).isNull();
    assertThat(PoolEntry.GRASS.toString()).isEqualTo("Grass (default)");
  }

  @Test
  @DisplayName("block(material): BLOCK kind, value retained, toString delegates to value")
  void blockFactory() {
    PoolEntry e = PoolEntry.block("STONE");
    assertThat(e.kind).isEqualTo(PoolEntry.Kind.BLOCK);
    assertThat(e.value).isEqualTo("STONE");
    assertThat(e.toString()).isEqualTo("STONE");
  }

  @Test
  @DisplayName("block(null): BLOCK kind, toString falls back to 'Grass (undefined)'")
  void blockFactoryNull() {
    // The BLOCK branch of toString() has an explicit null guard that
    // renders as "Grass (undefined)" rather than NPE'ing. Document.
    PoolEntry e = PoolEntry.block(null);
    assertThat(e.kind).isEqualTo(PoolEntry.Kind.BLOCK);
    assertThat(e.value).isNull();
    assertThat(e.toString()).isEqualTo("Grass (undefined)");
  }

  @Test
  @DisplayName(
      "lootTable(key): LOOT_TABLE kind, value is the key, toString includes 'loot_table: '")
  void lootTableFactory() {
    NamespacedKey key = NamespacedKey.minecraft("chests/simple_dungeon");
    PoolEntry e = PoolEntry.lootTable(key);
    assertThat(e.kind).isEqualTo(PoolEntry.Kind.LOOT_TABLE);
    assertThat(e.value).isSameAs(key);
    assertThat(e.toString()).startsWith("loot_table: ").contains("chests/simple_dungeon");
  }

  @Test
  @DisplayName("command(cmd): COMMAND kind, value verbatim, toString includes 'command: '")
  void commandFactory() {
    PoolEntry e = PoolEntry.command("/give %s 1 diamond");
    assertThat(e.kind).isEqualTo(PoolEntry.Kind.COMMAND);
    assertThat(e.value).isEqualTo("/give %s 1 diamond");
    assertThat(e.toString()).isEqualTo("command: /give %s 1 diamond");
  }

  @Test
  @DisplayName("Factories accept nulls without NPE (current behaviour; regression guard)")
  void factoriesAllowNullPayloads() {
    // No runtime guard is enforced at construction time. This test locks
    // the current behaviour so a future guard-add is a deliberate choice.
    assertThat(PoolEntry.lootTable(null).value).isNull();
    assertThat(PoolEntry.command(null).value).isNull();
  }

  @Test
  @DisplayName("chest(alias): CHEST kind, value is the alias, toString includes 'chest: '")
  void chestFactory() {
    PoolEntry e = PoolEntry.chest("starter");
    assertThat(e.kind).isEqualTo(PoolEntry.Kind.CHEST);
    assertThat(e.value).isEqualTo("starter");
    assertThat(e.toString()).isEqualTo("chest: starter");
  }

  @Test
  @DisplayName("Kind enum is closed: five cases, no MOB")
  void kindEnumShape() {
    PoolEntry.Kind[] all = PoolEntry.Kind.values();
    assertThat(all)
        .containsExactlyInAnyOrder(
            PoolEntry.Kind.BLOCK,
            PoolEntry.Kind.LOOT_TABLE,
            PoolEntry.Kind.CHEST,
            PoolEntry.Kind.COMMAND,
            PoolEntry.Kind.DEFAULT_GRASS);
  }
}
