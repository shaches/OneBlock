package oneblock;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * Registry mapping chest-alias names (as referenced from {@code blocks.yml} pool entries) to either
 * vanilla {@link NamespacedKey} loot-table identifiers or legacy {@link ItemStack} lists.
 *
 * <p>Dual-mode support: a chest alias may be a single string value ({@code name:
 * minecraft:loot_table_key}) or a YAML list of item maps ({@code name: [item1, item2, ...]}). The
 * former triggers the Bukkit {@link LootTable} API at runtime; the latter fills a placed chest
 * block directly from the stored item list.
 *
 * <p>Legacy item-list chests can be edited in-game via the {@code /ob chest <name> edit} GUI. Both
 * modes are persisted round-trip to {@code chests.yml}.
 */
public final class ChestItems {
  public static File chest;
  private static final Map<String, NamespacedKey> aliases = new LinkedHashMap<>();
  private static final Map<String, List<ItemStack>> aliasesLegacy = new LinkedHashMap<>();

  public static void save() {
    YamlConfiguration config = new YamlConfiguration();
    for (Map.Entry<String, NamespacedKey> e : aliases.entrySet())
      config.set(e.getKey(), e.getValue().toString());
    for (Map.Entry<String, List<ItemStack>> e : aliasesLegacy.entrySet())
      config.set(e.getKey(), e.getValue());
    try {
      config.save(chest);
    } catch (Exception e) {
      if (Bukkit.getServer() != null)
        Bukkit.getLogger().warning("[Oneblock] Failed to save chests.yml: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public static void load() {
    aliases.clear();
    aliasesLegacy.clear();
    if (chest == null || !chest.exists()) return;
    YamlConfiguration config = YamlConfiguration.loadConfiguration(chest);
    for (String name : config.getKeys(false)) {
      if (config.isList(name)) {
        List<?> raw = config.getList(name);
        if (raw == null) continue;
        List<ItemStack> items = new ArrayList<>();
        for (Object o : raw) {
          if (o instanceof ItemStack) items.add((ItemStack) o);
          else if (o instanceof Map) {
            try {
              items.add(ItemStack.deserialize((Map<String, Object>) o));
            } catch (Exception ex) {
              /* skip malformed item */
            }
          }
        }
        aliasesLegacy.put(name, items);
      } else {
        String keyString = config.getString(name);
        if (keyString == null) continue;
        NamespacedKey key = parseKey(keyString);
        if (key != null) aliases.put(name, key);
        else if (Bukkit.getServer() != null)
          Bukkit.getLogger()
              .warning(
                  "[Oneblock] Unknown loot-table key '"
                      + keyString
                      + "' for chest alias '"
                      + name
                      + "'");
      }
    }
  }

  public static Set<String> getChestNames() {
    Set<String> merged = new java.util.LinkedHashSet<>();
    merged.addAll(aliases.keySet());
    merged.addAll(aliasesLegacy.keySet());
    return Collections.unmodifiableSet(merged);
  }

  public static NamespacedKey resolve(String aliasName) {
    if (aliasName == null) return null;
    return aliases.get(aliasName);
  }

  public static List<ItemStack> getItems(String aliasName) {
    if (aliasName == null) return null;
    List<ItemStack> list = aliasesLegacy.get(aliasName);
    return list == null ? null : Collections.unmodifiableList(list);
  }

  public static void setItems(String name, List<ItemStack> items) {
    if (name == null || items == null) return;
    aliasesLegacy.put(name, new ArrayList<>(items));
  }

  public static boolean hasChest(String aliasName) {
    return aliasName != null
        && (aliases.containsKey(aliasName) || aliasesLegacy.containsKey(aliasName));
  }

  public static void setAlias(String name, NamespacedKey key) {
    if (name == null || key == null) return;
    aliases.put(name, key);
  }

  public static boolean removeAlias(String name) {
    return aliases.remove(name) != null || aliasesLegacy.remove(name) != null;
  }

  /**
   * Tolerant NamespacedKey parser. Accepts both {@code minecraft:path} and {@code namespace:path}
   * forms. Falls back to {@link NamespacedKey#minecraft(String)} when the namespace is missing.
   * Returns {@code null} on any validation failure.
   */
  public static NamespacedKey parseKey(String s) {
    if (s == null || s.isEmpty()) return null;
    String lower = s.toLowerCase();
    String[] parts = lower.split(":", 2);
    String ns = parts.length == 2 ? parts[0] : "minecraft";
    String path = parts.length == 2 ? parts[1] : parts[0];
    if ("minecraft".equals(ns)) {
      try {
        return NamespacedKey.minecraft(path);
      } catch (Throwable t) {
        return null;
      }
    }
    try {
      return NamespacedKey.fromString(lower);
    } catch (Throwable t) {
      return null;
    }
  }
}
