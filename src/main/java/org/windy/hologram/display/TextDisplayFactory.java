package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;

/**
 * Text Display 实体工厂。
 * <p>用 packetevents 构造 Text Display 实体的生成、销毁、元数据更新包。
 */
public class TextDisplayFactory {

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

        // 设置元数据（文本内容）
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
     * <p>Text Display 的元数据索引：
     * - 23: Text (Component)
     * - 24: Line Width (Int)
     * - 25: Background Color (Int)
     * - 26: Text Opacity (Byte)
     * - 27: Style flags (Byte)
     */
    private static WrapperPlayServerEntityMetadata createMetadata(int entityId, String text) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(text);

        // 构造元数据条目
        // Text Display 的 text 字段索引是 23（在 1.19.4+ 中）
        // 这里需要根据具体 MC 版本调整
        // 简化实现：直接构造 metadata 包

        // TODO: 正确构造 EntityMetadata 包需要更多 packetevents API 细节
        // 目前先返回一个空的 metadata 包，后续完善
        return new WrapperPlayServerEntityMetadata(entityId, new java.util.ArrayList<>());
    }

    /**
     * 通过 packetevents 获取玩家对象。
     */
    private static Object getPlayer(UUID playerId) {
        // packetevents 的 PlayerManager 可以通过 UUID 获取玩家
        // 但不同平台的获取方式不同，这里用反射兼容
        try {
            // Velocity: proxy.getPlayer(uuid).orElse(null)
            // Bukkit: Bukkit.getPlayer(uuid)
            // 统一通过 packetevents 的 PlayerManager
            // TODO: 实现平台无关的玩家获取
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
