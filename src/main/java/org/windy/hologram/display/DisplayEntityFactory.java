package org.windy.hologram.display;

import java.util.UUID;

/**
 * Display 实体工厂接口。
 * <p>每种 Display 类型（Text/Item/Block）实现此接口。
 * <p>通过 {@link DisplayFactoryRegistry} 注册和获取。
 */
public interface DisplayEntityFactory {

    /**
     * 向玩家发送实体生成包。
     *
     * @param player   目标玩家对象（Velocity Player / BungeeCord ProxiedPlayer）
     * @param entityId 实体 ID
     * @param x        X 坐标
     * @param y        Y 坐标
     * @param z        Z 坐标
     * @param config   显示配置
     */
    void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config);

    /**
     * 向玩家发送实体销毁包。
     *
     * @param player   目标玩家对象
     * @param entityId 实体 ID
     */
    void despawn(Object player, int entityId);

    /**
     * 向玩家发送实体元数据更新包。
     *
     * @param player   目标玩家对象
     * @param entityId 实体 ID
     * @param config   新的显示配置
     */
    void updateMetadata(Object player, int entityId, DisplayConfig config);
}
