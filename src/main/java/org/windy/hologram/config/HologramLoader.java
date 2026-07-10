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
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 悬浮字配置加载/保存器。
 * <p>对标 DecentHolograms 配置格式。
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
            if (!configDir.mkdirs() && !configDir.exists()) {
                System.err.println("[VelocityHologram] 无法创建悬浮字配置目录");
                return;
            }
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

    @SuppressWarnings("unchecked")
    private void loadFile(File file, HologramManager manager) throws IOException {
        Map<String, Object> root;
        try (FileInputStream input = new FileInputStream(file)) {
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map)) {
                throw new IOException("配置根节点必须是 Map");
            }
            root = (Map<String, Object>) loaded;
        }

        String name = file.getName().substring(0, file.getName().length() - 4);
        LocationData location = parseLocation(stringValue(root.get("location"), null));

        Hologram hologram = manager.createHologram(
                name,
                location.x,
                location.y,
                location.z,
                location.dimension,
                stringValue(root.get("server"), "")
        );

        hologram.setEnabled(booleanValue(root.get("enabled"), true));
        hologram.setViewDistance(doubleValue(root.get("display-range"), 48));
        hologram.setUpdateDistance(doubleValue(root.get("update-range"), 48));
        hologram.setUpdateInterval(intValue(root.get("update-interval"), 20));
        hologram.setFacing(floatValue(root.get("facing"), 0), floatValue(root.get("pitch"), 0));
        hologram.setDownOrigin(booleanValue(root.get("down-origin"), false));
        hologram.setAlwaysFacePlayer(booleanValue(root.get("always-face-player"), false));

        String permission = stringValue(root.get("permission"), null);
        if (permission != null && !permission.isBlank()) {
            hologram.setPermission(permission);
        }

        Object flagsObject = root.get("flags");
        if (flagsObject instanceof List) {
            for (Object flag : (List<?>) flagsObject) {
                if (flag != null) {
                    hologram.addFlag(String.valueOf(flag));
                }
            }
        }

        Object pagesObject = root.get("pages");
        List<?> pages = normalizeCollection(pagesObject);
        if (pages.isEmpty()) return;

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Object pageObject = pages.get(pageIndex);
            if (!(pageObject instanceof Map)) continue;

            Map<String, Object> pageMap = (Map<String, Object>) pageObject;
            Page page = pageIndex == 0 ? hologram.getPage(0) : hologram.addPage();
            if (page == null) continue;

            loadLines(page, pageMap.get("lines"), hologram);
            loadPageActions(page, pageMap.get("actions"), hologram);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLines(Page page, Object linesObject, Hologram hologram) {
        for (Object lineObject : normalizeCollection(linesObject)) {
            if (lineObject instanceof String) {
                page.addLine((String) lineObject);
                continue;
            }
            if (!(lineObject instanceof Map)) continue;

            Map<String, Object> lineMap = (Map<String, Object>) lineObject;
            DisplayConfig config = parseDisplayConfig(lineMap);
            HologramLine line = page.addLine(config);

            line.setLineHeight(doubleValue(
                    firstNonNull(lineMap.get("height"), lineMap.get("line-height")), 0));
            line.setOffsetX(doubleValue(
                    firstNonNull(lineMap.get("offsetX"), lineMap.get("offset-x")), 0));
            line.setOffsetY(doubleValue(
                    firstNonNull(lineMap.get("offsetY"), lineMap.get("offset-y")), 0));
            line.setOffsetZ(doubleValue(
                    firstNonNull(lineMap.get("offsetZ"), lineMap.get("offset-z")), 0));

            String permission = stringValue(lineMap.get("permission"), null);
            if (permission != null && !permission.isBlank()) {
                line.setPermission(permission);
            }

            Object flagsObject = lineMap.get("flags");
            if (flagsObject instanceof List) {
                for (Object flag : (List<?>) flagsObject) {
                    if (flag != null) {
                        line.addFlag(String.valueOf(flag));
                    }
                }
            }

            line.setLeftClickAction(parseAction(lineMap.get("left-click"), hologram));
            line.setRightClickAction(parseAction(lineMap.get("right-click"), hologram));
            line.setShiftLeftClickAction(parseAction(lineMap.get("shift-left-click"), hologram));
            line.setShiftRightClickAction(parseAction(lineMap.get("shift-right-click"), hologram));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPageActions(Page page, Object actionsObject, Hologram hologram) {
        if (!(actionsObject instanceof Map)) return;

        Map<String, Object> actions = (Map<String, Object>) actionsObject;
        for (Map.Entry<String, Object> entry : actions.entrySet()) {
            List<?> configuredActions = normalizeCollection(entry.getValue());
            for (Object configuredAction : configuredActions) {
                Action action = parseAction(configuredAction, hologram);
                if (action != null) {
                    page.addAction(normalizeClickType(entry.getKey()), action);
                }
            }
        }
    }

    private Action parseAction(Object value, Hologram hologram) {
        if (value == null) return null;
        return ActionFactory.parse(String.valueOf(value), hologram);
    }

    private DisplayConfig parseDisplayConfig(Map<String, Object> lineMap) {
        String explicitType = stringValue(lineMap.get("type"), null);
        if (explicitType != null) {
            DisplayEntityType type = DisplayEntityType.fromConfig(explicitType);
            return buildDisplayConfig(type, lineMap);
        }

        if (lineMap.containsKey("item")) {
            return buildDisplayConfig(DisplayEntityType.ITEM_DISPLAY, lineMap);
        }
        if (lineMap.containsKey("block")) {
            return buildDisplayConfig(DisplayEntityType.BLOCK_DISPLAY, lineMap);
        }
        if (lineMap.containsKey("entity")) {
            return buildDisplayConfig(DisplayEntityType.ENTITY, lineMap);
        }
        if (lineMap.containsKey("head")) {
            return buildDisplayConfig(DisplayEntityType.HEAD, lineMap);
        }
        if (lineMap.containsKey("smallhead")) {
            return buildDisplayConfig(DisplayEntityType.SMALLHEAD, lineMap);
        }
        if (lineMap.containsKey("icon")) {
            return buildDisplayConfig(DisplayEntityType.ICON, lineMap);
        }

        String content = stringValue(
                firstNonNull(lineMap.get("content"), lineMap.get("text")), "");
        return parseLegacyContent(content);
    }

    private DisplayConfig buildDisplayConfig(DisplayEntityType type, Map<String, Object> lineMap) {
        DisplayConfig.Builder builder = DisplayConfig.builder(type);

        switch (type) {
            case TEXT_DISPLAY:
                builder.text(stringValue(
                        firstNonNull(lineMap.get("text"), lineMap.get("content")), ""));
                break;
            case ITEM_DISPLAY:
                builder.itemId(stringValue(lineMap.get("item"), "minecraft:stone"));
                break;
            case BLOCK_DISPLAY:
                builder.blockId(stringValue(lineMap.get("block"), "minecraft:stone"));
                break;
            case ENTITY:
                builder.blockId(stringValue(lineMap.get("entity"), "minecraft:pig"));
                break;
            case HEAD:
                builder.blockId(stringValue(lineMap.get("head"), "minecraft:stone"));
                break;
            case SMALLHEAD:
                builder.blockId(stringValue(
                        firstNonNull(lineMap.get("smallhead"), lineMap.get("head")),
                        "minecraft:stone"));
                break;
            case ICON:
                builder.blockId(stringValue(lineMap.get("icon"), "minecraft:stone"));
                break;
            default:
                break;
        }

        String billboard = stringValue(lineMap.get("billboard"), null);
        if (billboard != null) {
            builder.billboard(DisplayConfig.Billboard.fromConfig(billboard));
        }

        builder.scale(
                floatValue(lineMap.get("scale-x"), 1),
                floatValue(lineMap.get("scale-y"), 1),
                floatValue(lineMap.get("scale-z"), 1)
        );
        builder.backgroundColor(intValue(lineMap.get("background-color"), 0x40000000));
        builder.textOpacity((byte) intValue(lineMap.get("text-opacity"), 255));
        builder.styleFlags((byte) intValue(lineMap.get("style-flags"), 0));
        builder.lineWidth(intValue(lineMap.get("line-width"), 200));
        return builder.build();
    }

    private DisplayConfig parseLegacyContent(String content) {
        String upper = content.toUpperCase();
        if (upper.startsWith("#ICON:")) {
            return DisplayConfig.builder(DisplayEntityType.ICON)
                    .blockId(content.substring(6))
                    .build();
        }
        if (upper.startsWith("#HEAD:")) {
            return DisplayConfig.builder(DisplayEntityType.HEAD)
                    .blockId(content.substring(6))
                    .build();
        }
        if (upper.startsWith("#SMALLHEAD:")) {
            return DisplayConfig.builder(DisplayEntityType.SMALLHEAD)
                    .blockId(content.substring(11))
                    .build();
        }
        if (upper.startsWith("#ENTITY:")) {
            return DisplayConfig.builder(DisplayEntityType.ENTITY)
                    .blockId(content.substring(8))
                    .build();
        }
        if (upper.startsWith("#ITEM:")) {
            return DisplayConfig.builder(DisplayEntityType.ITEM_DISPLAY)
                    .itemId(content.substring(6))
                    .build();
        }
        if (upper.startsWith("#BLOCK:")) {
            return DisplayConfig.builder(DisplayEntityType.BLOCK_DISPLAY)
                    .blockId(content.substring(7))
                    .build();
        }
        return DisplayConfig.builder(DisplayEntityType.TEXT_DISPLAY)
                .text(content)
                .build();
    }

    public void delete(String name) {
        File configFile = dataDir.resolve("holograms").resolve(name + ".yml").toFile();
        if (configFile.exists() && !configFile.delete()) {
            System.err.println("[VelocityHologram] 删除悬浮字配置失败: " + name);
        }
    }

    public void save(Hologram hologram) {
        File configDir = dataDir.resolve("holograms").toFile();
        if (!configDir.exists() && !configDir.mkdirs() && !configDir.exists()) {
            System.err.println("[VelocityHologram] 无法创建悬浮字配置目录");
            return;
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("location", serializeLocation(hologram));
        root.put("server", hologram.getPosition().server());
        root.put("enabled", hologram.isEnabled());
        root.put("display-range", hologram.getViewDistance());
        root.put("update-range", hologram.getUpdateDistance());
        root.put("update-interval", hologram.getUpdateInterval());
        root.put("facing", hologram.getFacingYaw());
        root.put("pitch", hologram.getFacingPitch());
        root.put("down-origin", hologram.isDownOrigin());
        root.put("always-face-player", hologram.isAlwaysFacePlayer());

        if (hologram.getPermission() != null && !hologram.getPermission().isBlank()) {
            root.put("permission", hologram.getPermission());
        }
        if (!hologram.getFlags().isEmpty()) {
            root.put("flags", new ArrayList<>(hologram.getFlags()));
        }

        List<Map<String, Object>> pages = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < hologram.getPageCount(); pageIndex++) {
            Page page = hologram.getPage(pageIndex);
            if (page == null) continue;

            Map<String, Object> pageMap = new LinkedHashMap<>();
            List<Map<String, Object>> lines = new ArrayList<>();

            for (HologramLine line : page.getLines()) {
                lines.add(serializeLine(line));
            }
            pageMap.put("lines", lines);

            if (!page.getActions().isEmpty()) {
                Map<String, List<String>> actions = new LinkedHashMap<>();
                for (Map.Entry<String, Action> entry : page.getActions().entrySet()) {
                    actions.computeIfAbsent(entry.getKey().toUpperCase(), key -> new ArrayList<>())
                            .add(ActionFactory.serialize(entry.getValue()));
                }
                pageMap.put("actions", actions);
            }

            pages.add(pageMap);
        }
        root.put("pages", pages);

        File file = new File(configDir, hologram.getName() + ".yml");
        try {
            String yaml = new Yaml().dump(root);
            Files.writeString(file.toPath(), yaml);
        } catch (IOException e) {
            System.err.println("[VelocityHologram] 保存悬浮字配置失败: " + hologram.getName());
            e.printStackTrace();
        }
    }

    private Map<String, Object> serializeLine(HologramLine line) {
        Map<String, Object> map = new LinkedHashMap<>();
        DisplayConfig config = line.getDisplayConfig();

        map.put("type", config.type().toConfig());
        switch (config.type()) {
            case TEXT_DISPLAY:
                map.put("text", line.getRawText());
                break;
            case ITEM_DISPLAY:
                map.put("item", config.itemId());
                break;
            case BLOCK_DISPLAY:
                map.put("block", config.blockId());
                break;
            case ENTITY:
                map.put("entity", config.blockId());
                break;
            case HEAD:
                map.put("head", config.blockId());
                break;
            case SMALLHEAD:
                map.put("smallhead", config.blockId());
                break;
            case ICON:
                map.put("icon", config.blockId());
                break;
            default:
                break;
        }

        if (config.billboard() != DisplayConfig.Billboard.CENTER) {
            map.put("billboard", config.billboard().name());
        }
        if (config.scaleX() != 1) map.put("scale-x", config.scaleX());
        if (config.scaleY() != 1) map.put("scale-y", config.scaleY());
        if (config.scaleZ() != 1) map.put("scale-z", config.scaleZ());
        if (config.backgroundColor() != 0x40000000) {
            map.put("background-color", config.backgroundColor());
        }
        if (Byte.toUnsignedInt(config.textOpacity()) != 255) {
            map.put("text-opacity", Byte.toUnsignedInt(config.textOpacity()));
        }
        if (config.styleFlags() != 0) {
            map.put("style-flags", Byte.toUnsignedInt(config.styleFlags()));
        }
        if (config.lineWidth() != 200) {
            map.put("line-width", config.lineWidth());
        }

        if (line.getLineHeight() > 0) map.put("height", line.getLineHeight());
        if (line.getOffsetX() != 0) map.put("offset-x", line.getOffsetX());
        if (line.getOffsetY() != 0) map.put("offset-y", line.getOffsetY());
        if (line.getOffsetZ() != 0) map.put("offset-z", line.getOffsetZ());
        if (line.getPermission() != null) map.put("permission", line.getPermission());
        if (!line.getFlags().isEmpty()) map.put("flags", new ArrayList<>(line.getFlags()));

        if (line.getLeftClickAction() != null) {
            map.put("left-click", ActionFactory.serialize(line.getLeftClickAction()));
        }
        if (line.getRightClickAction() != null) {
            map.put("right-click", ActionFactory.serialize(line.getRightClickAction()));
        }
        if (line.getShiftLeftClickAction() != null) {
            map.put("shift-left-click", ActionFactory.serialize(line.getShiftLeftClickAction()));
        }
        if (line.getShiftRightClickAction() != null) {
            map.put("shift-right-click", ActionFactory.serialize(line.getShiftRightClickAction()));
        }

        return map;
    }

    private String serializeLocation(Hologram hologram) {
        return hologram.getPosition().dimension()
                + ":" + hologram.getPosition().x()
                + ":" + hologram.getPosition().y()
                + ":" + hologram.getPosition().z();
    }

    private LocationData parseLocation(String value) {
        if (value == null || value.isBlank()) {
            return new LocationData("minecraft:overworld", 0, 0, 0);
        }

        int zSeparator = value.lastIndexOf(':');
        int ySeparator = zSeparator > 0 ? value.lastIndexOf(':', zSeparator - 1) : -1;
        int xSeparator = ySeparator > 0 ? value.lastIndexOf(':', ySeparator - 1) : -1;

        if (xSeparator < 1 || ySeparator < 0 || zSeparator < 0) {
            return new LocationData("minecraft:overworld", 0, 0, 0);
        }

        try {
            String dimension = normalizeDimension(value.substring(0, xSeparator));
            double x = Double.parseDouble(value.substring(xSeparator + 1, ySeparator));
            double y = Double.parseDouble(value.substring(ySeparator + 1, zSeparator));
            double z = Double.parseDouble(value.substring(zSeparator + 1));
            return new LocationData(dimension, x, y, z);
        } catch (NumberFormatException e) {
            return new LocationData("minecraft:overworld", 0, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> normalizeCollection(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        }
        if (value instanceof Map) {
            return new ArrayList<>(((Map<Object, Object>) value).values());
        }
        return List.of();
    }

    private String normalizeClickType(String value) {
        return value.toLowerCase().replace('_', '-');
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return "minecraft:overworld";
        if (dimension.contains(":")) return dimension.toLowerCase();

        switch (dimension.toLowerCase()) {
            case "overworld":
            case "the_overworld":
                return "minecraft:overworld";
            case "nether":
            case "the_nether":
                return "minecraft:the_nether";
            case "end":
            case "the_end":
                return "minecraft:the_end";
            default:
                return "minecraft:" + dimension.toLowerCase();
        }
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String stringValue(Object value, String defaultValue) {
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return Boolean.parseBoolean(String.valueOf(value));
        return defaultValue;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value != null ? Integer.decode(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private float floatValue(Object value, float defaultValue) {
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return value != null ? Float.parseFloat(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void createDefaultConfig(File configDir) {
        File file = new File(configDir, "welcome.yml");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("location", "minecraft:overworld:0.5:100.0:0.5");
        root.put("server", "");
        root.put("enabled", true);
        root.put("display-range", 48);
        root.put("update-range", 48);
        root.put("update-interval", 20);
        root.put("facing", 0.0);
        root.put("down-origin", false);

        List<Map<String, Object>> pages = new ArrayList<>();

        Map<String, Object> firstPage = new LinkedHashMap<>();
        firstPage.put("lines", List.of(
                Map.of("type", "text", "text", "§b§l欢迎来到本服", "height", 0.3),
                Map.of("type", "text", "text", "§7在线: %server_online%/%server_max_players%"),
                Map.of("type", "text", "text", "§e左键翻页 →")
        ));
        firstPage.put("actions", Map.of("LEFT", List.of("nextpage")));
        pages.add(firstPage);

        Map<String, Object> secondPage = new LinkedHashMap<>();
        secondPage.put("lines", List.of(
                Map.of("type", "text", "text", "§a§l第二页", "height", 0.3),
                Map.of("type", "text", "text", "§7这是悬浮字的第二页"),
                Map.of("type", "text", "text", "§c← 右键翻回")
        ));
        secondPage.put("actions", Map.of("RIGHT", List.of("prevpage")));
        pages.add(secondPage);

        root.put("pages", pages);

        try {
            Files.writeString(file.toPath(), new Yaml().dump(root));
        } catch (IOException ignored) {
        }
    }

    private static class LocationData {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;

        private LocationData(String dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
