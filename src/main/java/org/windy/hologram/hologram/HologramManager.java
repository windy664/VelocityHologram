package org.windy.hologram.hologram;

import org.slf4j.Logger;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.api.IHologram;
import org.windy.hologram.api.event.EventBus;
import org.windy.hologram.api.event.HologramCreateEvent;
import org.windy.hologram.api.event.HologramDeleteEvent;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.placeholder.PlaceholderManager;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;
import org.windy.hologram.utils.SchedulerUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 悬浮字管理器。
 * <p>管理所有悬浮字的生命周期，协调 PlayerTracker 进行可见性计算。
 * <p>支持动画更新、服务器分组和权限控制。
 */
public class HologramManager {

    private static final int ANIMATION_UPDATE_INTERVAL = 2;

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Hologram>> byServer = new ConcurrentHashMap<>();
    private final PlayerTracker playerTracker;
    private final ClickHandler clickHandler;
    private final PlaceholderManager placeholderManager;
    private final DisplayFactoryRegistry displayRegistry;
    private final Function<UUID, Object> playerResolver;

    private PermissionChecker permissionChecker;
    private Logger logger;
    private int animationTickCounter;

    /**
     * 权限检查接口。
     */
    @FunctionalInterface
    public interface PermissionChecker {
        boolean hasPermission(UUID playerId, String permission);
    }

    public HologramManager(PlayerTracker playerTracker,
                           ClickHandler clickHandler,
                           PlaceholderManager placeholderManager,
                           DisplayFactoryRegistry displayRegistry,
                           Function<UUID, Object> playerResolver) {
        this.playerTracker = playerTracker;
        this.clickHandler = clickHandler;
        this.placeholderManager = placeholderManager;
        this.displayRegistry = displayRegistry;
        this.playerResolver = playerResolver;

        if (this.clickHandler != null) {
            this.clickHandler.setHologramManager(this);
        }
    }

    public void setPermissionChecker(PermissionChecker checker) {
        this.permissionChecker = checker;
    }

    public PermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * 创建悬浮字。
     * <p>如果存在同名悬浮字，会先完整销毁旧对象并清理索引。
     */
    public Hologram createHologram(String name,
                                    double x,
                                    double y,
                                    double z,
                                    String dimension,
                                    String server) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("悬浮字名称不能为空");
        }

        String normalizedDimension = normalizeDimension(dimension);
        String normalizedServer = server != null ? server : "";

        removeExistingWithoutEvent(name);

        Hologram hologram = new Hologram(
                name,
                new IHologram.HologramPos(
                        x,
                        y,
                        z,
                        normalizedDimension,
                        normalizedServer
                ),
                clickHandler,
                placeholderManager,
                displayRegistry,
                playerResolver
        );

        holograms.put(name, hologram);
        addToServerIndex(hologram);

        EventBus.getInstance().fire(new HologramCreateEvent(hologram));
        return hologram;
    }

    /**
     * 获取悬浮字。
     */
    public Hologram getHologram(String name) {
        if (name == null) return null;
        return holograms.get(name);
    }

    /**
     * 获取所有悬浮字。
     */
    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    /**
     * 获取指定服务器的悬浮字。
     */
    public Collection<Hologram> getHologramsByServer(String server) {
        String normalizedServer = server != null ? server : "";
        Map<String, Hologram> serverHolograms = byServer.get(normalizedServer);
        if (serverHolograms == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(serverHolograms.values());
    }

    /**
     * 删除悬浮字。
     */
    public void removeHologram(String name) {
        if (name == null) return;

        Hologram hologram = holograms.remove(name);
        if (hologram == null) return;

        removeFromServerIndex(hologram, name);
        EventBus.getInstance().fire(new HologramDeleteEvent(hologram));
        hologram.destroy();
    }

    /**
     * 重命名悬浮字。
     *
     * @param oldName 旧名称
     * @param newName 新名称
     * @return true 如果重命名成功
     */
    public boolean renameHologram(String oldName, String newName) {
        if (oldName == null || newName == null || newName.isBlank()) {
            return false;
        }
        if (oldName.equals(newName)) {
            return holograms.containsKey(oldName);
        }
        if (holograms.containsKey(newName)) {
            return false;
        }

        Hologram hologram = holograms.remove(oldName);
        if (hologram == null) {
            return false;
        }

        removeFromServerIndex(hologram, oldName);
        hologram.setName(newName);
        holograms.put(newName, hologram);
        addToServerIndex(hologram);
        return true;
    }

    /**
     * 定时调用：更新所有悬浮字的可见性。
     * <p>所有入口统一使用完整可见性规则，避免玩家级 hide/show 状态被覆盖。
     */
    public void tickVisibility() {
        for (Map.Entry<UUID, PlayerState> entry
                : playerTracker.getAllStates().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerState state = entry.getValue();

            if (playerId == null || state == null) {
                continue;
            }

            for (Hologram hologram : holograms.values()) {
                updateVisibility(playerId, state, hologram);
            }
        }
    }

    /**
     * 定时调用：更新动画。
     */
    public void tickAnimation() {
        animationTickCounter++;
        if (animationTickCounter < ANIMATION_UPDATE_INTERVAL) {
            return;
        }

        animationTickCounter = 0;
        for (Hologram hologram : holograms.values()) {
            if (!hologram.isEnabled()) {
                continue;
            }
            if (hologram.hasFlag("disable_animations")) {
                continue;
            }
            hologram.tickAnimation();
        }
    }

    /**
     * 玩家断开连接时清理。
     */
    public void onPlayerDisconnect(UUID playerId) {
        if (playerId == null) return;

        for (Hologram hologram : holograms.values()) {
            hologram.onQuit(playerId);
        }

        if (clickHandler != null) {
            clickHandler.onPlayerDisconnect(playerId);
        }
    }

    /**
     * 玩家切换服务器时清理并重新评估。
     */
    public void onServerSwitch(UUID playerId) {
        if (playerId == null) return;

        hideAll(playerId);
        updateVisibility(playerId);
    }

    /**
     * 获取 Display 工厂注册表。
     */
    public DisplayFactoryRegistry getDisplayRegistry() {
        return displayRegistry;
    }

    /**
     * 更新指定悬浮字对所有玩家的可见性。
     */
    public void updateVisibility(Hologram hologram) {
        if (hologram == null) return;

        for (Map.Entry<UUID, PlayerState> entry
                : playerTracker.getAllStates().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerState state = entry.getValue();

            if (playerId != null && state != null) {
                updateVisibility(playerId, state, hologram);
            }
        }
    }

    /**
     * 更新指定玩家对所有悬浮字的可见性。
     */
    public void updateVisibility(UUID playerId) {
        if (playerId == null) return;

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
        if (playerId == null || hologram == null) return;

        PlayerState state = playerTracker.get(playerId);
        if (state == null) {
            hologram.hideFrom(playerId);
            return;
        }

        updateVisibility(playerId, state, hologram);
    }

    /**
     * 显示所有满足可见条件的悬浮字给指定玩家。
     * <p>不会绕过服务器、维度、距离、权限和玩家级可见状态。
     */
    public void showAll(UUID playerId) {
        updateVisibility(playerId);
    }

    /**
     * 隐藏所有悬浮字给指定玩家。
     */
    public void hideAll(UUID playerId) {
        if (playerId == null) return;

        for (Hologram hologram : holograms.values()) {
            hologram.hideFrom(playerId);
        }
    }

    /**
     * 点击处理。
     */
    public boolean onClick(UUID playerId, int entityId, String clickType) {
        if (playerId == null || clickType == null) {
            return false;
        }

        for (Hologram hologram : holograms.values()) {
            if (!hologram.isEnabled()) {
                continue;
            }
            if (!hologram.isObserver(playerId)) {
                continue;
            }
            if (hologram.findPageByEntityId(entityId) < 0) {
                continue;
            }
            return hologram.onClick(
                    playerId,
                    entityId,
                    normalizeClickType(clickType)
            );
        }

        return false;
    }

    /**
     * 玩家退出处理。
     */
    public void onQuit(UUID playerId) {
        onPlayerDisconnect(playerId);
    }

    /**
     * 重新加载所有悬浮字。
     */
    public void reload() {
        destroyAllHolograms();
    }

    /**
     * 销毁所有悬浮字。
     */
    public void destroy() {
        destroyAllHolograms();
    }

    /**
     * 注册悬浮字。
     * <p>如果存在同名对象，会先销毁旧对象。
     */
    public void registerHologram(Hologram hologram) {
        if (hologram == null
                || hologram.getName() == null
                || hologram.getName().isBlank()) {
            return;
        }

        removeExistingWithoutEvent(hologram.getName());
        holograms.put(hologram.getName(), hologram);
        addToServerIndex(hologram);
    }

    /**
     * 检查是否包含悬浮字。
     */
    public boolean containsHologram(String name) {
        return name != null && holograms.containsKey(name);
    }

    /**
     * 获取所有悬浮字名称。
     */
    public Set<String> getHologramNames() {
        return Collections.unmodifiableSet(holograms.keySet());
    }

    /**
     * 生成临时悬浮字行。
     */
    public Hologram spawnTemporaryHologramLine(double x,
                                               double y,
                                               double z,
                                               String dimension,
                                               String server,
                                               String content,
                                               long durationMs) {
        String name = "temp_"
                + System.currentTimeMillis()
                + "_"
                + UUID.randomUUID().toString().substring(0, 8);

        Hologram hologram = createHologram(
                name,
                x,
                y,
                z,
                dimension,
                server
        );
        hologram.addLine(content != null ? content : "");
        hologram.addFlag("always_visible");

        long safeDuration = Math.max(0, durationMs);
        String taskId = SchedulerUtils.runDelayedMs(
                () -> removeHologram(name),
                safeDuration
        );

        if (taskId == null && logger != null) {
            logger.warn(
                    "[VelocityHologram] 临时悬浮字删除任务未能调度: {}",
                    name
            );
        }

        updateVisibility(hologram);
        return hologram;
    }

    /**
     * 内部方法：根据完整规则更新可见性。
     */
    private void updateVisibility(UUID playerId,
                                  PlayerState state,
                                  Hologram hologram) {
        boolean shouldShow = shouldShow(playerId, state, hologram);
        boolean observing = hologram.isObserver(playerId);

        if (shouldShow && !observing) {
            hologram.showTo(playerId);
            logVisibilityShow(playerId, state, hologram);
        } else if (!shouldShow && observing) {
            hologram.hideFrom(playerId);
        }
    }

    /**
     * 判断指定玩家是否应该看到悬浮字。
     */
    private boolean shouldShow(UUID playerId,
                               PlayerState state,
                               Hologram hologram) {
        if (!hologram.isEnabled()) {
            return false;
        }
        if (!hologram.canShow(playerId)) {
            return false;
        }
        if (!checkVisibilityPermission(playerId, hologram)) {
            return false;
        }

        IHologram.HologramPos position = hologram.getPosition();
        if (position == null) {
            return false;
        }

        String hologramServer = safeString(position.server());
        String playerServer = safeString(state.getServer());
        if (!hologramServer.isEmpty()
                && !hologramServer.equalsIgnoreCase(playerServer)) {
            return false;
        }

        String hologramDimension = normalizeDimension(position.dimension());
        String playerDimension = normalizeDimension(state.getDimension());
        if (!hologramDimension.equals(playerDimension)) {
            return false;
        }

        if (hologram.hasFlag("always_visible")) {
            return true;
        }

        return hologram.isInDisplayRange(
                state.getX(),
                state.getY(),
                state.getZ()
        );
    }

    /**
     * 检查玩家是否有查看悬浮字的权限。
     */
    private boolean checkVisibilityPermission(UUID playerId,
                                              Hologram hologram) {
        String permission = hologram.getPermission();
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (permissionChecker == null) {
            return true;
        }
        return permissionChecker.hasPermission(playerId, permission);
    }

    /**
     * 移除同名对象，不触发删除事件。
     */
    private void removeExistingWithoutEvent(String name) {
        Hologram existing = holograms.remove(name);
        if (existing == null) return;

        removeFromServerIndex(existing, name);
        existing.destroy();
    }

    /**
     * 添加服务器索引。
     */
    private void addToServerIndex(Hologram hologram) {
        String server = safeString(hologram.getPosition().server());
        byServer.computeIfAbsent(
                server,
                ignored -> new ConcurrentHashMap<>()
        ).put(hologram.getName(), hologram);
    }

    /**
     * 删除服务器索引，并移除空分组。
     */
    private void removeFromServerIndex(Hologram hologram, String indexedName) {
        String server = safeString(hologram.getPosition().server());
        Map<String, Hologram> serverHolograms = byServer.get(server);
        if (serverHolograms == null) return;

        serverHolograms.remove(indexedName, hologram);
        if (serverHolograms.isEmpty()) {
            byServer.remove(server, serverHolograms);
        }
    }

    /**
     * 销毁并清理所有悬浮字。
     */
    private void destroyAllHolograms() {
        for (Hologram hologram : holograms.values()) {
            hologram.destroy();
        }
        holograms.clear();
        byServer.clear();
    }

    private void logVisibilityShow(UUID playerId,
                                   PlayerState state,
                                   Hologram hologram) {
        if (logger == null) return;

        IHologram.HologramPos position = hologram.getPosition();
        double dx = position.x() - state.getX();
        double dy = position.y() - state.getY();
        double dz = position.z() - state.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        String playerName = state.getName();
        if (playerName == null || playerName.isBlank()) {
            playerName = playerId.toString();
        }

        logger.debug(
                "[VH] showTo {} -> {} (dist={})",
                playerName,
                hologram.getName(),
                String.format("%.1f", distance)
        );
    }

    private String normalizeClickType(String clickType) {
        return clickType.trim()
                .toLowerCase()
                .replace('_', '-');
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return "minecraft:overworld";
        }

        String normalized = dimension.trim().toLowerCase();
        if (normalized.contains(":")) {
            return normalized;
        }

        switch (normalized) {
            case "world":
            case "overworld":
            case "the_overworld":
                return "minecraft:overworld";
            case "world_nether":
            case "nether":
            case "the_nether":
                return "minecraft:the_nether";
            case "world_the_end":
            case "end":
            case "the_end":
                return "minecraft:the_end";
            default:
                return "minecraft:" + normalized;
        }
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }
}
