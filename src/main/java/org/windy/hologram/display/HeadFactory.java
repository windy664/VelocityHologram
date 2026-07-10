package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * #HEAD / #SMALLHEAD 实体工厂。
 * <p>用隐形 ArmorStand 的头盔槽显示物品/方块。
 *
 * <p>配置语法：
 * <pre>
 * - head: "GRASS_BLOCK"
 * - smallhead: "DIAMOND_SWORD"
 * - head: "PLAYER_HEAD"
 *   head-texture: "d0by"
 * </pre>
 *
 * <p>元数据索引：
 * <ul>
 *   <li>0: Entity flags (Byte) - bit 5 = invisible</li>
 *   <li>15: ArmorStand flags (Byte) - bit 1 = small, bit 3 = no base plate</li>
 * </ul>
 */
public class HeadFactory implements DisplayEntityFactory {

    private static final int INDEX_ENTITY_FLAGS = 0;
    private static final int INDEX_ARMOR_STAND_FLAGS = 15;

    // Entity flags
    private static final byte FLAG_INVISIBLE = 0x20;

    // ArmorStand flags
    private static final byte FLAG_SMALL = 0x01;
    private static final byte FLAG_NO_BASE_PLATE = 0x08;

    private final boolean small;

    public HeadFactory(boolean small) {
        this.small = small;
    }

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        // 生成 ArmorStand
        WrapperPlayServerSpawnLivingEntity spawn = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ARMOR_STAND,
                new Vector3d(x, y, z),
                0f, 0f, 0f,
                new Vector3d(0, 0, 0),
                new ArrayList<EntityData<?>>()
        );

        // 设置元数据：隐形 + 无底座 + (可选)小
        byte armorStandFlags = FLAG_NO_BASE_PLATE;
        if (small) armorStandFlags |= FLAG_SMALL;

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(INDEX_ENTITY_FLAGS, EntityDataTypes.BYTE, FLAG_INVISIBLE));
        metadata.add(new EntityData<>(INDEX_ARMOR_STAND_FLAGS, EntityDataTypes.BYTE, armorStandFlags));
        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);

        // 设置头盔装备
        ItemStack headItem = resolveHeadItem(config);
        Equipment equipment = new Equipment(EquipmentSlot.HELMET, headItem);
        WrapperPlayServerEntityEquipment equipPacket = new WrapperPlayServerEntityEquipment(
                entityId, Collections.singletonList(equipment));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, equipPacket);
    }

    @Override
    public void despawn(Object player, int entityId) {
        if (player == null) return;
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroy);
    }

    @Override
    public void updateMetadata(Object player, int entityId, DisplayConfig config) {
        // 头盔装备更新
        if (player == null) return;
        ItemStack headItem = resolveHeadItem(config);
        Equipment equipment = new Equipment(EquipmentSlot.HELMET, headItem);
        WrapperPlayServerEntityEquipment equipPacket = new WrapperPlayServerEntityEquipment(
                entityId, Collections.singletonList(equipment));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, equipPacket);
    }

    /**
     * 解析头盔物品。
     * <p>blockId 字段存储物品/方块 ID。
     */
    private ItemStack resolveHeadItem(DisplayConfig config) {
        String id = config.blockId();
        if (id == null || id.isEmpty()) {
            return ItemStack.builder().type(ItemTypes.STONE).amount(1).build();
        }

        String fullId = id.contains(":") ? id : "minecraft:" + id;

        // 尝试作为物品
        try {
            var itemType = ItemTypes.getByName(fullId);
            if (itemType != null) {
                return ItemStack.builder().type(itemType).amount(1).build();
            }
        } catch (Exception ignored) {
        }

        // 降级为石头
        return ItemStack.builder().type(ItemTypes.STONE).amount(1).build();
    }
}
