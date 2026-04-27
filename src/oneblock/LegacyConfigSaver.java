// Copyright © 2025 MrMarL. The MIT License (MIT).
package oneblock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.configuration.file.FileConfiguration;

import com.cryptomorin.xseries.XMaterial;

/**
 * YAML writer that preserves comments and blank lines on legacy Minecraft
 * versions (1.8.x – 1.17.x) whose bundled SnakeYAML strips them on
 * {@code FileConfiguration.save(...)}. On 1.18+ falls through to the
 * built-in {@code save}. Stateless: every call takes the target
 * {@link File} explicitly. The previous static {@code file} field was
 * dropped in Phase 3 to eliminate the hidden-state handoff between
 * {@code ConfigManager} and {@code CommandHandler}.
 */
public final class LegacyConfigSaver {
    private LegacyConfigSaver() {}

    public static void save(final FileConfiguration fc, final File f) {
        // 1.8.x - 1.17.x
        if (!XMaterial.supports(18)) try {
            ArrayList<String> inputStr1 = new ArrayList<String>();
            try (BufferedReader fileIn = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = fileIn.readLine()) != null)
                    inputStr1.add(line);
            }
            ArrayList<String> inputStr2 = new ArrayList<String>();
            inputStr2.addAll(Arrays.asList(fc.saveToString().split("\n")));
            StringBuffer inputBuffer = new StringBuffer();

            int i = 0;
            for (String a:inputStr1) {
                if (i >= inputStr2.size())
                    break;
                if (a.contains("#") || a.isEmpty())
                    inputBuffer.append(a);
                else
                    inputBuffer.append(inputStr2.get(i++));
                inputBuffer.append('\n');
            }

            while (i < inputStr2.size()) {
                inputBuffer.append(inputStr2.get(i++));
                inputBuffer.append('\n');
            }

            try (FileOutputStream fileOut = new FileOutputStream(f)) {
                fileOut.write(inputBuffer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return;
        } 
        catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[Oneblock] Legacy config save failed for " + f + ": " + e.getMessage());
        }
        
        // 1.18+
        try { fc.save(f); } 
        catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[Oneblock] Config save failed for " + f + ": " + e.getMessage());
        }
    }
}