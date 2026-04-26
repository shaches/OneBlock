package oneblock.command.sub;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import oneblock.Level;
import oneblock.PoolEntry;
import oneblock.WeightedPool;
import oneblock.command.AdminPrelude;
import oneblock.command.CommandContext;
import oneblock.command.Subcommand;

/**
 * {@code /ob listlvl [index]} - admin. Without arg, lists every level
 * in the configured pool by name. With a numeric arg, prints the
 * named level's full block + mob spawn pool with weights, useful for
 * debugging weight-distribution after a {@code blocks.yml} edit.
 *
 * <p>Behaviour-equivalent to the legacy {@code "listlvl"} admin case
 * extracted in Phase 3.5b.
 */
public final class ListLvlCommand implements Subcommand {
    @Override public String name() { return "listlvl"; }
    @Override public String permission() { return "Oneblock.set"; }

    @Override
    public boolean execute(CommandContext ctx) {
        AdminPrelude.run(ctx);
        String[] args = ctx.args();
        if (args.length > 1) {
            int temp = 0;
            try {
                temp = Integer.parseInt(args[1]);
                if (temp < 0 || temp >= Level.size()) throw new NumberFormatException();
            } catch (NumberFormatException nfe) {
                ctx.sender().sendMessage(String.format("%sundefined lvl", ChatColor.RED));
                return true;
            }
            Level lvl = Level.get(temp);
            ctx.sender().sendMessage(String.format("%s%s %s(weight total: %d)", ChatColor.GREEN, lvl.name, ChatColor.GRAY,
                    lvl.blockPool.totalWeight() + lvl.mobPool.totalWeight()));
            for (WeightedPool.Entry<PoolEntry> e : lvl.blockPool.entries())
                ctx.sender().sendMessage("  " + e.value + " (weight " + e.weight + ")");
            for (WeightedPool.Entry<EntityType> e : lvl.mobPool.entries())
                ctx.sender().sendMessage("  mob: " + e.value + " (weight " + e.weight + ")");
            return true;
        }
        for (int i = 0; i < Level.size(); i++)
            ctx.sender().sendMessage(String.format("%d: %s%s", i, ChatColor.GREEN, Level.get(i).name));
        return true;
    }
}
