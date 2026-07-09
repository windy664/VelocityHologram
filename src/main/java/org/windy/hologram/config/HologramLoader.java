package org.windy.hologram.config;

import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮字配置加载器。
 * <p>从 YAML 文件加载悬浮字定义。
 */
public class HologramLoader {

    private final Path dataDir;

    public HologramLoader(Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * 加载所有悬浮字。
     */
    public void loadAll(HologramManager manager) {
        File configDir = dataDir.resolve("holograms").toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
            createDefaultConfig(configDir);
            return;
        }

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                loadFile(file, manager);
            } catch (Exception e) {
                // log error
            }
        }
    }

    /**
     * 加载单个配置文件。
     */
    private void loadFile(File file, HologramManager manager) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        String name = file.getName().replace(".yml", "");

        // 简单的 YAML 解析（不依赖外部库）
        double x = 0, y = 0, z = 0;
        String dimension = "minecraft:overworld";
        String server = "";
        List<String> textLines = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("x:")) x = Double.parseDouble(line.substring(2).trim());
            else if (line.startsWith("y:")) y = Double.parseDouble(line.substring(2).trim());
            else if (line.startsWith("z:")) z = Double.parseDouble(line.substring(2).trim());
            else if (line.startsWith("dimension:")) dimension = line.substring(10).trim();
            else if (line.startsWith("server:")) server = line.substring(7).trim();
            else if (line.startsWith("- ")) textLines.add(line.substring(2).trim());
        }

        Hologram hologram = manager.createHologram(name, x, y, z, dimension, server);
        for (String text : textLines) {
            hologram.addLine(text);
        }
    }

    /**
     * 创建默认配置。
     */
    private void createDefaultConfig(File configDir) {
        try {
            File defaultFile = new File(configDir, "welcome.yml");
            List<String> lines = List.of(
                    "# 悬浮字配置示例",
                    "x: 0.5",
                    "y: 100",
                    "z: 0.5",
                    "dimension: minecraft:overworld",
                    "server: lobby",
                    "lines:",
                    "- \"§b§l欢迎来到本服\"",
                    "- \"§7VelocityHologram v1.0.0\""
            );
            Files.write(defaultFile.toPath(), lines);
        } catch (IOException e) {
            // log error
        }
    }
}
