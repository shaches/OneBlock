package oneblock.command;

/**
 * One leaf of the {@code /oneblock} subcommand tree. Each implementation
 * owns the parsing, permission gates, and execution of a single
 * {@code args[0]} token (e.g. {@code "join"}, {@code "set"},
 * {@code "particle"}).
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link CommandRouter} resolves {@link #name()} (case-insensitive)
 *       in its registry.</li>
 *   <li>If found, the router enforces {@link #requiresPlayer()} and
 *       {@link #permission()} before calling {@link #execute}.</li>
 *   <li>Implementations receive a fully-built {@link CommandContext}
 *       and may freely re-dispatch (e.g. by calling
 *       {@code ctx.player().performCommand("ob j")}).</li>
 * </ol>
 *
 * <p>Implementations should be stateless; the router instantiates each
 * subcommand once at startup and shares it across all senders. Per-call
 * scratch state lives in the {@link CommandContext} or in local
 * variables.
 *
 * <p>Phase 3.5a introduces the contract; Phase 3.5b extracts the
 * existing 649-line switch in {@code CommandHandler.onCommand} into
 * concrete implementations under {@code oneblock.command.sub.*}.
 */
public interface Subcommand {

    /**
     * Lower-case command keyword matched against {@code args[0]}.
     * Examples: {@code "join"}, {@code "set"}, {@code "particle"}.
     * Aliases (e.g. {@code "j"} for {@code "join"}) are registered
     * separately in {@link CommandRouter}.
     */
    String name();

    /**
     * Permission node required to execute this subcommand. {@code null}
     * means the umbrella {@code Oneblock.join} permission already
     * checked by the router suffices and no per-command gate applies.
     * Admin commands typically return {@code "Oneblock.set"}; the few
     * with their own node return e.g. {@code "Oneblock.invite"}.
     */
    default String permission() { return null; }

    /**
     * Whether this command is meaningless from console (e.g. needs a
     * {@link org.bukkit.entity.Player} to teleport, open a GUI, or
     * read coordinates from). The router replies with a polite refusal
     * when {@code true} and the sender is not a {@code Player}.
     */
    default boolean requiresPlayer() { return false; }

    /**
     * Execute the subcommand. Return value matches Bukkit's
     * {@code CommandExecutor.onCommand} contract: {@code true} for
     * "handled" (suppress the {@code usage:} line from {@code plugin.yml}),
     * {@code false} to fall back on Bukkit's auto-usage message.
     */
    boolean execute(CommandContext ctx);
}
