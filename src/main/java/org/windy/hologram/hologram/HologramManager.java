package org.windy.hologram.hologram;

import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.api.IHologram;
import org.windy.hologram.placeholder.PlaceholderManager;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 悬浮字管理器。
 * <p>管理所有悬浮字的生命周期，协调 PlayerTracker 进行可见性计算。
 * <p>支持动画更新和空间分区优化。
 */
public class HologramManager {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Hologram>> byServer = new ConcurrentHashMap<>();
    private final PlayerTracker playerTracker;
    private final ClickHandler clickHandler;
    private final PlaceholderManager placeholderManager;

    // 动画更新计数器
    private int animationTickCounter = 0;
    private static final int ANIMATION_UPDATE_INTERVAL = 2; // 每 2 tick 更新一次动画

    public HologramManager(PlayerTracker playerTracker, ClickHandler clickHandler,
                           PlaceholderManager placeholderManager) {
        this.playerTracker = playerTracker;
        this.clickHandler = clickHandler;
        this.placeholderManager = placeholderManager;
    }

    /**
     * 创建悬浮字。
     */
    public Hologram createHologram(String name, double x, double y, double z,
                                    String dimension, String server) {
        Hologram hologram = new Hologram(name,
                new IHologram.HologramPos(x, y, z, dimension, server),
                clickHandler, placeholderManager);
        holograms.put(name, hologram);

        // 按服务器分组
        byServer.computeIfAbsent(server, k -> new ConcurrentHashMap<>())
                .put(name, hologram);

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
     * 获取指定服务器的悬浮字。
     */
    public Collection<Hologram> getHologramsByServer(String server) {
        Map<String, Hologram> serverHolograms = byServer.get(server);
        return serverHolograms != null ? serverHolograms.values() : java.util.Collections.emptyList();
    }

    /**
     * 删除悬浮字。
     */
    public void removeHologram(String name) {
        Hologram hologram = holograms.remove(name);
        if (hologram != null) {
            hologram.destroy();
            // 从服务器分组中移除
            Map<String, Hologram> serverHolograms = byServer.get(hologram.getPosition().server());
            if (serverHolograms != null) {
                serverHolograms.remove(name);
            }
        }
    }

    /**
     * 定时调用：更新所有悬浮字的可见性。
     * <p>优化：只计算同一服务器内的悬浮字。
     */
    public void tickVisibility() {
        for (Map.Entry<UUID, PlayerState> entry : playerTracker.getAllStates().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerState state = entry.getValue();

            // 获取玩家当前服务器的悬浮字
            Map<String, Hologram> serverHolograms = byServer.get(state.getServer());
            if (serverHolograms == null) {
                // 该服务器没有悬浮字，隐藏所有
                for (Hologram hologram : holograms.values()) {
                    hologram.hideFrom(playerId);
                }
                continue;
            }

            // 计算该服务器内的悬浮字可见性
            for (Hologram hologram : serverHolograms.values()) {
                IHologram.HologramPos pos = hologram.getPosition();

                // 检查维度
                if (!pos.dimension().equals(state.getDimension())) {
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

                // 视距检查
                double viewDistSq = hologram.getViewDistance() * hologram.getViewDistance();
                if (distanceSq <= viewDistSq) {
                    if (!hologram.isObserver(playerId)) {
                        hologram.showTo(playerId);
                    }
                } else {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                }
            }

            // 隐藏其他服务器的悬浮字
            for (Map.Entry<String, Map<String, Hologram>> serverEntry : byServer.entrySet()) {
                if (serverEntry.getKey().equals(state.getServer())) continue;
                for (Hologram hologram : serverEntry.getValue().values()) {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                }
            }
        }
    }

    /**
     * 定时调用：更新动画。
     * <p>比可见性更新更频繁（每 2 tick）。
     */
    public void tickAnimation() {
        animationTickCounter++;
        if (animationTickCounter >= ANIMATION_UPDATE_INTERVAL) {
            animationTickCounter = 0;

            for (Hologram hologram : holograms.values()) {
                hologram.tickAnimation();
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
