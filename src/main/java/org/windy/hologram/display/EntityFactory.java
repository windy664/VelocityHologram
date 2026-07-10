package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 实体（生物）工厂。
 * <p>用 packetevents 构造 living entity 的生成、销毁、元数据包。
 *
 * <p>配置语法：
 * <pre>
 * - entity: "PIG"
 * - entity: "AXOLOTL"
 * - entity: "ZOMBIE"
 * </pre>
 */
public class EntityFactory implements DisplayEntityFactory {

    // Living Entity 元数据索引
    private static final int INDEX_FLAGS = 0;       // Entity flags (Byte)
    private static final int INDEX_NO_GRAVITY = 5;  // No gravity (Boolean)

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        com.github.retrooper.packetevents.protocol.entity.type.EntityType entityType =
                resolveEntityType(config.blockId()); // blockId 字段复用存储实体类型名

        WrapperPlayServerSpawnLivingEntity spawn = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                entityType,
                new Vector3d(x, y, z),
                0f, 0f, 0f,
                new Vector3d(0, 0, 0),
                new java.util.ArrayList<EntityData<?>>()
        );

        // 设置无重力（悬浮不掉落）
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(INDEX_NO_GRAVITY, EntityDataTypes.BYTE, (byte) 1));
        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
    }

    @Override
    public void despawn(Object player, int entityId) {
        if (player == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
    }

    @Override
    public void updateMetadata(Object player, int entityId, DisplayConfig config) {
        // 实体通常不需要频繁更新元数据
    }

    /**
     * 解析实体类型。
     * <p>格式：minecraft:pig 或 pig
     */
    private com.github.retrooper.packetevents.protocol.entity.type.EntityType resolveEntityType(String name) {
        if (name == null || name.isEmpty()) return EntityTypes.PIG;
        String fullId = name.contains(":") ? name : "minecraft:" + name;
        try {
            var type = EntityTypes.getByName(fullId);
            if (type != null) return type;
        } catch (Exception ignored) {
        }
        return EntityTypes.PIG;
    }
}
