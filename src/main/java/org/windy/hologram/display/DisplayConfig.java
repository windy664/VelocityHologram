package org.windy.hologram.display;

/**
 * Display 实体的统一显示配置。
 * <p>三种 Display 类型共用同一配置结构，各字段按类型取用。
 */
public class DisplayConfig {

    private final DisplayEntityType type;
    private final String text;
    private final String itemId;
    private final String blockId;
    private final Billboard billboard;
    private final float scaleX, scaleY, scaleZ;
    private final int backgroundColor;
    private final byte textOpacity;
    private final byte styleFlags;
    private final int lineWidth;
    private final float[] leftRotation;
    private final float[] rightRotation;
    private final float[] translation;

    public DisplayConfig(DisplayEntityType type, String text, String itemId, String blockId,
                         Billboard billboard, float scaleX, float scaleY, float scaleZ,
                         int backgroundColor, byte textOpacity, byte styleFlags, int lineWidth,
                         float[] leftRotation, float[] rightRotation, float[] translation) {
        this.type = type;
        this.text = text;
        this.itemId = itemId;
        this.blockId = blockId;
        this.billboard = billboard;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.backgroundColor = backgroundColor;
        this.textOpacity = textOpacity;
        this.styleFlags = styleFlags;
        this.lineWidth = lineWidth;
        this.leftRotation = leftRotation;
        this.rightRotation = rightRotation;
        this.translation = translation;
    }

    public DisplayEntityType type() { return type; }
    public String text() { return text; }
    public String itemId() { return itemId; }
    public String blockId() { return blockId; }
    public Billboard billboard() { return billboard; }
    public float scaleX() { return scaleX; }
    public float scaleY() { return scaleY; }
    public float scaleZ() { return scaleZ; }
    public int backgroundColor() { return backgroundColor; }
    public byte textOpacity() { return textOpacity; }
    public byte styleFlags() { return styleFlags; }
    public int lineWidth() { return lineWidth; }
    public float[] leftRotation() { return leftRotation; }
    public float[] rightRotation() { return rightRotation; }
    public float[] translation() { return translation; }

    /** 默认 Text Display 配置。 */
    public static final DisplayConfig DEFAULT_TEXT = new DisplayConfig(
            DisplayEntityType.TEXT_DISPLAY,
            "", null, null,
            Billboard.CENTER,
            1f, 1f, 1f,
            0x40000000,
            (byte) 0xFF, (byte) 0x00, 200,
            null, null, null
    );

    /** 默认 Item Display 配置。 */
    public static final DisplayConfig DEFAULT_ITEM = new DisplayConfig(
            DisplayEntityType.ITEM_DISPLAY,
            null, "minecraft:stone", null,
            Billboard.CENTER,
            1f, 1f, 1f,
            0, (byte) 0, (byte) 0, 0,
            null, null, null
    );

    /** 默认 Block Display 配置。 */
    public static final DisplayConfig DEFAULT_BLOCK = new DisplayConfig(
            DisplayEntityType.BLOCK_DISPLAY,
            null, null, "minecraft:stone",
            Billboard.CENTER,
            1f, 1f, 1f,
            0, (byte) 0, (byte) 0, 0,
            null, null, null
    );

    /**
     * 朝向模式。
     */
    public enum Billboard {
        CENTER(0),
        VERTICAL(1),
        HORIZONTAL(2),
        FIXED(3);

        public final int id;
        Billboard(int id) { this.id = id; }

        public static Billboard fromConfig(String value) {
            if (value == null) return CENTER;
            switch (value.toLowerCase()) {
                case "vertical": return VERTICAL;
                case "horizontal": return HORIZONTAL;
                case "fixed": return FIXED;
                default: return CENTER;
            }
        }
    }

    // ===== Builder =====

    public static Builder builder(DisplayEntityType type) {
        return new Builder(type);
    }

    public static class Builder {
        private DisplayEntityType type;
        private String text = "";
        private String itemId;
        private String blockId;
        private Billboard billboard = Billboard.CENTER;
        private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
        private int backgroundColor = 0x40000000;
        private byte textOpacity = (byte) 0xFF;
        private byte styleFlags = 0;
        private int lineWidth = 200;
        private float[] leftRotation, rightRotation, translation;

        Builder(DisplayEntityType type) { this.type = type; }

        public Builder text(String v) { this.text = v; return this; }
        public Builder itemId(String v) { this.itemId = v; return this; }
        public Builder blockId(String v) { this.blockId = v; return this; }
        public Builder billboard(Billboard v) { this.billboard = v; return this; }
        public Builder scale(float x, float y, float z) { this.scaleX=x; this.scaleY=y; this.scaleZ=z; return this; }
        public Builder backgroundColor(int v) { this.backgroundColor = v; return this; }
        public Builder textOpacity(byte v) { this.textOpacity = v; return this; }
        public Builder styleFlags(byte v) { this.styleFlags = v; return this; }
        public Builder lineWidth(int v) { this.lineWidth = v; return this; }
        public Builder leftRotation(float[] v) { this.leftRotation = v; return this; }
        public Builder rightRotation(float[] v) { this.rightRotation = v; return this; }
        public Builder translation(float[] v) { this.translation = v; return this; }

        public DisplayConfig build() {
            return new DisplayConfig(type, text, itemId, blockId,
                    billboard, scaleX, scaleY, scaleZ,
                    backgroundColor, textOpacity, styleFlags, lineWidth,
                    leftRotation, rightRotation, translation);
        }
    }
}
