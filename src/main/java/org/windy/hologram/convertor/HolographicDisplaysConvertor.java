package org.windy.hologram.convertor;

import org.windy.hologram.api.DHAPI;
import org.windy.hologram.hologram.Hologram;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * HolographicDisplays 配置转换器。
 * <p>将 HolographicDisplays 的 YAML 配置转换为 VelocityHolograms 格式。
 */
public class HolographicDisplaysConvertor implements IConvertor {

    private final Path targetDir;

    public HolographicDisplaysConvertor(Path targetDir) {
        this.targetDir = targetDir;
    }

    @Override
    public String getName() {
        return "HolographicDisplays";
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
        double x = toDouble(config.get("x"), 0);
        double y = toDouble(config.get("y"), 100);
        double z = toDouble(config.get("z"), 0);
        String world = String.valueOf(config.getOrDefault("world", "world"));

        // 创建悬浮字
        Hologram hologram = DHAPI.createHologram(name, x, y, z, "minecraft:" + world, "");

        // 解析行
        Object linesObj = config.get("lines");
        if (linesObj instanceof java.util.List) {
            java.util.List<String> lines = (java.util.List<String>) linesObj;
            for (String line : lines) {
                DHAPI.addLine(hologram, line);
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
