package org.windy.hologram.config;

import org.yaml.snakeyaml.Yaml;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Display 实体默认属性配置管理器。
 * <p>从 attribute-defaults.yml 加载每种 Display 类型的默认属性值。
 */
public class AttributeDefaults {

    private final Path dataDir;
    private Map<String, Object> root;

    // 缓存的默认配置
    private DisplayConfig defaultText;
    private DisplayConfig defaultItem;
    private DisplayConfig defaultBlock;

    public AttributeDefaults(Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * 加载配置文件。
     */
    public void load() {
        File file = dataDir.resolve("attribute-defaults.yml").toFile();
        if (!file.exists()) {
            // 从 jar 中复制默认配置
            copyDefault(file);
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            root = yaml.load(fis);
            if (root == null) root = new LinkedHashMap<>();
        } catch (Exception e) {
            System.err.println("[VelocityHologram] 加载 attribute-defaults.yml 失败: " + e.getMessage());
            root = new LinkedHashMap<>();
        }

        // 解析默认配置
        defaultText = parseDisplayConfig("TEXT", DisplayEntityType.TEXT_DISPLAY);
        defaultItem = parseDisplayConfig("ITEM", DisplayEntityType.ITEM_DISPLAY);
        defaultBlock = parseDisplayConfig("BLOCK", DisplayEntityType.BLOCK_DISPLAY);
    }

    /**
     * 获取 Text Display 默认配置。
     */
    public DisplayConfig getDefaultText() {
        return defaultText != null ? defaultText : DisplayConfig.DEFAULT_TEXT;
    }

    /**
     * 获取 Item Display 默认配置。
     */
    public DisplayConfig getDefaultItem() {
        return defaultItem != null ? defaultItem : DisplayConfig.DEFAULT_ITEM;
    }

    /**
     * 获取 Block Display 默认配置。
     */
    public DisplayConfig getDefaultBlock() {
        return defaultBlock != null ? defaultBlock : DisplayConfig.DEFAULT_BLOCK;
    }

    /**
     * 获取指定类型的默认配置。
     */
    public DisplayConfig getDefault(DisplayEntityType type) {
        switch (type) {
            case TEXT_DISPLAY: return getDefaultText();
            case ITEM_DISPLAY: return getDefaultItem();
            case BLOCK_DISPLAY: return getDefaultBlock();
            default: return getDefaultText();
        }
    }

    /**
     * 解析 Display 配置。
     */
    @SuppressWarnings("unchecked")
    private DisplayConfig parseDisplayConfig(String section, DisplayEntityType type) {
        Object displayObj = root.get("display");
        if (!(displayObj instanceof Map)) return null;

        Map<String, Object> display = (Map<String, Object>) displayObj;
        Object sectionObj = display.get(section);
        if (!(sectionObj instanceof Map)) return null;

        Map<String, Object> sectionMap = (Map<String, Object>) sectionObj;
        DisplayConfig.Builder builder = DisplayConfig.builder(type);

        // Billboard
        Object billboardObj = sectionMap.get("billboard");
        if (billboardObj instanceof Map) {
            Map<String, Object> billboard = (Map<String, Object>) billboardObj;
            if (isEnabled(billboard)) {
                String value = getString(billboard, "value", "CENTER");
                builder.billboard(DisplayConfig.Billboard.fromConfig(value));
            }
        }

        // Scale
        Object scaleObj = sectionMap.get("scale");
        if (scaleObj instanceof Map) {
            Map<String, Object> scale = (Map<String, Object>) scaleObj;
            if (isEnabled(scale)) {
                Map<String, Object> value = getMap(scale, "value");
                if (value != null) {
                    float x = getFloat(value, "x", 1f);
                    float y = getFloat(value, "y", 1f);
                    float z = getFloat(value, "z", 1f);
                    builder.scale(x, y, z);
                }
            }
        }

        // Background Color (仅 TEXT)
        if (type == DisplayEntityType.TEXT_DISPLAY) {
            Object bgObj = sectionMap.get("background-color");
            if (bgObj instanceof Map) {
                Map<String, Object> bg = (Map<String, Object>) bgObj;
                if (isEnabled(bg)) {
                    Map<String, Object> value = getMap(bg, "value");
                    if (value != null) {
                        int r = getInt(value, "red", 0);
                        int g = getInt(value, "green", 0);
                        int b = getInt(value, "blue", 0);
                        int a = getInt(value, "alpha", 64);
                        builder.backgroundColor((a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }

            // Text Opacity
            Object opacityObj = sectionMap.get("text-opacity");
            if (opacityObj instanceof Map) {
                Map<String, Object> opacity = (Map<String, Object>) opacityObj;
                if (isEnabled(opacity)) {
                    int value = getInt(opacity, "value", 255);
                    builder.textOpacity((byte) value);
                }
            }

            // Line Width
            Object lineWidthObj = sectionMap.get("line-width");
            if (lineWidthObj instanceof Map) {
                Map<String, Object> lineWidth = (Map<String, Object>) lineWidthObj;
                if (isEnabled(lineWidth)) {
                    int value = getInt(lineWidth, "value", 200);
                    builder.lineWidth(value);
                }
            }

            // Style Flags (see-through + text-shadow)
            boolean seeThrough = false;
            boolean textShadow = false;

            Object seeThroughObj = sectionMap.get("see-through");
            if (seeThroughObj instanceof Map) {
                Map<String, Object> st = (Map<String, Object>) seeThroughObj;
                if (isEnabled(st)) {
                    seeThrough = getBoolean(st, "value", false);
                }
            }

            Object textShadowObj = sectionMap.get("text-shadow");
            if (textShadowObj instanceof Map) {
                Map<String, Object> ts = (Map<String, Object>) textShadowObj;
                if (isEnabled(ts)) {
                    textShadow = getBoolean(ts, "value", false);
                }
            }

            byte styleFlags = 0;
            if (seeThrough) styleFlags |= 0x01;
            if (textShadow) styleFlags |= 0x02;
            builder.styleFlags(styleFlags);
        }

        return builder.build();
    }

    /**
     * 检查是否启用。
     */
    @SuppressWarnings("unchecked")
    private boolean isEnabled(Map<String, Object> map) {
        Object enabled = map.get("enabled");
        return enabled instanceof Boolean && (Boolean) enabled;
    }

    /**
     * 获取字符串值。
     */
    private String getString(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : def;
    }

    /**
     * 获取整数值。
     */
    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : def;
    }

    /**
     * 获取浮点值。
     */
    private float getFloat(Map<String, Object> map, String key, float def) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).floatValue() : def;
    }

    /**
     * 获取布尔值。
     */
    private boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = map.get(key);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    /**
     * 获取 Map 值。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    /**
     * 从 jar 中复制默认配置文件。
     */
    private void copyDefault(File target) {
        try {
            Files.createDirectories(dataDir);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("attribute-defaults.yml")) {
                if (is != null) {
                    Files.copy(is, target.toPath());
                }
            }
        } catch (IOException e) {
            System.err.println("[VelocityHologram] 复制默认 attribute-defaults.yml 失败: " + e.getMessage());
        }
    }
}
