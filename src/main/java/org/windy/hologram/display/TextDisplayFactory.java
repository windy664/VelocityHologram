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
 *
 * <p>元数据索引（1.19.4+）：
 * <ul>
 *   <li>8-10: translation (Vector3f)</li>
 *   <li>11-13: scale (Vector3f)</li>
 *   <li>22: Text (Component)</li>
 *   <li>23: LineWidth (Int)</li>
 *   <li>24: BackgroundColor (Int ARGB)</li>
 *   <li>25: TextOpacity (Byte)</li>
 *   <li>26: StyleFlags (Byte)</li>
 *   <li>27: Billboard (Byte) — 0=FIXED, 1=VERTICAL, 2=HORIZONTAL, 3=CENTER</li>
 * </ul>
 */
public class TextDisplayFactory implements DisplayEntityFactory {

    // Display 基础
    private static final int INDEX_SCALE = 11;
    private static final int INDEX_BILLBOARD = 15;

    // TextDisplay 专属（MC 26.1.2 验证通过）
    private static final int INDEX_TEXT = 23;
    private static final int INDEX_LINE_WIDTH = 24;
    private static final int INDEX_BACKGROUND_COLOR = 25;
    private static final int INDEX_TEXT_OPACITY = 26;
    private static final int INDEX_STYLE_FLAGS = 27;

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

        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config);

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
        WrapperPlayServerEntityMetadata metadata = createMetadata(entityId, config);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadata);
    }

    private WrapperPlayServerEntityMetadata createMetadata(int entityId, DisplayConfig config) {
        List<EntityData<?>> metadata = new ArrayList<>();

        // 文本内容
        Component component = LegacyComponentSerializer.legacySection().deserialize(
                config.text() != null ? config.text() : "");
        metadata.add(new EntityData<>(INDEX_TEXT, EntityDataTypes.ADV_COMPONENT, component));

        // 行宽
        metadata.add(new EntityData<>(INDEX_LINE_WIDTH, EntityDataTypes.INT, config.lineWidth()));

        // 背景颜色
        metadata.add(new EntityData<>(INDEX_BACKGROUND_COLOR, EntityDataTypes.INT, config.backgroundColor()));

        // 文本透明度
        metadata.add(new EntityData<>(INDEX_TEXT_OPACITY, EntityDataTypes.BYTE, config.textOpacity()));

        // 样式标志
        metadata.add(new EntityData<>(INDEX_STYLE_FLAGS, EntityDataTypes.BYTE, config.styleFlags()));

        // Billboard 模式（3=CENTER，始终面向玩家）
        metadata.add(new EntityData<>(INDEX_BILLBOARD, EntityDataTypes.BYTE, (byte) config.billboard().id));

        // 缩放
        if (config.scaleX() != 1f || config.scaleY() != 1f || config.scaleZ() != 1f) {
            metadata.add(new EntityData<>(INDEX_SCALE, EntityDataTypes.VECTOR3F,
                    new com.github.retrooper.packetevents.util.Vector3f(
                            config.scaleX(), config.scaleY(), config.scaleZ())));
        }

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }
}
