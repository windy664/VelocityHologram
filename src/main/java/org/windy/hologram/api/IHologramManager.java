package org.windy.hologram.api;

import java.util.Collection;
import java.util.UUID;

/**
 * 悬浮字管理器公共 API 接口。
 */
public interface IHologramManager {

    /**
     * 创建悬浮字。
     */
    IHologram createHologram(String name, double x, double y, double z,
                             String dimension, String server);

    /**
     * 获取悬浮字。
     */
    IHologram getHologram(String name);

    /**
     * 获取所有悬浮字。
     */
    Collection<IHologram> getAllHolograms();

    /**
     * 删除悬浮字。
     */
    void removeHologram(String name);

    /**
     * 重命名悬浮字。
     */
    boolean renameHologram(String oldName, String newName);

    /**
     * 玩家断开连接时清理。
     */
    void onPlayerDisconnect(UUID playerId);

    /**
     * 玩家切换服务器时清理。
     */
    void onServerSwitch(UUID playerId);
}
