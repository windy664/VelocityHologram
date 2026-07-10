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

    public double getDefaultViewDistance() {
        return getDefaultsDouble("view-distance", 48.0);
    }

    public double getDefaultLineSpacing() {
        return getDefaultsDouble("line-spacing", 0.3);
    }

    public int getDefaultBackgroundColor() {
        return getDefaultsInt("background-color", 0x40000000);
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
                    "",
                    "# RCON 服务器配置（用于 rcon: 动作和 /holo tp）",
                    "rcon:",
                    "  shelter:",
                    "    host: 127.0.0.1",
                    "    port: 35565",
                    "    password: \"12345678\"",
                    "",
                    "# 默认设置",
                    "defaults:",
                    "  view-distance: 48",
                    "  line-spacing: 0.3",
                    "  background-color: 0x40000000"
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
