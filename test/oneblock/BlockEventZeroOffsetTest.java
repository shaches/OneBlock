package oneblock;

import static org.assertj.core.api.Assertions.assertThatCode;

import oneblock.events.BlockEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Regression test for the divide-by-zero crash in {@link BlockEvent#onItemStackSpawn} when the
 * plugin has not yet been configured ({@code offset == 0}).
 */
class BlockEventZeroOffsetTest {

  private IslandOrigin savedOrigin;
  private boolean savedDropTossUp;

  @BeforeEach
  void snapshot() {
    savedOrigin = Oneblock.ORIGIN.get();
    savedDropTossUp = Oneblock.settings().dropTossUp;
  }

  @AfterEach
  void restore() {
    Oneblock.ORIGIN.set(savedOrigin);
    Oneblock.settings().dropTossUp = savedDropTossUp;
  }

  @Test
  @DisplayName("onItemStackSpawn must not throw when offset is zero (unconfigured plugin)")
  void itemStackSpawnSurvivesZeroOffset() {
    World mockWorld = Mockito.mock(World.class);
    Oneblock.ORIGIN.set(new IslandOrigin(mockWorld, 0, 64, 0, 0));
    Oneblock.settings().dropTossUp = true;

    Item mockItem = Mockito.mock(Item.class);
    Location loc = new Location(mockWorld, 0, 64, 0);
    Mockito.when(mockItem.getLocation()).thenReturn(loc);

    ItemSpawnEvent event = Mockito.mock(ItemSpawnEvent.class);
    Mockito.when(event.getEntity()).thenReturn(mockItem);

    BlockEvent handler = new BlockEvent();

    assertThatCode(() -> handler.onItemStackSpawn(event)).doesNotThrowAnyException();
  }
}
