package org.windy.hologram.config;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ActionFactory;
import org.windy.hologram.animation.AnimationParser;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮字配置加载/保存器。
 * <p>支持持久化悬浮字定义（包含动作和动画）。
 *
 * <p>配置格式：
 * <pre>
 * # 悬浮字配置
 * x: 0.5
 * y: 100
 * z: 0.5
 * dimension: minecraft:overworld
 * server: lobby
 * view-distance: 48
 * lines:
 *   - text: "§b§l欢迎来到本服"
 *     left-click: "command:/spawn"
 *     right-click: "url:https://example.com"
 *   - text: "{cycle:20|§a帧1|§b帧2|§c帧3}"
 *   - text: "§7在线: %online%/%max%"
 * </pre>
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
                System.err.println("[VelocityHologram] 加载悬浮字配置失败: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * 加载单个配置文件。
     */
    private void loadFile(File file, HologramManager manager) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        String name = file.getName().replace(".yml", "");

        double x = 0, y = 0, z = 0;
        double viewDistance = 48;
        String dimension = "minecraft:overworld";
        String server = "";
        List<LineConfig> lineConfigs = new ArrayList<>();

        LineConfig currentLine = null;
        boolean inLines = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.equals("lines:")) {
                inLines = true;
                continue;
            }

            if (!inLines) {
                // 顶层配置
                if (line.startsWith("x:")) x = parseDouble(line, 0);
                else if (line.startsWith("y:")) y = parseDouble(line, 0);
                else if (line.startsWith("z:")) z = parseDouble(line, 0);
                else if (line.startsWith("dimension:")) dimension = line.substring(10).trim();
                else if (line.startsWith("server:")) server = line.substring(7).trim();
                else if (line.startsWith("view-distance:")) viewDistance = parseDouble(line, 48);
            } else {
                // 行配置
                if (line.startsWith("- text:")) {
                    if (currentLine != null) {
                        lineConfigs.add(currentLine);
                    }
                    currentLine = new LineConfig();
                    currentLine.text = line.substring(7).trim();
                    // 移除引号
                    if (currentLine.text.startsWith("\"") && currentLine.text.endsWith("\"")) {
                        currentLine.text = currentLine.text.substring(1, currentLine.text.length() - 1);
                    }
                } else if (line.startsWith("left-click:") && currentLine != null) {
                    currentLine.leftClick = line.substring(11).trim();
                } else if (line.startsWith("right-click:") && currentLine != null) {
                    currentLine.rightClick = line.substring(12).trim();
                }
            }
        }

        if (currentLine != null) {
            lineConfigs.add(currentLine);
        }

        // 创建悬浮字
        Hologram hologram = manager.createHologram(name, x, y, z, dimension, server);
        hologram.setViewDistance(viewDistance);

        // 添加行
        for (LineConfig lineConfig : lineConfigs) {
            hologram.addLine(lineConfig.text);

            // 设置点击动作
            if (lineConfig.leftClick != null || lineConfig.rightClick != null) {
                int lastIndex = hologram.getLines().size() - 1;
                Action leftAction = lineConfig.leftClick != null ?
                        ActionFactory.parse(lineConfig.leftClick) : null;
                Action rightAction = lineConfig.rightClick != null ?
                        ActionFactory.parse(lineConfig.rightClick) : null;
                hologram.setLineAction(lastIndex, leftAction, rightAction);
            }
        }
    }

    /**
     * 保存悬浮字到配置文件。
     */
    public void save(Hologram hologram) {
        File configDir = dataDir.resolve("holograms").toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, hologram.getName() + ".yml");
        List<String> lines = new ArrayList<>();

        lines.add("# 悬浮字配置: " + hologram.getName());
        lines.add("x: " + hologram.getPosition().x());
        lines.add("y: " + hologram.getPosition().y());
        lines.add("z: " + hologram.getPosition().z());
        lines.add("dimension: " + hologram.getPosition().dimension());
        lines.add("server: " + hologram.getPosition().server());
        lines.add("view-distance: " + hologram.getViewDistance());
        lines.add("lines:");

        for (var line : hologram.getLines()) {
            lines.add("  - text: \"" + escapeYaml(line.getText()) + "\"");

            // 保存点击动作（需要转换为 HologramLine）
            if (line instanceof org.windy.hologram.hologram.HologramLine holoLine) {
                if (holoLine.getLeftClickAction() != null) {
                    lines.add("    left-click: " + ActionFactory.serialize(holoLine.getLeftClickAction()));
                }
                if (holoLine.getRightClickAction() != null) {
                    lines.add("    right-click: " + ActionFactory.serialize(holoLine.getRightClickAction()));
                }
            }
        }

        try {
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            System.err.println("[VelocityHologram] 保存悬浮字配置失败: " + hologram.getName());
            e.printStackTrace();
        }
    }

    /**
     * 创建默认配置。
     */
    private void createDefaultConfig(File configDir) {
        File defaultFile = new File(configDir, "welcome.yml");
        List<String> lines = List.of(
                "# 悬浮字配置示例",
                "x: 0.5",
                "y: 100",
                "z: 0.5",
                "dimension: minecraft:overworld",
                "server: lobby",
                "view-distance: 48",
                "lines:",
                "  - text: \"§b§l欢迎来到本服\"",
                "    right-click: \"command:/spawn\"",
                "  - text: \"§7在线: %online%/%max%\"",
                "  - text: \"{cycle:20|§a帧1|§b帧2|§c帧3}\""
        );
        try {
            Files.write(defaultFile.toPath(), lines);
        } catch (IOException e) {
            // ignore
        }
    }

    private double parseDouble(String line, double defaultValue) {
        try {
            String value = line.substring(line.indexOf(':') + 1).trim();
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String escapeYaml(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class LineConfig {
        String text;
        String leftClick;
        String rightClick;
    }
}
