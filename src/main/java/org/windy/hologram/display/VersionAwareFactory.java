package org.windy.hologram.display;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;

/**
 * 版本感知的 Display 工厂。
 * <p>根据玩家客户端版本自动选择：
 * <ul>
 *   <li>1.19.3 及更早：使用 Armor Stand 模拟</li>
 *   <li>1.19.4+：使用 Display 实体</li>
 * </ul>
 */
public class VersionAwareFactory implements DisplayEntityFactory {

    private final TextDisplayFactory textFactory = new TextDisplayFactory();
    private final ItemDisplayFactory itemFactory = new ItemDisplayFactory();
    private final BlockDisplayFactory blockFactory = new BlockDisplayFactory();
    private final EntityFactory entityFactory = new EntityFactory();
    private final HeadFactory headFactory = new HeadFactory(false);
    private final SmallHeadFactory smallHeadFactory = new SmallHeadFactory();
    private final IconFactory iconFactory = new IconFactory();
    private final ArmorStandFactory armorStandFactory = new ArmorStandFactory();

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        if (player == null) return;

        ClientVersion version = getPlayerVersion(player);

        // 1.19.3 及更早：使用 Armor Stand
        if (!VersionMapper.isDisplaySupported(version)) {
            armorStandFactory.spawn(player, entityId, x, y, z, config);
            return;
        }

        // 1.19.4+：根据类型选择工厂
        DisplayEntityFactory factory = getFactoryForType(config.type());
        if (factory != null) {
            factory.spawn(player, entityId, x, y, z, config);
        }
    }

    @Override
    public void despawn(Object player, int entityId) {
        if (player == null) return;

        // 两种方式都尝试删除（因为不知道当初是用哪个生成的）
        textFactory.despawn(player, entityId);
        armorStandFactory.despawn(player, entityId);
    }

    @Override
    public void updateMetadata(Object player, int entityId, DisplayConfig config) {
        if (player == null) return;

        ClientVersion version = getPlayerVersion(player);

        // 1.19.3 及更早：使用 Armor Stand
        if (!VersionMapper.isDisplaySupported(version)) {
            armorStandFactory.updateMetadata(player, entityId, config);
            return;
        }

        // 1.19.4+：根据类型选择工厂
        DisplayEntityFactory factory = getFactoryForType(config.type());
        if (factory != null) {
            factory.updateMetadata(player, entityId, config);
        }
    }

    /**
     * 根据类型获取工厂。
     */
    private DisplayEntityFactory getFactoryForType(DisplayEntityType type) {
        switch (type) {
            case TEXT_DISPLAY: return textFactory;
            case ITEM_DISPLAY: return itemFactory;
            case BLOCK_DISPLAY: return blockFactory;
            case ENTITY: return entityFactory;
            case HEAD: return headFactory;
            case SMALLHEAD: return smallHeadFactory;
            case ICON: return iconFactory;
            default: return textFactory;
        }
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
     * SmallHead 工厂（内部类）。
     */
    private static class SmallHeadFactory implements DisplayEntityFactory {
        private final HeadFactory headFactory = new HeadFactory(true);

        @Override
        public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
            headFactory.spawn(player, entityId, x, y, z, config);
        }

        @Override
        public void despawn(Object player, int entityId) {
            headFactory.despawn(player, entityId);
        }

        @Override
        public void updateMetadata(Object player, int entityId, DisplayConfig config) {
            headFactory.updateMetadata(player, entityId, config);
        }
    }
}
