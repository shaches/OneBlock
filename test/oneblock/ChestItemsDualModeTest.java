package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for {@link ChestItems} dual-mode support: modern {@link NamespacedKey}
 * loot-table aliases and legacy {@link ItemStack} list aliases.
 */
class ChestItemsDualModeTest {

  private File tempChests;

  @BeforeEach
  void reset() throws Exception {
    tempChests = Files.createTempFile("chests", ".yml").toFile();
    tempChests.deleteOnExit();
    ChestItems.chest = tempChests;
    ChestItems.load();
  }

  @Test
  @DisplayName("empty chests.yml -> no aliases, no legacy items")
  void emptyFile() {
    assertThat(ChestItems.getChestNames()).isEmpty();
    assertThat(ChestItems.resolve("missing")).isNull();
    assertThat(ChestItems.getItems("missing")).isNull();
    assertThat(ChestItems.hasChest("missing")).isFalse();
  }

  @Test
  @DisplayName("setAlias + save + load round-trip for NamespacedKey")
  void lootTableAliasRoundTrip() {
    NamespacedKey key = NamespacedKey.minecraft("chests/simple_dungeon");
    ChestItems.setAlias("dungeon", key);
    ChestItems.save();
    ChestItems.load();

    assertThat(ChestItems.resolve("dungeon")).isEqualTo(key);
    assertThat(ChestItems.getItems("dungeon")).isNull();
    assertThat(ChestItems.hasChest("dungeon")).isTrue();
    assertThat(ChestItems.getChestNames()).contains("dungeon");
  }

  @Test
  @DisplayName(
      "setItems + in-memory persistence (skip save/load due to Bukkit ItemStack serialization"
          + " limitation)")
  void legacyItemListInMemory() {
    ItemStack diamond = new ItemStack(Material.DIAMOND, 3);
    ItemStack stone = new ItemStack(Material.STONE, 1);
    ChestItems.setItems("starter", Arrays.asList(diamond, stone));

    List<ItemStack> loaded = ChestItems.getItems("starter");
    assertThat(loaded).isNotNull();
    assertThat(loaded).hasSize(2);
    assertThat(loaded.get(0).getType()).isEqualTo(Material.DIAMOND);
    assertThat(loaded.get(0).getAmount()).isEqualTo(3);
    assertThat(loaded.get(1).getType()).isEqualTo(Material.STONE);
    assertThat(ChestItems.resolve("starter")).isNull();
    assertThat(ChestItems.hasChest("starter")).isTrue();
  }

  @Test
  @DisplayName("mixed in-memory: one loot-table alias and one legacy list coexist")
  void mixedAliasesCoexist() {
    ChestItems.setAlias("dungeon", NamespacedKey.minecraft("chests/simple_dungeon"));
    ChestItems.setItems("starter", Collections.singletonList(new ItemStack(Material.APPLE)));

    assertThat(ChestItems.getChestNames()).containsExactlyInAnyOrder("dungeon", "starter");
    assertThat(ChestItems.resolve("dungeon")).isNotNull();
    assertThat(ChestItems.getItems("dungeon")).isNull();
    assertThat(ChestItems.resolve("starter")).isNull();
    assertThat(ChestItems.getItems("starter")).isNotNull();
  }

  @Test
  @DisplayName("getItems returns unmodifiable list")
  void getItemsUnmodifiable() {
    ChestItems.setItems("test", Arrays.asList(new ItemStack(Material.DIRT)));
    List<ItemStack> items = ChestItems.getItems("test");
    assertThat(items).isNotNull();
    // Should throw if caller tries to mutate the returned list
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> items.clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("removeAlias deletes loot-table entry")
  void removeAliasDeletesLootTable() {
    ChestItems.setAlias("x", NamespacedKey.minecraft("chests/abandoned_mineshaft"));

    assertThat(ChestItems.hasChest("x")).isTrue();
    assertThat(ChestItems.removeAlias("x")).isTrue();
    assertThat(ChestItems.hasChest("x")).isFalse();
    assertThat(ChestItems.resolve("x")).isNull();
  }

  @Test
  @DisplayName("removeAlias deletes legacy item-list entry")
  void removeAliasDeletesLegacyItems() {
    ChestItems.setItems("x", Collections.singletonList(new ItemStack(Material.GOLD_INGOT)));

    assertThat(ChestItems.hasChest("x")).isTrue();
    assertThat(ChestItems.removeAlias("x")).isTrue();
    assertThat(ChestItems.hasChest("x")).isFalse();
    assertThat(ChestItems.getItems("x")).isNull();
  }

  @Test
  @DisplayName("setItems with null name or list is a no-op")
  void setItemsNullSafety() {
    ChestItems.setItems(null, Arrays.asList(new ItemStack(Material.DIRT)));
    ChestItems.setItems("test", null);
    assertThat(ChestItems.hasChest("test")).isFalse();
  }
}
