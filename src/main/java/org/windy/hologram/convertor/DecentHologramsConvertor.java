package org.windy.hologram.convertor;

import org.windy.hologram.api.DHAPI;
import org.windy.hologram.hologram.Hologram;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * DecentHolograms 配置转换器。
 * <p>将 DecentHolograms 的 YAML 配置转换为 VelocityHolograms 格式。
 */
public class DecentHologramsConvertor implements IConvertor {

    private final Path targetDir;

    public DecentHologramsConvertor(Path targetDir) {
        this.targetDir = targetDir;
    }

    @Override
    public String getName() {
        return "DecentHolograms";
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean convert(String sourcePath) {
        File sourceDir = new File(sourcePath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.err.println("[VelocityHologram] 源目录不存在: " + sourcePath);
            return false;
        }

        File[] files = sourceDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            System.err.println("[VelocityHologram] 未找到配置文件");
            return false;
        }

        int converted = 0;
        for (File file : files) {
            try {
                if (convertFile(file)) {
                    converted++;
                }
            } catch (Exception e) {
                System.err.println("[VelocityHologram] 转换失败: " + file.getName() + " - " + e.getMessage());
            }
        }

        System.out.println("[VelocityHologram] 已转换 " + converted + "/" + files.length + " 个悬浮字");
        return converted > 0;
    }

    @SuppressWarnings("unchecked")
    private boolean convertFile(File file) throws Exception {
        Yaml yaml = new Yaml();
        Map<String, Object> config;
        try (FileInputStream fis = new FileInputStream(file)) {
            config = yaml.load(fis);
        }

        if (config == null) return false;

        String name = file.getName().replace(".yml", "");

        // 解析位置
        Object locationObj = config.get("location");
        double x = 0, y = 0, z = 0;
        String world = "world";

        if (locationObj instanceof Map) {
            Map<String, Object> loc = (Map<String, Object>) locationObj;
            x = toDouble(loc.get("x"), 0);
            y = toDouble(loc.get("y"), 100);
            z = toDouble(loc.get("z"), 0);
            world = String.valueOf(loc.getOrDefault("world", "world"));
        }

        // 创建悬浮字
        Hologram hologram = DHAPI.createHologram(name, x, y, z, "minecraft:" + world, "");

        // 解析配置
        if (config.containsKey("enabled")) {
            hologram.setEnabled(Boolean.parseBoolean(String.valueOf(config.get("enabled"))));
        }
        if (config.containsKey("display-range")) {
            hologram.setViewDistance(toDouble(config.get("display-range"), 48));
        }
        if (config.containsKey("update-range")) {
            hologram.setUpdateDistance(toDouble(config.get("update-range"), 48));
        }
        if (config.containsKey("update-interval")) {
            hologram.setUpdateInterval((int) toDouble(config.get("update-interval"), 20));
        }
        if (config.containsKey("down-origin")) {
            hologram.setDownOrigin(Boolean.parseBoolean(String.valueOf(config.get("down-origin"))));
        }
        if (config.containsKey("permission")) {
            hologram.setPermission(String.valueOf(config.get("permission")));
        }

        // 解析页面
        Object pagesObj = config.get("pages");
        if (pagesObj instanceof Map) {
            Map<String, Object> pages = (Map<String, Object>) pagesObj;
            boolean firstPage = true;

            for (Map.Entry<String, Object> entry : pages.entrySet()) {
                if (!firstPage) {
                    DHAPI.addPage(hologram);
                }
                firstPage = false;

                Object pageObj = entry.getValue();
                if (pageObj instanceof Map) {
                    Map<String, Object> page = (Map<String, Object>) pageObj;
                    Object linesObj = page.get("lines");

                    if (linesObj instanceof Map) {
                        Map<String, Object> lines = (Map<String, Object>) linesObj;
                        for (Map.Entry<String, Object> lineEntry : lines.entrySet()) {
                            Object lineObj = lineEntry.getValue();
                            if (lineObj instanceof Map) {
                                Map<String, Object> line = (Map<String, Object>) lineObj;
                                String content = String.valueOf(line.getOrDefault("content", ""));
                                DHAPI.addLine(hologram, content);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[VelocityHologram] 已转换: " + name);
        return true;
    }

    private double toDouble(Object val, double def) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return def; }
    }
}
