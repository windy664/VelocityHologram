package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
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
 * <p>根据玩家客户端版本动态选择正确的元数据索引。
 */
public class TextDisplayFactory implements DisplayEntityFactory {

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(x, y, z),
                0f, 0f, 0f,
                0,
                Optional.empty()
        );

        ClientVersion version = getPlayerVersion(player);
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config, version);

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
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config, version);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    /**
     * 获取玩家客户端版本。
     */
    private ClientVersion getPlayerVersion(Object player) {
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) {
                return user.getClientVersion();
            }
        } catch (Exception ignored) {
        }
        return ClientVersion.V_1_21; // 默认最新版本
    }

    /**
     * 创建元数据包（根据版本动态选择索引）。
     */
    private WrapperPlayServerEntityMetadata createMetadata(int entityId, DisplayConfig config, ClientVersion version) {
        VersionMapper.MetadataIndices indices = VersionMapper.getIndices(version);
        List<EntityData<?>> metadata = new ArrayList<>();

        // 文本内容
        Component component = LegacyComponentSerializer.legacySection().deserialize(
                config.text() != null ? config.text() : "");
        metadata.add(new EntityData<>(indices.text, EntityDataTypes.ADV_COMPONENT, component));

        // 行宽
        metadata.add(new EntityData<>(indices.lineWidth, EntityDataTypes.INT, config.lineWidth()));

        // 背景颜色
        metadata.add(new EntityData<>(indices.backgroundColor, EntityDataTypes.INT, config.backgroundColor()));

        // 文本透明度
        metadata.add(new EntityData<>(indices.textOpacity, EntityDataTypes.BYTE, config.textOpacity()));

        // 样式标志
        metadata.add(new EntityData<>(indices.styleFlags, EntityDataTypes.BYTE, config.styleFlags()));

        // Billboard 模式（3=CENTER，始终面向玩家）
        metadata.add(new EntityData<>(indices.billboard, EntityDataTypes.BYTE, (byte) config.billboard().id));

        // 缩放
        if (config.scaleX() != 1f || config.scaleY() != 1f || config.scaleZ() != 1f) {
            metadata.add(new EntityData<>(indices.scale, EntityDataTypes.VECTOR3F,
                    new com.github.retrooper.packetevents.util.Vector3f(
                            config.scaleX(), config.scaleY(), config.scaleZ())));
        }

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }
}
