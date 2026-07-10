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
 */
public class VersionMapper {

    // ===== 版本索引映射 =====

    // 1.19.4 - 1.20.1
    private static final MetadataIndices V1_19_4 = new MetadataIndices(
            22, 23, 24, 25, 26,  // TextDisplay: text, lineWidth, bgColor, textOpacity, styleFlags
            22, 23, 22,          // ItemDisplay: item, itemTransform / BlockDisplay: blockState
            11, 15               // Scale, Billboard
    );

    // 1.20.2 - 1.20.4
    private static final MetadataIndices V1_20_2 = new MetadataIndices(
            23, 24, 25, 26, 27,  // TextDisplay
            23, 24, 23,          // Item/Block Display
            11, 15               // Scale, Billboard
    );

    // 1.20.5 - 1.20.6
    private static final MetadataIndices V1_20_5 = new MetadataIndices(
            25, 26, 27, 28, 29,  // TextDisplay
            25, 26, 25,          // Item/Block Display
            11, 18               // Scale, Billboard
    );

    // 1.21+ (MC 26.1.2 使用这个)
    private static final MetadataIndices V1_21 = new MetadataIndices(
            25, 26, 27, 28, 29,  // TextDisplay
            25, 26, 25,          // Item/Block Display
            11, 18               // Scale, Billboard
    );

    /**
     * 获取当前连接版本的 TextDisplay 元数据索引。
     */
    public static MetadataIndices getIndices(ClientVersion version) {
        if (version == null) return V1_21;

        if (version.isNewerThanOrEquals(ClientVersion.V_1_21)) {
            return V1_21;
        } else if (version.isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
            return V1_20_5;
        } else if (version.isNewerThanOrEquals(ClientVersion.V_1_20_2)) {
            return V1_20_2;
        } else {
            return V1_19_4;
        }
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
}
