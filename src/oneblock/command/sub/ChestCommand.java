package oneblock.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;

import oneblock.ChestItems;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob chest [name [set <key>]]} - admin. Read or rewrite the
 * mapping from chest-alias to vanilla loot-table {@link NamespacedKey}
 * stored in {@code chests.yml}. With no args, lists every alias and
 * its current key. With one arg (alias), prints that alias's current
 * key (or a usage hint). With four args (`<alias> set <key>`),
 * rewrites the mapping and persists.
 *
 * <p>Behaviour-equivalent to the legacy {@code "chest"} admin case
 * extracted in Phase 3.5b.4.
 */
public final class ChestCommand implements Subcommand {
    @Override public String name() { return "chest"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length < 2) {
            if (ChestItems.getChestNames().isEmpty()) {
                ctx.sender().sendMessage(ChatColor.YELLOW + "No chest aliases configured. Define them in chests.yml as 'name: minecraft:chests/<loot_table>'.");
                return true;
            }
            for (String name : ChestItems.getChestNames()) {
                NamespacedKey k = ChestItems.resolve(name);
                ctx.sender().sendMessage(ChatColor.GREEN + name + ChatColor.GRAY + " -> " + ChatColor.WHITE + (k == null ? "<unset>" : k));
            }
            return true;
        }
        String chestName = args[1];
        if (args.length < 3) {
            NamespacedKey current = ChestItems.resolve(chestName);
            if (current == null) {
                ctx.sender().sendMessage(ChatColor.YELLOW + "No loot-table mapping for '" + chestName + "'. Usage: /ob chest " + chestName + " set <namespaced_key>");
            } else {
                ctx.sender().sendMessage(ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + current);
                ctx.sender().sendMessage(ChatColor.GRAY + "Usage: /ob chest " + chestName + " set <namespaced_key>");
            }
            return true;
        }
        if (!args[2].equalsIgnoreCase("set") || args.length < 4) {
            ctx.sender().sendMessage(ChatColor.RED + "Usage: /ob chest <name> [set <namespaced_key>]");
            return true;
        }
        NamespacedKey newKey = ChestItems.parseKey(args[3]);
        if (newKey == null) {
            ctx.sender().sendMessage(ChatColor.RED + "Invalid namespaced key '" + args[3] + "'.");
            return true;
        }
        ChestItems.setAlias(chestName, newKey);
        ChestItems.save();
        ctx.sender().sendMessage(ChatColor.GREEN + chestName + ChatColor.GRAY + " -> " + ChatColor.WHITE + newKey);
        return true;
    }
}
