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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 悬浮字配置加载/保存器。
 * <p>对标 DecentHolograms 配置格式。
 *
 * <p>配置格式（对标 DH）：
 * <pre>
 * location: world:0.500:100.0:0.500
 * enabled: true
 * display-range: 64
 * update-range: 64
 * update-interval: 20
 * facing: 0.0
 * down-origin: false
 * pages:
 *   - lines:
 *       - content: "§b标题"
 *         height: 0.3
 *         offsetX: 0.0
 *         offsetZ: 0.0
 *       - content: "§7内容"
 *     actions:
 *       RIGHT:
 *         - "command:/spawn"
 *       LEFT:
 *         - "nextpage"
 * </pre>
 */
public class HologramLoader {

    private final Path dataDir;
    private final DisplayFactoryRegistry displayRegistry;

    // 位置解析正则：world:x:y:z
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^([^:]+):(-?[\\d.]+):(-?[\\d.]+):(-?[\\d.]+)$");

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

        // 顶层配置
        String locationStr = null;
        boolean enabled = true;
        int displayRange = 48;
        int updateRange = 48;
        int updateInterval = 20;
        float facing = 0;
        boolean downOrigin = false;
        String permission = null;
        List<String> flags = new ArrayList<>();

        // 页面解析
        List<PageData> pagesData = new ArrayList<>();
        PageData currentPage = null;
        LineData currentLine = null;
        String currentActionType = null;

        // 解析状态
        enum State { TOP, PAGES, LINES, LINE_DETAIL, ACTIONS, ACTION_LIST }
        State state = State.TOP;

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int indent = getIndent(raw);

            switch (state) {
                case TOP:
                    if (line.startsWith("location:")) {
                        locationStr = line.substring(9).trim();
                    } else if (line.startsWith("enabled:")) {
                        enabled = Boolean.parseBoolean(line.substring(8).trim());
                    } else if (line.startsWith("display-range:")) {
                        displayRange = parseInt(line.substring(14).trim(), 48);
                    } else if (line.startsWith("update-range:")) {
                        updateRange = parseInt(line.substring(13).trim(), 48);
                    } else if (line.startsWith("update-interval:")) {
                        updateInterval = parseInt(line.substring(16).trim(), 20);
                    } else if (line.startsWith("facing:")) {
                        facing = parseFloat(line.substring(7).trim(), 0);
                    } else if (line.startsWith("down-origin:")) {
                        downOrigin = Boolean.parseBoolean(line.substring(12).trim());
                    } else if (line.startsWith("permission:")) {
                        permission = line.substring(11).trim().replace("\"", "");
                    } else if (line.equals("flags:")) {
                        // 下一行开始解析 flags
                    } else if (line.startsWith("- ") && permission != null && flags != null) {
                        flags.add(line.substring(2).trim().toLowerCase());
                    } else if (line.equals("pages:")) {
                        state = State.PAGES;
                    }
                    break;

                case PAGES:
                    if (line.equals("- lines:") || line.equals("- lines")) {
                        currentPage = new PageData();
                        currentLine = null;
                        state = State.LINES;
                    }
                    break;

                case LINES:
                    if (line.equals("actions:") || line.equals("actions")) {
                        state = State.ACTIONS;
                        break;
                    }
                    if (line.startsWith("- content:") || line.startsWith("- content ")) {
                        // 保存上一行
                        if (currentLine != null) {
                            currentPage.lines.add(currentLine);
                        }
                        currentLine = new LineData();
                        currentLine.content = extractQuotedValue(line, 10);
                        state = State.LINE_DETAIL;
                    } else if (line.startsWith("- text:") || line.startsWith("- text ")) {
                        // 兼容旧格式
                        if (currentLine != null) {
                            currentPage.lines.add(currentLine);
                        }
                        currentLine = new LineData();
                        currentLine.content = extractQuotedValue(line, 7);
                        state = State.LINE_DETAIL;
                    } else if (line.equals("- lines:") || line.equals("- lines")) {
                        // 新页
                        if (currentPage != null) {
                            if (currentLine != null) {
                                currentPage.lines.add(currentLine);
                                currentLine = null;
                            }
                            pagesData.add(currentPage);
                        }
                        currentPage = new PageData();
                    }
                    break;

                case LINE_DETAIL:
                    if (line.equals("actions:") || line.equals("actions")) {
                        state = State.ACTIONS;
                        break;
                    }
                    if (line.startsWith("- content:") || line.startsWith("- content ")) {
                        // 新行
                        if (currentLine != null) {
                            currentPage.lines.add(currentLine);
                        }
                        currentLine = new LineData();
                        currentLine.content = extractQuotedValue(line, 10);
                    } else if (line.startsWith("- text:") || line.startsWith("- text ")) {
                        // 兼容旧格式
                        if (currentLine != null) {
                            currentPage.lines.add(currentLine);
                        }
                        currentLine = new LineData();
                        currentLine.content = extractQuotedValue(line, 7);
                    } else if (line.equals("- lines:") || line.equals("- lines")) {
                        // 新页
                        if (currentPage != null) {
                            if (currentLine != null) {
                                currentPage.lines.add(currentLine);
                                currentLine = null;
                            }
                            pagesData.add(currentPage);
                        }
                        currentPage = new PageData();
                        state = State.LINES;
                    } else if (currentLine != null) {
                        if (line.startsWith("height:")) {
                            currentLine.height = parseFloat(line.substring(7).trim(), 0.3f);
                        } else if (line.startsWith("offsetX:") || line.startsWith("offset-x:")) {
                            String val = line.contains("offsetX:") ? line.substring(8) : line.substring(9);
                            currentLine.offsetX = parseFloat(val.trim(), 0);
                        } else if (line.startsWith("offsetZ:") || line.startsWith("offset-z:")) {
                            String val = line.contains("offsetZ:") ? line.substring(8) : line.substring(9);
                            currentLine.offsetZ = parseFloat(val.trim(), 0);
                        } else if (line.startsWith("permission:")) {
                            currentLine.permission = line.substring(11).trim().replace("\"", "");
                        }
                    }
                    break;

                case ACTIONS:
                    if (line.equals("RIGHT:") || line.equals("LEFT:") ||
                            line.equals("SHIFT_RIGHT:") || line.equals("SHIFT_LEFT:")) {
                        currentActionType = line.replace(":", "").trim();
                        state = State.ACTION_LIST;
                    } else if (line.equals("- lines:") || line.equals("- lines")) {
                        // 新页
                        if (currentPage != null) {
                            if (currentLine != null) {
                                currentPage.lines.add(currentLine);
                                currentLine = null;
                            }
                            pagesData.add(currentPage);
                        }
                        currentPage = new PageData();
                        state = State.LINES;
                    }
                    break;

                case ACTION_LIST:
                    if (line.startsWith("- ")) {
                        String actionStr = line.substring(2).trim();
                        if (currentActionType != null) {
                            currentPage.actions.computeIfAbsent(currentActionType, k -> new ArrayList<>())
                                    .add(actionStr);
                        }
                    } else if (line.equals("RIGHT:") || line.equals("LEFT:") ||
                            line.equals("SHIFT_RIGHT:") || line.equals("SHIFT_LEFT:")) {
                        currentActionType = line.replace(":", "").trim();
                    } else if (line.equals("- lines:") || line.equals("- lines")) {
                        // 新页
                        if (currentPage != null) {
                            if (currentLine != null) {
                                currentPage.lines.add(currentLine);
                                currentLine = null;
                            }
                            pagesData.add(currentPage);
                        }
                        currentPage = new PageData();
                        state = State.LINES;
                    } else {
                        state = State.ACTIONS;
                    }
                    break;
            }
        }

        // 收尾
        if (currentLine != null && currentPage != null) {
            currentPage.lines.add(currentLine);
        }
        if (currentPage != null) {
            pagesData.add(currentPage);
        }
        if (pagesData.isEmpty()) {
            pagesData.add(new PageData());
        }

        // 解析位置
        double x = 0, y = 0, z = 0;
        String dimension = "minecraft:overworld";
        String server = "";

        if (locationStr != null) {
            Matcher matcher = LOCATION_PATTERN.matcher(locationStr);
            if (matcher.matches()) {
                dimension = normalizeDimension(matcher.group(1));
                x = Double.parseDouble(matcher.group(2));
                y = Double.parseDouble(matcher.group(3));
                z = Double.parseDouble(matcher.group(4));
            }
        }

        // 创建悬浮字
        Hologram hologram = manager.createHologram(name, x, y, z, dimension, server);
        hologram.setEnabled(enabled);
        hologram.setViewDistance(displayRange);
        hologram.setUpdateDistance(updateRange);
        hologram.setUpdateInterval(updateInterval);
        hologram.setFacing(facing, 0);
        hologram.setDownOrigin(downOrigin);

        if (permission != null && !permission.isEmpty()) {
            hologram.setPermission(permission);
        }
        for (String flag : flags) {
            hologram.addFlag(flag);
        }

        // 填充页和行
        for (int pi = 0; pi < pagesData.size(); pi++) {
            PageData pageData = pagesData.get(pi);
            Page page = (pi == 0) ? hologram.getPage(0) : hologram.addPage();
            if (page == null) continue;

            // 加载行
            for (LineData lineData : pageData.lines) {
                DisplayConfig config = parseDisplayConfig(lineData);
                HologramLine holoLine = page.addLine(config);

                if (lineData.height > 0) holoLine.setLineHeight(lineData.height);
                if (lineData.offsetX != 0) holoLine.setOffsetX(lineData.offsetX);
                if (lineData.offsetZ != 0) holoLine.setOffsetZ(lineData.offsetZ);
                if (lineData.permission != null) holoLine.setPermission(lineData.permission);
            }

            // 加载页面动作
            for (var entry : pageData.actions.entrySet()) {
                String clickType = entry.getKey();
                for (String actionStr : entry.getValue()) {
                    Action action = ActionFactory.parse(actionStr);
                    if (action != null) {
                        page.addAction(clickType, action);
                    }
                }
            }
        }
    }

    /**
     * 解析行配置。
     */
    private DisplayConfig parseDisplayConfig(LineData lineData) {
        String content = lineData.content;

        // 检测行类型
        if (content.toUpperCase().startsWith("#ICON:")) {
            return DisplayConfig.builder(DisplayEntityType.ICON)
                    .blockId(content.substring(6))
                    .build();
        } else if (content.toUpperCase().startsWith("#HEAD:")) {
            return DisplayConfig.builder(DisplayEntityType.HEAD)
                    .blockId(content.substring(6))
                    .build();
        } else if (content.toUpperCase().startsWith("#SMALLHEAD:")) {
            return DisplayConfig.builder(DisplayEntityType.SMALLHEAD)
                    .blockId(content.substring(11))
                    .build();
        } else if (content.toUpperCase().startsWith("#ENTITY:")) {
            return DisplayConfig.builder(DisplayEntityType.ENTITY)
                    .blockId(content.substring(8))
                    .build();
        } else {
            // 默认文本
            return DisplayConfig.builder(DisplayEntityType.TEXT_DISPLAY)
                    .text(content)
                    .build();
        }
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

        // 顶层配置（对标 DH 格式）
        out.add("# 悬浮字配置: " + hologram.getName());
        out.add("location: " + hologram.getPosition().dimension()
                + ":" + hologram.getPosition().x()
                + ":" + hologram.getPosition().y()
                + ":" + hologram.getPosition().z());
        out.add("enabled: " + hologram.isEnabled());
        out.add("display-range: " + (int) hologram.getViewDistance());
        out.add("update-range: " + (int) hologram.getUpdateDistance());
        out.add("update-interval: " + hologram.getUpdateInterval());
        out.add("facing: " + hologram.getFacingYaw());
        out.add("down-origin: " + hologram.isDownOrigin());

        if (hologram.getPermission() != null) {
            out.add("permission: \"" + hologram.getPermission() + "\"");
        }
        if (!hologram.getFlags().isEmpty()) {
            out.add("flags:");
            for (String flag : hologram.getFlags()) {
                out.add("  - " + flag);
            }
        }

        // 页面
        out.add("pages:");
        for (int pi = 0; pi < hologram.getPageCount(); pi++) {
            Page page = hologram.getPage(pi);
            if (page == null) continue;

            out.add("  - lines:");
            for (var line : page.getLines()) {
                if (!(line instanceof HologramLine)) continue;
                HologramLine hl = (HologramLine) line;
                DisplayConfig config = hl.getDisplayConfig();

                // 行内容
                String content;
                switch (config.type()) {
                    case ICON: content = "#ICON:" + config.blockId(); break;
                    case HEAD: content = "#HEAD:" + config.blockId(); break;
                    case SMALLHEAD: content = "#SMALLHEAD:" + config.blockId(); break;
                    case ENTITY: content = "#ENTITY:" + config.blockId(); break;
                    default: content = config.text() != null ? config.text() : ""; break;
                }
                out.add("        - content: \"" + escapeYaml(content) + "\"");

                // 行属性
                if (hl.getLineHeight() > 0) out.add("          height: " + hl.getLineHeight());
                if (hl.getOffsetX() != 0) out.add("          offsetX: " + hl.getOffsetX());
                if (hl.getOffsetZ() != 0) out.add("          offsetZ: " + hl.getOffsetZ());
                if (hl.getPermission() != null) out.add("          permission: \"" + hl.getPermission() + "\"");
            }

            // 页面动作
            if (!page.getActions().isEmpty()) {
                out.add("    actions:");
                for (var entry : page.getActions().entrySet()) {
                    out.add("      " + entry.getKey() + ":");
                    out.add("        - \"" + ActionFactory.serialize(entry.getValue()) + "\"");
                }
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
                "# 悬浮字配置示例（对标 DecentHolograms 格式）",
                "location: minecraft:overworld:0.500:100.0:0.500",
                "enabled: true",
                "display-range: 48",
                "update-range: 48",
                "update-interval: 20",
                "facing: 0.0",
                "down-origin: false",
                "pages:",
                "  - lines:",
                "      - content: \"§b§l欢迎来到本服\"",
                "        height: 0.3",
                "      - content: \"§7在线: %server_online%/%server_max_players%\"",
                "      - content: \"§e左键翻页 →\"",
                "    actions:",
                "      LEFT:",
                "        - \"nextpage\"",
                "  - lines:",
                "      - content: \"§a§l第二页\"",
                "        height: 0.3",
                "      - content: \"§7这是悬浮字的第二页\"",
                "      - content: \"§c← 右键翻回\"",
                "    actions:",
                "      RIGHT:",
                "        - \"prevpage\""
        );
        try { Files.write(f.toPath(), lines); } catch (IOException ignored) {}
    }

    // ===== 工具方法 =====

    private int getIndent(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') indent++;
            else if (c == '\t') indent += 4;
            else break;
        }
        return indent;
    }

    private int parseInt(String str, int def) {
        try { return Integer.parseInt(str); } catch (Exception e) { return def; }
    }

    private float parseFloat(String str, float def) {
        try { return Float.parseFloat(str); } catch (Exception e) { return def; }
    }

    private String extractQuotedValue(String line, int prefixLen) {
        String v = line.substring(prefixLen).trim();
        if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
        return v;
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) return "minecraft:overworld";
        if (dimension.contains(":")) return dimension.toLowerCase();
        switch (dimension.toLowerCase()) {
            case "overworld": return "minecraft:overworld";
            case "nether": return "minecraft:the_nether";
            case "end": return "minecraft:the_end";
            default: return "minecraft:" + dimension.toLowerCase();
        }
    }

    private String escapeYaml(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ===== 内部数据类 =====

    private static class PageData {
        List<LineData> lines = new ArrayList<>();
        java.util.Map<String, List<String>> actions = new java.util.LinkedHashMap<>();
    }

    private static class LineData {
        String content = "";
        double height = 0;
        double offsetX = 0;
        double offsetZ = 0;
        String permission;
    }
}
