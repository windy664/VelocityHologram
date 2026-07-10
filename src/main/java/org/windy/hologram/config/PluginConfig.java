package org.windy.hologram.config;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主配置文件 config.yml。
 *
 * <pre>
 * # RCON 服务器配置（用于 rcon: 动作）
 * rcon:
 *   lobby:
 *     host: 127.0.0.1
 *     port: 25575
 *     password: "xxx"
 *   survival:
 *     host: 127.0.0.1
 *     port: 25576
 *     password: "xxx"
 *
 * # 默认设置
 * defaults:
 *   view-distance: 48
 *   line-spacing: 0.3
 *   background-color: 0x40000000
 * </pre>
 */
public class PluginConfig {

    private final Path dataDir;
    private Map<String, Object> root;

    public PluginConfig(Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * 加载或创建配置文件。
     */
    public void load() {
        File file = dataDir.resolve("config.yml").toFile();
        if (!file.exists()) {
            createDefault(file);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            root = yaml.load(fis);
            if (root == null) root = new LinkedHashMap<>();
        } catch (Exception e) {
            System.err.println("[VelocityHologram] 加载 config.yml 失败: " + e.getMessage());
            root = new LinkedHashMap<>();
        }
    }

    /**
     * 获取 RCON 服务器配置。
     *
     * @return Map<serverName, RconServerConfig>
     */
    @SuppressWarnings("unchecked")
    public Map<String, RconServerConfig> getRconServers() {
        Object rcon = root.get("rcon");
        if (!(rcon instanceof Map)) return Collections.emptyMap();

        Map<String, RconServerConfig> result = new LinkedHashMap<>();
        Map<String, Object> rconMap = (Map<String, Object>) rcon;
        for (Map.Entry<String, Object> entry : rconMap.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> serverMap = (Map<String, Object>) entry.getValue();
                String host = String.valueOf(serverMap.getOrDefault("host", "127.0.0.1"));
                int port = toInt(serverMap.get("port"), 25575);
                String password = String.valueOf(serverMap.getOrDefault("password", ""));
                result.put(entry.getKey(), new RconServerConfig(host, port, password));
            }
        }
        return result;
    }

    // ===== 更新检查器 =====

    public boolean isCheckForUpdates() {
        return getBoolean("update-checker", true);
    }

    // ===== 默认设置 =====

    public String getDefaultText() {
        return getString("defaults.text", "");
    }

    public boolean isDefaultDownOrigin() {
        return getBoolean("defaults.down-origin", false);
    }

    public double getDefaultHeightText() {
        return getDouble("defaults.height.text", 0.3);
    }

    public double getDefaultHeightIcon() {
        return getDouble("defaults.height.icon", 0.6);
    }

    public double getDefaultHeightHead() {
        return getDouble("defaults.height.head", 0.75);
    }

    public double getDefaultHeightSmallHead() {
        return getDouble("defaults.height.smallhead", 0.6);
    }

    public int getDefaultDisplayRange() {
        return getInt("defaults.display-range", 48);
    }

    public int getDefaultUpdateRange() {
        return getInt("defaults.update-range", 48);
    }

    public int getDefaultUpdateInterval() {
        return getInt("defaults.update-interval", 20);
    }

    public int getDefaultLruCacheSize() {
        return getInt("defaults.lru-cache-size", 500);
    }

    public double getDefaultViewDistance() {
        return getDefaultsDouble("view-distance", 48.0);
    }

    public double getDefaultLineSpacing() {
        return getDefaultsDouble("line-spacing", 0.3);
    }

    public int getDefaultBackgroundColor() {
        return getDefaultsInt("background-color", 0x40000000);
    }

    public int getClickCooldown() {
        return getDefaultsInt("click-cooldown", 1);
    }

    public boolean isEyeLevelPositioning() {
        return getBoolean("defaults.eye-level-positioning", false);
    }

    public boolean isDisplaysEyeLevelPositioning() {
        return getBoolean("defaults.displays-eye-level-positioning", false);
    }

    // ===== 动画设置 =====

    public boolean isAllowPlaceholdersInsideAnimations() {
        return getBoolean("allow-placeholders-inside-animations", false);
    }

    // ===== 可见性设置 =====

    public boolean isUpdateVisibilityOnTeleport() {
        return getBoolean("update-visibility-on-teleport", false);
    }

    // ===== 皮肤设置 =====

    public int getPlayerSkinConnectionTimeout() {
        return getInt("player-skin-connection-timeout", 5);
    }

    // ===== 自定义替换字符 =====

    @SuppressWarnings("unchecked")
    public Map<String, String> getCustomReplacements() {
        Object replacements = root.get("custom-replacements");
        if (replacements instanceof Map) {
            return (Map<String, String>) replacements;
        }
        return Collections.emptyMap();
    }

    // ===== 伤害显示配置 =====

    public boolean isDamageDisplayEnabled() {
        return getBoolean("damage-display.enabled", false);
    }

    public int getDamageDisplayDuration() {
        return getInt("damage-display.duration", 40);
    }

    public String getDamageDisplayAppearance() {
        return getString("damage-display.appearance", "§c-{damage}❤");
    }

    public String getDamageDisplayCriticalAppearance() {
        return getString("damage-display.critical-appearance", "§4§l暴击！ §c-{damage}❤");
    }

    // ===== 治疗显示配置 =====

    public boolean isHealingDisplayEnabled() {
        return getBoolean("healing-display.enabled", false);
    }

    public int getHealingDisplayDuration() {
        return getInt("healing-display.duration", 40);
    }

    public String getHealingDisplayAppearance() {
        return getString("healing-display.appearance", "§a+{healing}❤");
    }

    @SuppressWarnings("unchecked")
    private boolean getBoolean(String path, boolean def) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return def;
            }
        }
        Object val = current.get(parts[parts.length - 1]);
        if (val instanceof Boolean) return (Boolean) val;
        return def;
    }

    @SuppressWarnings("unchecked")
    private int getInt(String path, int def) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return def;
            }
        }
        Object val = current.get(parts[parts.length - 1]);
        if (val instanceof Number) return ((Number) val).intValue();
        return def;
    }

    @SuppressWarnings("unchecked")
    private double getDouble(String path, double def) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return def;
            }
        }
        Object val = current.get(parts[parts.length - 1]);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return def;
    }

    @SuppressWarnings("unchecked")
    private String getString(String path, String def) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return def;
            }
        }
        Object val = current.get(parts[parts.length - 1]);
        if (val instanceof String) return (String) val;
        return def;
    }

    @SuppressWarnings("unchecked")
    private double getDefaultsDouble(String key, double def) {
        Object defaults = root.get("defaults");
        if (defaults instanceof Map) {
            Object val = ((Map<String, Object>) defaults).get(key);
            if (val instanceof Number) return ((Number) val).doubleValue();
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private int getDefaultsInt(String key, int def) {
        Object defaults = root.get("defaults");
        if (defaults instanceof Map) {
            Object val = ((Map<String, Object>) defaults).get(key);
            if (val instanceof Number) return ((Number) val).intValue();
        }
        return def;
    }

    private int toInt(Object val, int def) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return def; }
    }

    private void createDefault(File file) {
        try {
            Files.createDirectories(dataDir);
            List<String> lines = List.of(
                    "# VelocityHologram 主配置",
                    "# 对标 DecentHolograms 配置格式",
                    "",
                    "# =========================================",
                    "# 更新检查器",
                    "# =========================================",
                    "update-checker: true",
                    "",
                    "# =========================================",
                    "# RCON 服务器配置（用于 rcon: 动作和 /holo tp）",
                    "# =========================================",
                    "rcon:",
                    "  lobby:",
                    "    host: 127.0.0.1",
                    "    port: 25575",
                    "    password: \"\"",
                    "",
                    "# =========================================",
                    "# 默认设置",
                    "# =========================================",
                    "defaults:",
                    "  # 默认文本（空行显示的文本）",
                    "  text: \"\"",
                    "",
                    "  # 向下生长原点（行从下往上排列）",
                    "  down-origin: false",
                    "",
                    "  # 行高设置",
                    "  height:",
                    "    text: 0.3",
                    "    icon: 0.6",
                    "    head: 0.75",
                    "    smallhead: 0.6",
                    "",
                    "  # 显示范围（玩家进入此范围才显示）",
                    "  display-range: 48",
                    "",
                    "  # 更新范围（玩家在此范围内才接收更新）",
                    "  update-range: 48",
                    "",
                    "  # 更新间隔（tick，1秒=20tick）",
                    "  update-interval: 20",
                    "",
                    "  # 点击冷却（秒）",
                    "  click-cooldown: 1",
                    "",
                    "  # LRU 缓存大小",
                    "  lru-cache-size: 500",
                    "",
                    "  # 眼级定位（悬浮字在玩家眼高还是脚高）",
                    "  eye-level-positioning: false",
                    "  displays-eye-level-positioning: false",
                    "",
                    "  # 背景颜色",
                    "  background-color: 0x40000000",
                    "",
                    "  # 行间距",
                    "  line-spacing: 0.3",
                    "",
                    "# =========================================",
                    "# 动画设置",
                    "# =========================================",
                    "allow-placeholders-inside-animations: false",
                    "",
                    "# =========================================",
                    "# 可见性设置",
                    "# =========================================",
                    "# 传送/重生时更新可见性（可能导致视觉闪烁）",
                    "update-visibility-on-teleport: false",
                    "",
                    "# =========================================",
                    "# 自定义替换字符",
                    "# =========================================",
                    "custom-replacements:",
                    "  \"[x]\": \"█\"",
                    "  \"[X]\": \"█\"",
                    "  \"[/]\": \"▌\"",
                    "  \"[,]\": \"░\"",
                    "  \"[,,]\": \"▒\"",
                    "  \"[,,,]\": \"▓\"",
                    "  \"[p]\": \"•\"",
                    "  \"[P]\": \"•\"",
                    "  \"[|]\": \"⎟\"",
                    "",
                    "# =========================================",
                    "# 皮肤设置",
                    "# =========================================",
                    "player-skin-connection-timeout: 5",
                    "",
                    "# =========================================",
                    "# 伤害显示",
                    "# =========================================",
                    "damage-display:",
                    "  enabled: false",
                    "  duration: 40",
                    "  appearance: \"§c-{damage}❤\"",
                    "  critical-appearance: \"§4§l暴击！ §c-{damage}❤\"",
                    "",
                    "# =========================================",
                    "# 治疗显示",
                    "# =========================================",
                    "healing-display:",
                    "  enabled: false",
                    "  duration: 40",
                    "  appearance: \"§a+{healing}❤\""
            );
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            System.err.println("[VelocityHologram] 创建默认 config.yml 失败");
        }
    }

    /**
     * RCON 服务器配置。
     */
    public static class RconServerConfig {
        public final String host;
        public final int port;
        public final String password;

        public RconServerConfig(String host, int port, String password) {
            this.host = host;
            this.port = port;
            this.password = password;
        }
    }
}
