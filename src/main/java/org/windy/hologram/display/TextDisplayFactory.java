package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Text Display 实体工厂。
 * <p>用 packetevents 构造 Text Display 实体的生成、销毁、元数据更新包。
 *
 * <p>Text Display 实体元数据索引（1.19.4+）：
 * <ul>
 *   <li>23: Text (Component) - 显示的文本内容</li>
 *   <li>24: Line Width (Int) - 行宽</li>
 *   <li>25: Background Color (Int) - 背景颜色</li>
 *   <li>26: Text Opacity (Byte) - 文本透明度</li>
 *   <li>27: Style flags (Byte) - 样式标志</li>
 * </ul>
 */
public class TextDisplayFactory {

    // Text Display 元数据索引
    private static final int INDEX_TEXT = 23;
    private static final int INDEX_LINE_WIDTH = 24;
    private static final int INDEX_BACKGROUND_COLOR = 25;
    private static final int INDEX_TEXT_OPACITY = 26;
    private static final int INDEX_STYLE_FLAGS = 27;

    // 默认值
    private static final int DEFAULT_LINE_WIDTH = 200;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x40000000; // 半透明黑色
    private static final byte DEFAULT_TEXT_OPACITY = (byte) 0xFF; // 完全不透明
    private static final byte DEFAULT_STYLE_FLAGS = 0x00; // 无阴影

    private TextDisplayFactory() {}

    /**
     * 向玩家发送 Text Display 实体生成包。
     */
    public static void spawn(UUID playerId, int entityId, double x, double y, double z, String text) {
        Object player = getPlayer(playerId);
        if (player == null) return;

        // 生成实体
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(x, y, z),
                0f,   // pitch
                0f,   // yaw
                0f,   // headYaw
                0,    // data
                Optional.empty()
        );

        // 设置元数据（文本内容 + 默认样式）
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, text);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    /**
     * 向玩家发送实体销毁包。
     */
    public static void despawn(UUID playerId, int entityId) {
        Object player = getPlayer(playerId);
        if (player == null) return;

        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
    }

    /**
     * 向玩家发送实体元数据更新包（更新文本内容）。
     */
    public static void updateText(UUID playerId, int entityId, String text) {
        Object player = getPlayer(playerId);
        if (player == null) return;

        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, text);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    /**
     * 创建 Text Display 元数据包。
     */
    private static WrapperPlayServerEntityMetadata createMetadata(int entityId, String text) {
        List<EntityData<?>> metadata = new ArrayList<>();

        // 将 Legacy 文本转换为 Adventure Component
        Component component = LegacyComponentSerializer.legacySection().deserialize(text);

        // 索引 23: Text (Component)
        metadata.add(new EntityData<>(INDEX_TEXT, EntityDataTypes.ADV_COMPONENT, component));

        // 索引 24: Line Width
        metadata.add(new EntityData<>(INDEX_LINE_WIDTH, EntityDataTypes.INT, DEFAULT_LINE_WIDTH));

        // 索引 25: Background Color
        metadata.add(new EntityData<>(INDEX_BACKGROUND_COLOR, EntityDataTypes.INT, DEFAULT_BACKGROUND_COLOR));

        // 索引 26: Text Opacity
        metadata.add(new EntityData<>(INDEX_TEXT_OPACITY, EntityDataTypes.BYTE, DEFAULT_TEXT_OPACITY));

        // 索引 27: Style flags
        metadata.add(new EntityData<>(INDEX_STYLE_FLAGS, EntityDataTypes.BYTE, DEFAULT_STYLE_FLAGS));

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    /**
     * 通过 packetevents 获取玩家对象。
     * <p>packetevents 的 PlayerManager 支持直接传入 UUID。
     */
    private static Object getPlayer(UUID playerId) {
        // packetevents 的 PlayerManager 可以通过 UUID 获取玩家
        // 但不同平台的实现不同，这里直接返回 UUID 让 PlayerManager 处理
        // 实际上 packetevents 的 sendPacket 方法支持 UUID 参数
        return playerId;
    }
}
