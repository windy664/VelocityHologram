package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Armor Stand 实体工厂（用于 1.19.3 及更早版本）。
 * <p>使用不可见的 Armor Stand + 自定义名称来模拟悬浮字。
 *
 * <p>元数据索引（1.8 - 1.19.3）：
 * <ul>
 *   <li>0: Entity flags (Byte)</li>
 *   <li>2: Custom name (String)</li>
 *   <li>3: Custom name visible (Boolean)</li>
 *   <li>14: Armor stand flags (Byte)</li>
 *   <li>15: Head rotation (Vector3f) - 仅 1.9+</li>
 * </ul>
 */
public class ArmorStandFactory implements DisplayEntityFactory {

    // Armor Stand 元数据索引
    private static final int INDEX_ENTITY_FLAGS = 0;
    private static final int INDEX_CUSTOM_NAME = 2;
    private static final int INDEX_CUSTOM_NAME_VISIBLE = 3;
    private static final int INDEX_ARMOR_STAND_FLAGS = 14;

    // Entity flags
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_GLOWING = 0x40;

    // Armor Stand flags
    private static final byte AS_FLAG_SMALL = 0x01;
    private static final byte AS_FLAG_NO_BASE_PLATE = 0x08;
    private static final byte AS_FLAG_NO_GRAVITY = 0x02;

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        ClientVersion version = getPlayerVersion(player);
        boolean isSmall = config.type() == DisplayEntityType.SMALLHEAD;

        // 生成 Armor Stand
        WrapperPlayServerSpawnLivingEntity spawn = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ARMOR_STAND,
                new Vector3d(x, y - 1.5, z), // Armor Stand 需要偏移
                0f, 0f, 0f,
                new Vector3d(0, 0, 0),
                new ArrayList<>()
        );

        // 设置元数据
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config, version, isSmall);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    @Override
    public void despawn(Object player, int entityId) {
        if (player == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
    }

    @Override
    public void updateMetadata(Object player, int entityId, DisplayConfig config) {
        if (player == null) return;
        ClientVersion version = getPlayerVersion(player);
        boolean isSmall = config.type() == DisplayEntityType.SMALLHEAD;
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config, version, isSmall);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    /**
     * 创建 Armor Stand 元数据。
     */
    private WrapperPlayServerEntityMetadata createMetadata(int entityId, DisplayConfig config,
                                                            ClientVersion version, boolean isSmall) {
        List<EntityData<?>> metadata = new ArrayList<>();

        // 设置不可见
        byte entityFlags = FLAG_INVISIBLE;
        metadata.add(new EntityData<>(INDEX_ENTITY_FLAGS, EntityDataTypes.BYTE, entityFlags));

        // 设置自定义名称
        String text = config.text() != null ? config.text() : "";
        Component component = LegacyComponentSerializer.legacySection().deserialize(text);

        // 1.9+ 使用 Component，1.8 使用 String
        if (version.isNewerThanOrEquals(ClientVersion.V_1_9)) {
            metadata.add(new EntityData<>(INDEX_CUSTOM_NAME, EntityDataTypes.ADV_COMPONENT, component));
        } else {
            metadata.add(new EntityData<>(INDEX_CUSTOM_NAME, EntityDataTypes.STRING, text));
        }

        // 显示自定义名称
        metadata.add(new EntityData<>(INDEX_CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, true));

        // Armor Stand 标志
        byte asFlags = AS_FLAG_NO_BASE_PLATE | AS_FLAG_NO_GRAVITY;
        if (isSmall) {
            asFlags |= AS_FLAG_SMALL;
        }
        metadata.add(new EntityData<>(INDEX_ARMOR_STAND_FLAGS, EntityDataTypes.BYTE, asFlags));

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    /**
     * 获取玩家客户端版本。
     */
    private ClientVersion getPlayerVersion(Object player) {
        try {
            var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) {
                return user.getClientVersion();
            }
        } catch (Exception ignored) {
        }
        return ClientVersion.V_1_8; // 默认老版本
    }
}
