package oneblock;

import java.util.ArrayList;
<<<<<<< HEAD
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public class Level {
    public static Level max = new Level("Level: MAX");
    public static ArrayList<Level> levels = new ArrayList<>();
    public static int multiplier = 5;
    
    public static Level get(int i) {
        if (i < levels.size())
            return levels.get(i);
        return max;
    }
    
    public static int size() {
        return levels.size();
    }
    
    public String name;
    public int blocks = 0;  // PoolRegistry blocks bound
    public int mobs = 0;    // PoolRegistry mobs bound
    public BarColor color;
    public BarStyle style;
    public int length = 100;
    
    public Level(String name) {
        this.name = name;
    }
    
    public int getId() {
        for (int i = 0; i < size(); i++) 
            if (get(i) == this)
                return i;
        return 1;
    }
=======
import java.util.Collections;
import java.util.List;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;

public final class Level {
	public static Level max = new Level("Level: MAX");

	/**
	 * Levels list, published as an immutable snapshot via
	 * {@link #replaceAll(List)}. Phase 4.1 made the field {@code volatile}
	 * and {@code private}: writers ({@code ConfigManager.loadBlocks}) build a
	 * fresh {@link ArrayList} fully and only call {@code replaceAll} once
	 * parsing completed without throwing - if a parse step throws (rare,
	 * most YAML failures are tolerated with a warning) the old list stays
	 * visible to readers. Readers ({@code Oneblock.generateBlock} and the
	 * {@code OBP} placeholder helpers via {@link #get(int)} / {@link #size()})
	 * snapshot the field once and iterate the local reference, so they
	 * never observe a half-populated list during a {@code /ob reload}.
	 */
	private static volatile List<Level> levels = Collections.emptyList();

	public static int multiplier = 5;
	
	public static Level get(int i) {
		List<Level> snapshot = levels;
		if (i < snapshot.size())
			return snapshot.get(i);
		return max;
	}
	
	public static int size() {
		return levels.size();
	}

	/**
	 * Snapshot of the current levels list. Always non-null and immutable;
	 * iterating it concurrently with a {@link #replaceAll(List)} call from
	 * another thread is safe - the iterator walks the captured pre-call
	 * reference even after the volatile field has been swapped.
	 */
	public static List<Level> snapshot() { return levels; }

	/**
	 * Atomically publish a new levels list. Defensive-copies the argument
	 * into an {@link Collections#unmodifiableList(List) unmodifiable} wrapper
	 * before the volatile write so callers cannot retroactively mutate the
	 * published state, then performs a single volatile assignment which
	 * establishes a happens-before to every subsequent read of
	 * {@link #levels}. Passing {@code null} or an empty list publishes
	 * {@link Collections#emptyList()}.
	 */
	public static void replaceAll(List<Level> newLevels) {
		levels = (newLevels == null || newLevels.isEmpty())
				? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(newLevels));
	}
	
	public String name;
	public WeightedPool<PoolEntry> blockPool = new WeightedPool<>();
	public WeightedPool<EntityType> mobPool = new WeightedPool<>();
	public BarColor color;
	public BarStyle style;
	public int length = 100;
	
	public Level(String name) {
        this.name = name;
    }
	
	public int getId() {
		List<Level> snapshot = levels;
		for (int i = 0; i < snapshot.size(); i++) 
			if (snapshot.get(i) == this)
				return i;
		return 1;
	}
	
	public int blockPoolSize() { return blockPool.size(); }
	public int mobPoolSize()   { return mobPool.size(); }
	
	public void resetPools() {
		blockPool = new WeightedPool<>();
		mobPool = new WeightedPool<>();
	}
>>>>>>> origin/main
}