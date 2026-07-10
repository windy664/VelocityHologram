package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Item Display 实体工厂。
 *
 * <p>元数据索引（1.19.4+）：
 * <ul>
 *   <li>11-13: scale</li>
 *   <li>22: Item (ItemStack)</li>
 *   <li>23: ItemDisplayTransform (Byte)</li>
 *   <li>27: Billboard (Byte)</li>
 * </ul>
 */
public class ItemDisplayFactory implements DisplayEntityFactory {

    private static final int INDEX_SCALE = 11;
    private static final int INDEX_BILLBOARD = 15;
    private static final int INDEX_ITEM = 23;
    private static final int INDEX_ITEM_DISPLAY_TRANSFORM = 24;

    private static final byte TRANSFORM_FIXED = 8;

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ITEM_DISPLAY,
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

        ItemStack itemStack = resolveItem(config.itemId());
        metadata.add(new EntityData<>(INDEX_ITEM, EntityDataTypes.ITEMSTACK, itemStack));
        metadata.add(new EntityData<>(INDEX_ITEM_DISPLAY_TRANSFORM, EntityDataTypes.BYTE, TRANSFORM_FIXED));
        metadata.add(new EntityData<>(INDEX_BILLBOARD, EntityDataTypes.BYTE, (byte) config.billboard().id));

        if (config.scaleX() != 1f || config.scaleY() != 1f || config.scaleZ() != 1f) {
            metadata.add(new EntityData<>(INDEX_SCALE, EntityDataTypes.VECTOR3F,
                    new com.github.retrooper.packetevents.util.Vector3f(
                            config.scaleX(), config.scaleY(), config.scaleZ())));
        }

        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    private ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        String fullId = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        try {
            var itemType = ItemTypes.getByName(fullId);
            if (itemType != null) return ItemStack.builder().type(itemType).amount(1).build();
        } catch (Exception ignored) {
        }
        return ItemStack.builder().type(ItemTypes.STONE).amount(1).build();
    }
}
