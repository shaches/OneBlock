package oneblock.command.sub;

import oneblock.ChestItems;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;
import oneblock.gui.GUI;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;

/**
 * {@code /ob chest [name [set <key> | edit]]} - admin. Read or rewrite the mapping from chest-alias
 * to vanilla loot-table {@link NamespacedKey} or legacy {@code ItemStack} list stored in {@code
 * chests.yml}. With no args, lists every alias and its current key/item-count. With one arg
 * (alias), prints that alias's current mode. With three args ({@code <alias> set <key>}), rewrites
 * the loot-table mapping. With three args ({@code <alias> edit}), opens the chest GUI for editing
 * legacy item-list chests.
 */
public final class ChestCommand implements Subcommand {
  @Override
  public String name() {
    return "chest";
  }

  @Override
  public String permission() {
    return "Oneblock.set";
  }

  @Override
  public boolean execute(CommandContext ctx) {
    AdminPrelude.run(ctx);
    String[] args = ctx.args();
    if (args.length < 2) {
      if (ChestItems.getChestNames().isEmpty()) {
        ctx.sender()
            .sendMessage(
                ChatColor.YELLOW
                    + "No chest aliases configured. Define them in chests.yml as 'name:"
                    + " minecraft:chests/<loot_table>'.");
        return true;
      }
      for (String name : ChestItems.getChestNames()) {
        NamespacedKey k = ChestItems.resolve(name);
        int legacyCount = ChestItems.getItems(name) != null ? ChestItems.getItems(name).size() : 0;
        String mode =
            k != null
                ? "loot_table: " + k
                : (legacyCount > 0 ? "legacy items (" + legacyCount + ")" : "<unset>");
        ctx.sender()
            .sendMessage(ChatColor.GREEN + name + ChatColor.GRAY + " -> " + ChatColor.WHITE + mode);
      }
      return true;
    }
    String chestName = args[1];
    if (args.length < 3) {
      NamespacedKey current = ChestItems.resolve(chestName);
      if (current != null) {
        ctx.sender()
            .sendMessage(
                ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + current);
        ctx.sender()
            .sendMessage(
                ChatColor.GRAY + "Usage: /ob chest " + chestName + " set <namespaced_key> | edit");
        return true;
      }
      if (ChestItems.getItems(chestName) != null) {
        ctx.sender()
            .sendMessage(
                ChatColor.GREEN + chestName + ChatColor.GRAY + " -> legacy item-list chest");
        ctx.sender().sendMessage(ChatColor.GRAY + "Usage: /ob chest " + chestName + " edit");
        return true;
      }
      ctx.sender()
          .sendMessage(
              ChatColor.YELLOW
                  + "No mapping for '"
                  + chestName
                  + "'. Usage: /ob chest "
                  + chestName
                  + " set <namespaced_key> | edit");
      return true;
    }
    String sub = args[2].toLowerCase();
    if (sub.equals("edit")) {
      if (ctx.player() == null) {
        ctx.sender().sendMessage(ChatColor.RED + "This command can only be used by a player.");
        return true;
      }
      GUI.chestGUI(ctx.player(), chestName);
      return true;
    }
    if (!sub.equals("set") || args.length < 4) {
      ctx.sender()
          .sendMessage(ChatColor.RED + "Usage: /ob chest <name> [set <namespaced_key> | edit]");
      return true;
    }
    NamespacedKey newKey = ChestItems.parseKey(args[3]);
    if (newKey == null) {
      ctx.sender().sendMessage(ChatColor.RED + "Invalid namespaced key '" + args[3] + "'.");
      return true;
    }
    ChestItems.setAlias(chestName, newKey);
    ChestItems.save();
    ctx.sender()
        .sendMessage(
            ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + newKey);
    return true;
  }
}
