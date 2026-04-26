package oneblock.command;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import oneblock.LegacyConfigSaver;
import oneblock.Oneblock;

/**
 * Shared "load config from disk, schedule save 2 ticks later" prelude
 * that the legacy admin {@code default:} block ran around every
 * {@code Oneblock.set}-gated subcommand. Phase 3.5b admin extractions
 * each call {@link #run} at the top of their {@code execute} body so
 * the on-disk re-read + delayed save behaviour is preserved one-for-one.
 *
 * <p>The reload-then-save dance is load-bearing: it picks up any manual
 * edits an operator made to {@code config.yml} between commands, and
 * the 2-tick delay lets the command's own {@code config.set(...)} writes
 * land before the file is overwritten. Match the legacy ordering when
 * adding new admin subcommands.
 */
public final class AdminPrelude {
    private AdminPrelude() {}

    public static void run(CommandContext ctx) {
        final java.io.File f = Oneblock.configManager.getMainConfigFile();
        Oneblock.config = YamlConfiguration.loadConfiguration(f);
        Bukkit.getScheduler().runTaskLater(ctx.plugin(),
                () -> LegacyConfigSaver.save(Oneblock.config, f), 2L);
    }
}
