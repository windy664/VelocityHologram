package org.windy.hologram.hologram;

import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.api.IHologram;
import org.windy.hologram.api.event.EventBus;
import org.windy.hologram.api.event.HologramCreateEvent;
import org.windy.hologram.api.event.HologramDeleteEvent;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.placeholder.PlaceholderManager;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;

import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 悬浮字管理器。
 * <p>管理所有悬浮字的生命周期，协调 PlayerTracker 进行可见性计算。
 * <p>支持动画更新、空间分区优化和权限控制。
 */
public class HologramManager {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Hologram>> byServer = new ConcurrentHashMap<>();
    private final PlayerTracker playerTracker;
    private final ClickHandler clickHandler;
    private final PlaceholderManager placeholderManager;
    private final DisplayFactoryRegistry displayRegistry;
    private final Function<UUID, Object> playerResolver;
    private Logger logger;

    // 权限检查器（由插件注入）
    private PermissionChecker permissionChecker;

    // 动画更新计数器
    private int animationTickCounter = 0;
    private static final int ANIMATION_UPDATE_INTERVAL = 2;

    /**
     * 权限检查接口。
     */
    @FunctionalInterface
    public interface PermissionChecker {
        boolean hasPermission(UUID playerId, String permission);
    }

    public HologramManager(PlayerTracker playerTracker, ClickHandler clickHandler,
                           PlaceholderManager placeholderManager, DisplayFactoryRegistry displayRegistry,
                           Function<UUID, Object> playerResolver) {
        this.playerTracker = playerTracker;
        this.clickHandler = clickHandler;
        this.placeholderManager = placeholderManager;
        this.displayRegistry = displayRegistry;
        this.playerResolver = playerResolver;
    }

    public void setPermissionChecker(PermissionChecker checker) {
        this.permissionChecker = checker;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 创建悬浮字。
     */
    public Hologram createHologram(String name, double x, double y, double z,
                                    String dimension, String server) {
        Hologram hologram = new Hologram(name,
                new IHologram.HologramPos(x, y, z, dimension, server),
                clickHandler, placeholderManager, displayRegistry, playerResolver);
        holograms.put(name, hologram);

        // 按服务器分组
        byServer.computeIfAbsent(server, k -> new ConcurrentHashMap<>())
                .put(name, hologram);

        EventBus.getInstance().fire(new HologramCreateEvent(hologram));
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
            EventBus.getInstance().fire(new HologramDeleteEvent(hologram));
            hologram.destroy();
            Map<String, Hologram> serverHolograms = byServer.get(hologram.getPosition().server());
            if (serverHolograms != null) {
                serverHolograms.remove(name);
            }
        }
    }

    /**
     * 重命名悬浮字。
     *
     * @param oldName 旧名称
     * @param newName 新名称
     * @return true 如果重命名成功
     */
    public boolean renameHologram(String oldName, String newName) {
        if (holograms.containsKey(newName)) return false;
        Hologram hologram = holograms.remove(oldName);
        if (hologram == null) return false;
        hologram.setName(newName);
        holograms.put(newName, hologram);
        // 更新服务器分组
        Map<String, Hologram> serverHolograms = byServer.get(hologram.getPosition().server());
        if (serverHolograms != null) {
            serverHolograms.remove(oldName);
            serverHolograms.put(newName, hologram);
        }
        return true;
    }

    /**
     * 定时调用：更新所有悬浮字的可见性。
     */
    public void tickVisibility() {
        for (Map.Entry<UUID, PlayerState> entry : playerTracker.getAllStates().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerState state = entry.getValue();
            String playerServer = state.getServer();

            // 遍历所有悬浮字，判断可见性
            for (Hologram hologram : holograms.values()) {
                // 检查启用状态
                if (!hologram.isEnabled()) {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                    continue;
                }

                IHologram.HologramPos pos = hologram.getPosition();
                String holoServer = pos.server();

                // 服务器检查：空 server = 所有服务器可见
                boolean serverMatch = holoServer.isEmpty() || holoServer.equals(playerServer);
                if (!serverMatch) {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                    continue;
                }

                // 维度检查
                if (!pos.dimension().equals(state.getDimension())) {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                    continue;
                }

                // 权限检查
                if (!checkVisibilityPermission(playerId, hologram)) {
                    if (hologram.isObserver(playerId)) {
                        hologram.hideFrom(playerId);
                    }
                    continue;
                }

                // ALWAYS_VISIBLE flag 跳过距离检查
                if (hologram.hasFlag("always_visible")) {
                    if (!hologram.isObserver(playerId)) {
                        hologram.showTo(playerId);
                    }
                    continue;
                }

                // 距离检查
                double dx = pos.x() - state.getX();
                double dy = pos.y() - state.getY();
                double dz = pos.z() - state.getZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;

                double displayDistSq = hologram.getViewDistance() * hologram.getViewDistance();
                if (distanceSq <= displayDistSq) {
                    if (!hologram.isObserver(playerId)) {
                        hologram.showTo(playerId);
                        if (logger != null) {
                            logger.info("[VH] showTo {} → {} (dist={})", state.getName(), hologram.getName(),
                                    String.format("%.1f", Math.sqrt(distanceSq)));
                        }
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
     * 检查玩家是否有查看悬浮字的权限。
     */
    private boolean checkVisibilityPermission(UUID playerId, Hologram hologram) {
        String perm = hologram.getPermission();
        if (perm == null || perm.isEmpty()) return true;
        if (permissionChecker == null) return true;
        return permissionChecker.hasPermission(playerId, perm);
    }

    /**
     * 定时调用：更新动画。
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
            hologram.onQuit(playerId);
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

    /**
     * 获取 Display 工厂注册表。
     */
    public DisplayFactoryRegistry getDisplayRegistry() {
        return displayRegistry;
    }

    // ===== 批量操作（对标 DecentHolograms） =====

    /**
     * 更新指定悬浮字的可见性。
     */
    public void updateVisibility(Hologram hologram) {
        for (Map.Entry<UUID, PlayerState> entry : playerTracker.getAllStates().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerState state = entry.getValue();
            updateVisibility(playerId, state, hologram);
        }
    }

    /**
     * 更新指定玩家的可见性。
     */
    public void updateVisibility(UUID playerId) {
        PlayerState state = playerTracker.get(playerId);
        if (state == null) return;
        for (Hologram hologram : holograms.values()) {
            updateVisibility(playerId, state, hologram);
        }
    }

    /**
     * 更新指定玩家对特定悬浮字的可见性。
     */
    public void updateVisibility(UUID playerId, Hologram hologram) {
        PlayerState state = playerTracker.get(playerId);
        if (state == null) return;
        updateVisibility(playerId, state, hologram);
    }

    /**
     * 内部方法：更新可见性。
     */
    private void updateVisibility(UUID playerId, PlayerState state, Hologram hologram) {
        // 检查启用状态
        if (!hologram.isEnabled()) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        // 检查每玩家可见性
        if (hologram.isHideState(playerId)) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        // 检查默认可见状态
        if (!hologram.isDefaultVisibleState() && !hologram.isShowState(playerId)) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        IHologram.HologramPos pos = hologram.getPosition();
        String holoServer = pos.server();
        String playerServer = state.getServer();

        // 服务器检查
        boolean serverMatch = holoServer.isEmpty() || holoServer.equals(playerServer);
        if (!serverMatch) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        // 维度检查
        if (!pos.dimension().equals(state.getDimension())) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        // 权限检查
        if (!checkVisibilityPermission(playerId, hologram)) {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
            return;
        }

        // ALWAYS_VISIBLE flag 跳过距离检查
        if (hologram.hasFlag("always_visible")) {
            if (!hologram.isObserver(playerId)) {
                hologram.showTo(playerId);
            }
            return;
        }

        // 距离检查
        double dx = pos.x() - state.getX();
        double dy = pos.y() - state.getY();
        double dz = pos.z() - state.getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;

        double displayDistSq = hologram.getViewDistance() * hologram.getViewDistance();
        if (distanceSq <= displayDistSq) {
            if (!hologram.isObserver(playerId)) {
                hologram.showTo(playerId);
            }
        } else {
            if (hologram.isObserver(playerId)) {
                hologram.hideFrom(playerId);
            }
        }
    }

    /**
     * 显示所有悬浮字给指定玩家。
     */
    public void showAll(UUID playerId) {
        for (Hologram hologram : holograms.values()) {
            if (hologram.isEnabled() && !hologram.isHideState(playerId)) {
                hologram.showTo(playerId);
            }
        }
    }

    /**
     * 隐藏所有悬浮字给指定玩家。
     */
    public void hideAll(UUID playerId) {
        for (Hologram hologram : holograms.values()) {
            hologram.hideFrom(playerId);
        }
    }

    /**
     * 点击处理。
     */
    public boolean onClick(UUID playerId, int entityId, String clickType) {
        for (Hologram hologram : holograms.values()) {
            if (hologram.onClick(playerId, entityId, clickType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 玩家退出处理。
     */
    public void onQuit(UUID playerId) {
        for (Hologram hologram : holograms.values()) {
            hologram.onQuit(playerId);
        }
    }

    /**
     * 重新加载所有悬浮字。
     */
    public void reload() {
        for (Hologram hologram : holograms.values()) {
            hologram.destroy();
        }
        holograms.clear();
        byServer.clear();
    }

    /**
     * 销毁所有悬浮字。
     */
    public void destroy() {
        for (Hologram hologram : holograms.values()) {
            hologram.destroy();
        }
        holograms.clear();
        byServer.clear();
    }

    /**
     * 注册悬浮字。
     */
    public void registerHologram(Hologram hologram) {
        holograms.put(hologram.getName(), hologram);
        byServer.computeIfAbsent(hologram.getPosition().server(), k -> new ConcurrentHashMap<>())
                .put(hologram.getName(), hologram);
    }

    /**
     * 检查是否包含悬浮字。
     */
    public boolean containsHologram(String name) {
        return holograms.containsKey(name);
    }

    /**
     * 获取所有悬浮字名称。
     */
    public java.util.Set<String> getHologramNames() {
        return holograms.keySet();
    }

    /**
     * 生成临时悬浮字行。
     */
    public Hologram spawnTemporaryHologramLine(double x, double y, double z,
                                                String dimension, String server,
                                                String content, long durationMs) {
        String name = "temp_" + System.currentTimeMillis();
        Hologram hologram = createHologram(name, x, y, z, dimension, server);
        hologram.addLine(content);
        hologram.addFlag("always_visible");

        // 定时删除
        new Thread(() -> {
            try {
                Thread.sleep(durationMs);
                removeHologram(name);
            } catch (InterruptedException ignored) {
            }
        }).start();

        return hologram;
    }
}
