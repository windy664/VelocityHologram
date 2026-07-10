package org.windy.hologram.display;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;

/**
 * 实体元数据索引版本映射。
 * <p>不同 MC 版本的 Entity/Display 字段数量不同，导致 TextDisplay 等子类的索引偏移。
 *
 * <p>已知偏移原因：
 * <ul>
 *   <li>Entity 类在不同版本增减字段（如 DATA_TICKS_FROZEN 是后加的）</li>
 *   <li>Display 类在不同版本增减字段</li>
 * </ul>
 *
 * <p>规则：以 MC 26.1.2（Entity 8字段, Display 15字段）为基准，
 * 其他版本按字段差值调整索引。
 */
public class VersionMapper {

    // MC 26.1.2 基准（从客户端 jar 反编译确认）
    // Entity: 8 fields (0-7)
    // Display: 15 fields (8-22)
    // TextDisplay: 5 fields (23-27)
    // ItemDisplay: 2 fields (23-24)
    // BlockDisplay: 1 field (23)

    private static final int BASE_ENTITY_FIELDS = 8;
    private static final int BASE_DISPLAY_FIELDS = 15;

    // 基准索引（MC 26.1.2）
    private static final int BASE_TEXT = 23;
    private static final int BASE_LINE_WIDTH = 24;
    private static final int BASE_BACKGROUND_COLOR = 25;
    private static final int BASE_TEXT_OPACITY = 26;
    private static final int BASE_STYLE_FLAGS = 27;
    private static final int BASE_ITEM = 23;
    private static final int BASE_ITEM_TRANSFORM = 24;
    private static final int BASE_BLOCK_STATE = 23;
    private static final int BASE_SCALE = 14;
    private static final int BASE_BILLBOARD = 15;

    /**
     * 获取当前连接版本的 TextDisplay 元数据索引。
     * <p>如果没有精确映射，使用基准值（26.1.2）。
     */
    public static MetadataIndices getIndices(ClientVersion version) {
        // 目前只有 26.1.2 的精确数据
        // 其他版本暂用基准值，后续按需补充
        return DEFAULT;
    }

    /**
     * TextDisplay 元数据索引。
     */
    public static class MetadataIndices {
        public final int text;
        public final int lineWidth;
        public final int backgroundColor;
        public final int textOpacity;
        public final int styleFlags;
        public final int item;
        public final int itemTransform;
        public final int blockState;
        public final int scale;
        public final int billboard;

        public MetadataIndices(int text, int lineWidth, int backgroundColor,
                               int textOpacity, int styleFlags,
                               int item, int itemTransform, int blockState,
                               int scale, int billboard) {
            this.text = text;
            this.lineWidth = lineWidth;
            this.backgroundColor = backgroundColor;
            this.textOpacity = textOpacity;
            this.styleFlags = styleFlags;
            this.item = item;
            this.itemTransform = itemTransform;
            this.blockState = blockState;
            this.scale = scale;
            this.billboard = billboard;
        }
    }

    // 默认值（MC 26.1.2 基准）
    private static final MetadataIndices DEFAULT = new MetadataIndices(
            BASE_TEXT, BASE_LINE_WIDTH, BASE_BACKGROUND_COLOR,
            BASE_TEXT_OPACITY, BASE_STYLE_FLAGS,
            BASE_ITEM, BASE_ITEM_TRANSFORM, BASE_BLOCK_STATE,
            BASE_SCALE, BASE_BILLBOARD
    );

    // ===== 补充其他版本时在此添加 =====
    // 示例：
    // private static final MetadataIndices V1_20_4 = new MetadataIndices(
    //         22, 23, 24, 25, 26,  // TextDisplay
    //         22, 23, 22,          // Item/Block Display
    //         11, 15               // Scale, Billboard (Entity 7字段时)
    // );
}
