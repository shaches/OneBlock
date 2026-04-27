package oneblock.config;

/**
 * Mutable runtime-configuration bag for the Oneblock plugin. Holds every admin-toggleable flag
 * previously kept as a {@code public static volatile} field on {@link oneblock.Oneblock}; folding
 * them into a single object gives Phase 3 one cohesive type to test against and to swap atomically
 * if a future {@code /ob reload} ever needs all-or-nothing visibility (today each flag still flips
 * individually).
 *
 * <p>Phase 6.2 (originally scheduled as Phase 3.7) finished the snake_case -> camelCase rename of
 * these fields across the whole tree. The on-disk YAML keys remain unchanged ({@code
 * mob_spawn_chance}, {@code progress_bar}, {@code droptossup}, ...) - {@code
 * ConfigManager.readOrDefault} is the single bridge between the YAML key strings and the Java field
 * names.
 *
 * <p>Each field is {@code volatile} so single-value writes from the main thread (admin commands,
 * {@code ConfigManager.loadMainConfig()}) are visible to async-scheduler readers ({@code
 * TaskParticle}, {@code Task}, placeholder expansion in {@code OBP}) without an unrelated
 * synchronized block. Cross-field consistency (e.g. several flags changing together) is not
 * guaranteed and was never guaranteed under the legacy layout either; a full atomic snapshot would
 * require an immutable record + an {@link java.util.concurrent.atomic.AtomicReference} swap, which
 * we defer until a real use case appears.
 *
 * <p>Construction is parameterless; the canonical singleton lives at {@link
 * oneblock.Oneblock#settings()}. Tests can simply {@code new Settings()} and exercise the fields
 * without spinning up a Bukkit server.
 */
public final class Settings {

  /** Cap on team size; {@code 0} means "unlimited". */
  public volatile int maxPlayersTeam = 0;

  /**
   * 1-in-N chance per tick that a hostile mob spawns when a block is generated. Values below {@code
   * 2} are corrected to {@code 9} during {@code loadMainConfig()} parsing to avoid divide-by-zero.
   */
  public volatile int mobSpawnChance = 9;

  /** Spawn the first-time-join welcome island layout (blocks + chest). */
  public volatile boolean islandForNewPlayers = false;

  /** Allow {@code /ob rebirth_on_the_island}. */
  public volatile boolean rebirth = false;

  /** Auto-teleport joining players to their island. */
  public volatile boolean autojoin = false;

  /** Drop "loose" items above the generation block instead of placing them. */
  public volatile boolean dropTossUp = true;

  /** Pass {@code physics=true} to placer; lets falling blocks fall etc. */
  public volatile boolean physics = false;

  /** Show the level NAME on the BossBar instead of the templated {@code phText}. */
  public volatile boolean lvlBarMode = false;

  /** Spawn the per-tick portal-particle decoration over the generation block. */
  public volatile boolean particle = true;

  /** Allow nether portals to function on the island world. */
  public volatile boolean allowNether = true;

  /** Push players who wander outside their island region back to it. */
  public volatile boolean protection = false;

  /** Snapshot/restore inventory across {@code /ob leave} / {@code /ob j}. */
  public volatile boolean savePlayerInventory = false;

  /** Show a virtual {@code WorldBorder} around each island. */
  public volatile boolean border = false;

  /** Use a circular id-grid spiral instead of the linear strip layout. */
  public volatile boolean circleMode = true;

  /** Recycle ids whose owner-{@code uuid} is null instead of always appending. */
  public volatile boolean useEmptyIslands = true;

  /** Render a per-player progress BossBar tracking blocks-to-next-level. */
  public volatile boolean progressBar = false;

  /** PlaceholderAPI-template text for the BossBar title. */
  public volatile String phText = "";

  /**
   * Mode for legacy blocks.yml migration. CUMULATIVE flattens the union of levels 0..N, while
   * PER_LEVEL keeps each level self-contained. Default is CUMULATIVE for backward compatibility.
   */
  public volatile MigrationMode migrationBlocksMode = MigrationMode.CUMULATIVE;

  /** Migration mode enum for legacy blocks.yml migration. */
  public enum MigrationMode {
    /** Flatten union of levels 0..N (preserves pre-migration sampling distribution) */
    CUMULATIVE,
    /** Each level self-contained (prevents duplication from previous levels) */
    PER_LEVEL
  }
}
