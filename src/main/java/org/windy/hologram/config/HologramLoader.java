package org.windy.hologram.config;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ActionFactory;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramLine;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.hologram.Page;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 悬浮字配置加载/保存器。
 * <p>支持多页配置，向后兼容旧的单页 lines: 格式。
 *
 * <p>配置格式 v1.2（多页）：
 * <pre>
 * x: 0.5
 * y: 100
 * z: 0.5
 * dimension: minecraft:overworld
 * server: lobby
 * view-distance: 48
 * line-spacing: 0.3
 * pages:
 *   - lines:
 *     - text: "§b第一页"
 *     - text: "§7内容A"
 *   - lines:
 *     - text: "§a第二页"
 *     - text: "§7内容B"
 * </pre>
 *
 * <p>旧格式（自动转为单页）：
 * <pre>
 * lines:
 *   - text: "§b标题"
 * </pre>
 */
public class HologramLoader {

    private final Path dataDir;
    private final DisplayFactoryRegistry displayRegistry;

    public HologramLoader(Path dataDir, DisplayFactoryRegistry displayRegistry) {
        this.dataDir = dataDir;
        this.displayRegistry = displayRegistry;
    }

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

    private void loadFile(File file, HologramManager manager) throws IOException {
        List<String> rawLines = Files.readAllLines(file.toPath());
        String name = file.getName().replace(".yml", "");

        double x = 0, y = 0, z = 0;
        double viewDistance = 48;
        double updateDistance = 48;
        double lineSpacing = 0.3;
        int updateInterval = 20;
        String dimension = "minecraft:overworld";
        String server = "";
        String permission = null;
        java.util.Set<String> flags = new java.util.HashSet<>();

        // 解析结果：多页
        List<List<LineConfig>> pages = new ArrayList<>();
        List<LineConfig> currentPage = null;
        LineConfig currentLine = null;
        boolean inPages = false;
        boolean inLines = false;

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // 检测 pages: 入口
            if (line.equals("pages:")) {
                inPages = true;
                inLines = false;
                continue;
            }

            // 检测旧格式 lines: 入口（非 pages 内）
            if (!inPages && line.equals("lines:")) {
                inLines = true;
                currentPage = new ArrayList<>();
                continue;
            }

            if (inPages) {
                // 多页模式
                if (line.startsWith("- lines:")) {
                    // 新页开始
                    if (currentPage != null) pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentLine = null;
                    continue;
                }
                if (line.startsWith("- lines")) {
                    // 兼容 "- lines" 无冒号
                    if (currentPage != null) pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentLine = null;
                    continue;
                }
                if (currentPage == null) {
                    // 可能是缩进的 "- lines:" 形式
                    if (line.equals("- lines:") || line.equals("- lines")) {
                        currentPage = new ArrayList<>();
                        currentLine = null;
                        continue;
                    }
                    // 不在任何页内，跳过
                }
                // 解析行
                if (currentPage != null) {
                    currentLine = parseLine(line, currentLine, currentPage);
                }
            } else if (inLines) {
                // 旧格式单页模式
                currentLine = parseLine(line, currentLine, currentPage);
            } else {
                // 顶层配置
                if (line.startsWith("x:")) x = parseDouble(line, 0);
                else if (line.startsWith("y:")) y = parseDouble(line, 0);
                else if (line.startsWith("z:")) z = parseDouble(line, 0);
                else if (line.startsWith("dimension:")) dimension = line.substring(10).trim();
                else if (line.startsWith("server:")) server = line.substring(7).trim();
                else if (line.startsWith("view-distance:")) viewDistance = parseDouble(line, 48);
                else if (line.startsWith("update-distance:")) updateDistance = parseDouble(line, 48);
                else if (line.startsWith("line-spacing:")) lineSpacing = parseDouble(line, 0.3);
                else if (line.startsWith("update-interval:")) updateInterval = (int) parseDouble(line, 20);
                else if (line.startsWith("permission:")) permission = line.substring(11).trim().replace("\"", "");
                else if (line.startsWith("flags:")) {} // 下面解析
                else if (line.startsWith("- ") && line.length() > 2 && flags != null) {
                    // flags 列表项
                    flags.add(line.substring(2).trim().toLowerCase());
                }
            }
        }

        // 收尾
        if (currentPage != null) pages.add(currentPage);

        // 如果没有解析到任何页，创建一个空页
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>());
        }

        // 创建悬浮字
        Hologram hologram = manager.createHologram(name, x, y, z, dimension, server);
        hologram.setViewDistance(viewDistance);
        hologram.setUpdateDistance(updateDistance);
        hologram.setLineSpacing(lineSpacing);
        hologram.setUpdateInterval(updateInterval);
        if (permission != null && !permission.isEmpty()) {
            hologram.setPermission(permission);
        }
        for (String flag : flags) {
            hologram.addFlag(flag);
        }

        // 填充页和行
        for (int pi = 0; pi < pages.size(); pi++) {
            Page page = (pi == 0) ? hologram.getPage(0) : hologram.addPage();
            if (page == null) continue;

            for (LineConfig lc : pages.get(pi)) {
                DisplayConfig config = buildDisplayConfig(lc);
                HologramLine holoLine = page.addLine(config);

                // 行偏移
                if (lc.offsetY > 0) holoLine.setOffsetY(lc.offsetY);
                if (lc.offsetX != 0) holoLine.setOffsetX(lc.offsetX);
                if (lc.offsetZ != 0) holoLine.setOffsetZ(lc.offsetZ);

                // 点击动作
                if (lc.leftClick != null || lc.rightClick != null
                        || lc.shiftLeftClick != null || lc.shiftRightClick != null) {
                    Action left = lc.leftClick != null ? ActionFactory.parse(lc.leftClick) : null;
                    Action right = lc.rightClick != null ? ActionFactory.parse(lc.rightClick) : null;
                    Action shiftLeft = lc.shiftLeftClick != null ? ActionFactory.parse(lc.shiftLeftClick) : null;
                    Action shiftRight = lc.shiftRightClick != null ? ActionFactory.parse(lc.shiftRightClick) : null;
                    page.setLineAction(page.getLineCount() - 1, left, right, shiftLeft, shiftRight, null);
                }
            }
        }
    }

    /**
     * 解析一行配置，返回 currentLine（可能是新的或更新后的）。
     */
    private LineConfig parseLine(String line, LineConfig currentLine, List<LineConfig> target) {
        if (line.startsWith("- text:") || line.startsWith("- item:") || line.startsWith("- block:")
                || line.startsWith("- entity:") || line.startsWith("- head:") || line.startsWith("- smallhead:")) {
            if (currentLine != null) target.add(currentLine);
            currentLine = new LineConfig();
            if (line.startsWith("- text:")) {
                currentLine.type = DisplayEntityType.TEXT_DISPLAY;
                currentLine.text = extractQuotedValue(line, 7);
            } else if (line.startsWith("- item:")) {
                currentLine.type = DisplayEntityType.ITEM_DISPLAY;
                currentLine.itemId = extractQuotedValue(line, 7);
            } else if (line.startsWith("- block:")) {
                currentLine.type = DisplayEntityType.BLOCK_DISPLAY;
                currentLine.blockId = extractQuotedValue(line, 8);
            } else if (line.startsWith("- entity:")) {
                currentLine.type = DisplayEntityType.ENTITY;
                currentLine.entityId = extractQuotedValue(line, 9);
            } else if (line.startsWith("- head:")) {
                currentLine.type = DisplayEntityType.HEAD;
                currentLine.blockId = extractQuotedValue(line, 7);
            } else if (line.startsWith("- smallhead:")) {
                currentLine.type = DisplayEntityType.SMALLHEAD;
                currentLine.blockId = extractQuotedValue(line, 12);
            }
        } else if (currentLine != null) {
            if (line.startsWith("left-click:")) currentLine.leftClick = line.substring(11).trim();
            else if (line.startsWith("right-click:")) currentLine.rightClick = line.substring(12).trim();
            else if (line.startsWith("shift-left-click:")) currentLine.shiftLeftClick = line.substring(17).trim();
            else if (line.startsWith("shift-right-click:")) currentLine.shiftRightClick = line.substring(18).trim();
            else if (line.startsWith("billboard:")) currentLine.billboard = line.substring(10).trim();
            else if (line.startsWith("scale:")) currentLine.scale = (float) parseDouble(line, 1.0);
            else if (line.startsWith("scale-x:")) currentLine.scaleX = (float) parseDouble(line, 1.0);
            else if (line.startsWith("scale-y:")) currentLine.scaleY = (float) parseDouble(line, 1.0);
            else if (line.startsWith("scale-z:")) currentLine.scaleZ = (float) parseDouble(line, 1.0);
            else if (line.startsWith("opacity:")) currentLine.opacity = (byte) Math.max(0, Math.min(255, (int) parseDouble(line, 255)));
            else if (line.startsWith("background:")) currentLine.backgroundColor = parseArgb(line);
            else if (line.startsWith("offset-y:")) currentLine.offsetY = parseDouble(line, 0);
            else if (line.startsWith("offset-x:")) currentLine.offsetX = parseDouble(line, 0);
            else if (line.startsWith("offset-z:")) currentLine.offsetZ = parseDouble(line, 0);
            else if (line.startsWith("line-width:")) currentLine.lineWidth = (int) parseDouble(line, 200);
        }
        return currentLine;
    }

    private DisplayConfig buildDisplayConfig(LineConfig lc) {
        DisplayConfig.Builder builder = DisplayConfig.builder(lc.type);
        if (lc.type == DisplayEntityType.TEXT_DISPLAY) builder.text(lc.text != null ? lc.text : "");
        else if (lc.type == DisplayEntityType.ITEM_DISPLAY) builder.itemId(lc.itemId != null ? lc.itemId : "minecraft:stone");
        else if (lc.type == DisplayEntityType.BLOCK_DISPLAY) builder.blockId(lc.blockId != null ? lc.blockId : "minecraft:stone");
        else if (lc.type == DisplayEntityType.ENTITY) builder.blockId(lc.entityId != null ? lc.entityId : "minecraft:pig");
        else if (lc.type == DisplayEntityType.HEAD || lc.type == DisplayEntityType.SMALLHEAD)
            builder.blockId(lc.blockId != null ? lc.blockId : "minecraft:stone");

        if (lc.billboard != null) builder.billboard(DisplayConfig.Billboard.fromConfig(lc.billboard));
        if (lc.scale > 0) builder.scale(lc.scale, lc.scale, lc.scale);
        else if (lc.scaleX > 0 || lc.scaleY > 0 || lc.scaleZ > 0) {
            builder.scale(lc.scaleX > 0 ? lc.scaleX : 1f, lc.scaleY > 0 ? lc.scaleY : 1f, lc.scaleZ > 0 ? lc.scaleZ : 1f);
        }
        if (lc.backgroundColor != 0) builder.backgroundColor(lc.backgroundColor);
        if (lc.opacity > 0) builder.textOpacity(lc.opacity);
        if (lc.lineWidth > 0) builder.lineWidth(lc.lineWidth);
        return builder.build();
    }

    // ===== 保存 =====

    public void delete(String name) {
        File file = dataDir.resolve("holograms").toFile();
        File configFile = new File(file, name + ".yml");
        if (configFile.exists()) configFile.delete();
    }

    public void save(Hologram hologram) {
        File configDir = dataDir.resolve("holograms").toFile();
        if (!configDir.exists()) configDir.mkdirs();

        File file = new File(configDir, hologram.getName() + ".yml");
        List<String> out = new ArrayList<>();

        out.add("# 悬浮字配置: " + hologram.getName());
        out.add("x: " + hologram.getPosition().x());
        out.add("y: " + hologram.getPosition().y());
        out.add("z: " + hologram.getPosition().z());
        out.add("dimension: " + hologram.getPosition().dimension());
        out.add("server: " + hologram.getPosition().server());
        out.add("view-distance: " + hologram.getViewDistance());
        out.add("update-distance: " + hologram.getUpdateDistance());
        out.add("line-spacing: " + hologram.getLineSpacing());
        out.add("update-interval: " + hologram.getUpdateInterval());
        if (hologram.getPermission() != null) {
            out.add("permission: \"" + hologram.getPermission() + "\"");
        }
        if (!hologram.getFlags().isEmpty()) {
            out.add("flags:");
            for (String flag : hologram.getFlags()) {
                out.add("  - " + flag);
            }
        }

        boolean multiPage = hologram.getPageCount() > 1;

        if (multiPage) {
            out.add("pages:");
        }

        for (int pi = 0; pi < hologram.getPageCount(); pi++) {
            Page page = hologram.getPage(pi);
            if (page == null) continue;

            if (multiPage) {
                out.add("  - lines:");
            } else {
                out.add("lines:");
            }

            String indent = multiPage ? "    " : "  ";

            for (var line : page.getLines()) {
                if (!(line instanceof HologramLine)) continue;
                HologramLine hl = (HologramLine) line;
                DisplayConfig config = hl.getDisplayConfig();

                switch (config.type()) {
                    case TEXT_DISPLAY: out.add(indent + "- text: \"" + escapeYaml(config.text()) + "\""); break;
                    case ITEM_DISPLAY: out.add(indent + "- item: \"" + escapeYaml(config.itemId()) + "\""); break;
                    case BLOCK_DISPLAY: out.add(indent + "- block: \"" + escapeYaml(config.blockId()) + "\""); break;
                    case ENTITY: out.add(indent + "- entity: \"" + escapeYaml(config.blockId()) + "\""); break;
                    case HEAD: out.add(indent + "- head: \"" + escapeYaml(config.blockId()) + "\""); break;
                    case SMALLHEAD: out.add(indent + "- smallhead: \"" + escapeYaml(config.blockId()) + "\""); break;
                }

                if (config.billboard() != DisplayConfig.Billboard.CENTER)
                    out.add(indent + "  billboard: " + config.billboard().name().toLowerCase());
                if (config.scaleX() != 1f || config.scaleY() != 1f || config.scaleZ() != 1f) {
                    if (config.scaleX() == config.scaleY() && config.scaleY() == config.scaleZ())
                        out.add(indent + "  scale: " + config.scaleX());
                    else {
                        out.add(indent + "  scale-x: " + config.scaleX());
                        out.add(indent + "  scale-y: " + config.scaleY());
                        out.add(indent + "  scale-z: " + config.scaleZ());
                    }
                }
                if (config.textOpacity() != (byte) 0xFF)
                    out.add(indent + "  opacity: " + (config.textOpacity() & 0xFF));
                if (config.backgroundColor() != 0x40000000)
                    out.add(indent + "  background: 0x" + Integer.toHexString(config.backgroundColor()));
                if (config.lineWidth() != 200)
                    out.add(indent + "  line-width: " + config.lineWidth());
                if (hl.getOffsetY() > 0) out.add(indent + "  offset-y: " + hl.getOffsetY());
                if (hl.getOffsetX() != 0) out.add(indent + "  offset-x: " + hl.getOffsetX());
                if (hl.getOffsetZ() != 0) out.add(indent + "  offset-z: " + hl.getOffsetZ());

                if (hl.getLeftClickAction() != null)
                    out.add(indent + "  left-click: " + ActionFactory.serialize(hl.getLeftClickAction()));
                if (hl.getRightClickAction() != null)
                    out.add(indent + "  right-click: " + ActionFactory.serialize(hl.getRightClickAction()));
                if (hl.getShiftLeftClickAction() != null)
                    out.add(indent + "  shift-left-click: " + ActionFactory.serialize(hl.getShiftLeftClickAction()));
                if (hl.getShiftRightClickAction() != null)
                    out.add(indent + "  shift-right-click: " + ActionFactory.serialize(hl.getShiftRightClickAction()));
            }
        }

        try {
            Files.write(file.toPath(), out);
        } catch (IOException e) {
            System.err.println("[VelocityHologram] 保存悬浮字配置失败: " + hologram.getName());
            e.printStackTrace();
        }
    }

    private void createDefaultConfig(File configDir) {
        File f = new File(configDir, "welcome.yml");
        List<String> lines = List.of(
                "# 悬浮字配置示例（多页）",
                "x: 0.5",
                "y: 100",
                "z: 0.5",
                "dimension: minecraft:overworld",
                "server: lobby",
                "view-distance: 48",
                "line-spacing: 0.3",
                "pages:",
                "  - lines:",
                "    - text: \"§b§l欢迎来到本服\"",
                "      billboard: center",
                "      right-click: \"command:/spawn\"",
                "    - text: \"§7在线: %server_online%/%server_max_players%\"",
                "    - text: \"§e左键翻页 →\"",
                "      left-click: \"nextpage\"",
                "  - lines:",
                "    - text: \"§a§l第二页\"",
                "      billboard: center",
                "    - text: \"§7这是一悬浮字的第二页\"",
                "    - text: \"§c← 右键翻回\"",
                "      right-click: \"prevpage\""
        );
        try { Files.write(f.toPath(), lines); } catch (IOException ignored) {}
    }

    // ===== 工具 =====

    private double parseDouble(String line, double def) {
        try { return Double.parseDouble(line.substring(line.indexOf(':') + 1).trim()); }
        catch (Exception e) { return def; }
    }

    private String extractQuotedValue(String line, int prefixLen) {
        String v = line.substring(prefixLen).trim();
        if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        return v;
    }

    private int parseArgb(String line) {
        try {
            String v = line.substring(line.indexOf(':') + 1).trim();
            if (v.startsWith("0x") || v.startsWith("0X")) return (int) Long.parseLong(v.substring(2), 16);
            return Integer.parseInt(v);
        } catch (Exception e) { return 0; }
    }

    private String escapeYaml(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class LineConfig {
        DisplayEntityType type = DisplayEntityType.TEXT_DISPLAY;
        String text, itemId, blockId, entityId;
        String leftClick, rightClick, shiftLeftClick, shiftRightClick;
        String billboard;
        float scale, scaleX, scaleY, scaleZ;
        byte opacity;
        int backgroundColor, lineWidth;
        double offsetY, offsetX, offsetZ;
    }
}
