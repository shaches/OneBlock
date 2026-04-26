package oneblock.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import oneblock.Oneblock;

/**
 * Per-call scratch bag passed to every {@link Subcommand}. Values are
 * captured once at the entry point of {@link CommandRouter#onCommand}
 * and are read-only for the duration of the dispatch.
 *
 * <p>Phase 3.5a introduces this record so subcommand impls don't have
 * to repeat the {@code Player player = sender instanceof Player ?
 * (Player) sender : null;} boilerplate that the legacy 649-line switch
 * carried at every leaf.
 *
 * @param sender the original {@link CommandSender} (player or console)
 * @param player non-null iff the sender is a {@link Player}; saves
 *               implementations the {@code instanceof} cast
 * @param args   the original command tokens, including {@code args[0]}
 *               which the router already used to dispatch
 * @param plugin the plugin singleton, for callers that need access to
 *               instance state (config manager, OBWG, scheduler)
 */
public record CommandContext(
        CommandSender sender,
        Player player,
        String[] args,
        Oneblock plugin) {
}
