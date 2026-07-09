package org.windy.hologram.hologram;

import org.windy.hologram.api.IHologram;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 悬浮字管理器。
 * <p>管理所有悬浮字的生命周期，协调 PlayerTracker 进行可见性计算。
 */
public class HologramManager {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final PlayerTracker playerTracker;

    public HologramManager(PlayerTracker playerTracker) {
        this.playerTracker = playerTracker;
    }

    /**
     * 创建悬浮字。
     */
    public Hologram createHologram(String name, double x, double y, double z,
                                    String dimension, String server) {
        Hologram hologram = new Hologram(name,
                new IHologram.HologramPos(x, y, z, dimension, server));
        holograms.put(name, hologram);
        return hologram;
    }

    /**
     * 获取悬浮字。
     */
    public Hologram getHologram(String name) {
        return holograms.get(name);
    }

    /**
     * 获取所有悬浮字。
     */
    public Collection<Hologram> getAllHolograms() {
        return holograms.values();
    }

    /**
     * 删除悬浮字。
     */
    public void removeHologram(String name) {
        Hologram hologram = holograms.remove(name);
        if (hologram != null) {
            hologram.destroy();
        }
    }

    /**
     * 定时调用：更新所有悬浮字的可见性。
     * <p>由异步调度器每 500ms 调用一次。
     */
    public void tickVisibility() {
        for (Hologram hologram : holograms.values()) {
            IHologram.HologramPos pos = hologram.getPosition();

            // 遍历所有在线玩家
            for (Map.Entry<UUID, PlayerState> entry : playerTracker.getAllStates().entrySet()) {
                UUID playerId = entry.getKey();
                PlayerState state = entry.getValue();

                // 检查是否在同一服务器和维度
                if (!pos.server().equals(state.getServer()) || !pos.dimension().equals(state.getDimension())) {
                    // 不在同一世界，隐藏
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                    continue;
                }

                // 计算距离
                double dx = pos.x() - state.getX();
                double dy = pos.y() - state.getY();
                double dz = pos.z() - state.getZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;

                // 视距 48 格（48^2 = 2304）
                if (distanceSq <= 2304) {
                    if (!hologram.isObserver(playerId)) {
                        hologram.showTo(playerId);
                    }
                } else {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                }
            }
        }
    }

    /**
     * 玩家断开连接时清理。
     */
    public void onPlayerDisconnect(UUID playerId) {
        for (Hologram hologram : holograms.values()) {
            hologram.hideFrom(playerId);
        }
    }

    /**
     * 玩家切换服务器时清理并重新评估。
     */
    public void onServerSwitch(UUID playerId) {
        for (Hologram hologram : holograms.values()) {
            hologram.hideFrom(playerId);
        }
    }
}
