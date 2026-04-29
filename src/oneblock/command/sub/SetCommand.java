package oneblock.command.sub;

import oneblock.LegacyConfigSaver;
import oneblock.Messages;
import oneblock.Oneblock;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.utils.Compat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * {@code /ob set [offset [x y z [world]]]} - admin. The big setup command: sets the cell-edge
 * length ({@code offset}) and the island grid origin coordinates. From the console, all four args
 * are required; from a player, omitting the position uses the player's current location. After
 * setting, recreates WorldGuard regions and places the central grass block.
 *
 * <p>Behaviour-equivalent to the legacy {@code "set"} admin case extracted in Phase 3.5b.4. The arg
 * validation rejects {@code offset == 0}, {@code |offset| > 10000}, and any non-integer position
 * component.
 */
public final class SetCommand implements Subcommand {
  @Override
  public String name() {
    return "set";
  }

  @Override
  public String permission() {
    return "Oneblock.set";
  }

  @Override
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
      justification = "getBlockAt() never returns null in Bukkit; SpotBugs false positive")
  public boolean execute(CommandContext ctx) {
    AdminPrelude.run(ctx);
    Player player = ctx.player();
    String[] args = ctx.args();
    if (player == null && args.length < 6) {
      ctx.sender()
          .sendMessage(ChatColor.RED + "Usage from console: /ob set <offset> <x> <y> <z> [world]");
      return true;
    }

    if (args.length >= 2) {
      try {
        int off_set = Integer.parseInt(args[1]);
        if (off_set == 0 || off_set > 10000 || off_set < -10000) throw new NumberFormatException();
        ctx.plugin().setOffset(off_set);
      } catch (NumberFormatException nfe) {
        ctx.sender().sendMessage(Messages.invalid_value);
        return true;
      }
    }

    Location location;
    if (args.length >= 5) {
      try {
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        int z = Integer.parseInt(args[4]);
        World world =
            args.length >= 6
                ? Bukkit.getWorld(args[5])
                : (player != null ? player.getWorld() : null);
        if (world == null) {
          ctx.sender().sendMessage(ChatColor.YELLOW + "World not found!");
          return true;
        }
        location = new Location(world, x, y, z);
      } catch (NumberFormatException nfe) {
        ctx.sender().sendMessage(Messages.invalid_value);
        return true;
      }
    } else if (player != null) {
      location = player.getLocation();
    } else {
      ctx.sender()
          .sendMessage(ChatColor.RED + "Usage from console: /ob set <offset> <x> <y> <z> [world]");
      return true;
    }

    // plugin.setOffset above already persisted `set` to config; no explicit set needed here.
    ctx.plugin().setPosition(location);

    if (Oneblock.getOffset() == 0) ctx.plugin().setOffset(100);

    if (Oneblock.getOffset() == 0) {
      ctx.sender().sendMessage(ChatColor.RED + "Island offset cannot be zero.");
      return true;
    }

    if (!ctx.plugin().enabled) ctx.plugin().runMainTask();

    World world = Oneblock.getWorld();
    if (world != null) {
      org.bukkit.block.Block block =
          java.util.Objects.requireNonNull(
              world.getBlockAt(Oneblock.getX(), Oneblock.getY(), Oneblock.getZ()));
      block.setType(Compat.GRASS_BLOCK.get());
      ctx.plugin().worldGuard.recreateRegions();
      LegacyConfigSaver.save(Oneblock.config, Oneblock.configManager.getMainConfigFile());

      ctx.sender()
          .sendMessage(
              String.format(
                  "%sset OneBlock on: %n%s%s, %s, %s%s in world %s%s",
                  ChatColor.GREEN,
                  ChatColor.WHITE,
                  Oneblock.getX(),
                  Oneblock.getY(),
                  Oneblock.getZ(),
                  ChatColor.GRAY,
                  ChatColor.WHITE,
                  world.getName()));
    } else {
      ctx.sender().sendMessage(ChatColor.RED + "World not initialized yet.");
    }
    return true;
  }
}
