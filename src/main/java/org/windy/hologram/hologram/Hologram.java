package org.windy.hologram.hologram;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.animation.AnimationParser;
import org.windy.hologram.animation.GradientParser;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.api.IHologram;
import org.windy.hologram.api.IHologramLine;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityFactory;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.placeholder.PlaceholderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 悬浮字核心实现。
 * <p>支持多页，每页一组行。玩家可独立翻页。
 */
public class Hologram implements IHologram {

    private final UUID id;
    private String name;
    private HologramPos position;
    private final List<Page> pages = new ArrayList<>();
    private final Set<UUID> observers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();

    // 外部依赖
    private final ClickHandler clickHandler;
    private final PlaceholderManager placeholderManager;
    private final DisplayFactoryRegistry displayRegistry;
    private final Function<UUID, Object> playerResolver;

    // 配置
    private boolean enabled = true;
    private DisableCause disableCause = DisableCause.NONE;
    private double viewDistance = 48.0;      // 显示范围（进入此范围才显示）
    private double updateDistance = 48.0;    // 更新范围（在此范围内才接收更新）
    private double lineSpacing = 0.3;
    private int updateInterval = 20;         // 更新间隔（tick，1秒=20tick）
    private String permission;
    private int editPageIndex = 0;
    private java.util.Set<String> flags = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // 朝向
    private float facingYaw = 0;
    private float facingPitch = 0;

    // 生长原点和面向
    private boolean downOrigin = false;       // 向下生长（行从上往下排列）
    private boolean alwaysFacePlayer = false; // 始终面向玩家

    // 每玩家可见性控制
    private final Set<UUID> hidePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> showPlayers = ConcurrentHashMap.newKeySet();

    public Hologram(String name, HologramPos position, ClickHandler clickHandler,
                    PlaceholderManager placeholderManager, DisplayFactoryRegistry displayRegistry,
                    Function<UUID, Object> playerResolver) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.position = position;
        this.clickHandler = clickHandler;
        this.placeholderManager = placeholderManager;
        this.displayRegistry = displayRegistry;
        this.playerResolver = playerResolver;
        // 默认创建第一页
        pages.add(new Page(0));
    }

    @Override public UUID getId() { return id; }
    @Override public String getName() { return name; }
    @Override public HologramPos getPosition() { return position; }

    /**
     * 重命名悬浮字。
     */
    public void setName(String name) { this.name = name; }

    /**
     * 更新悬浮字位置（用于 move 命令）。
     */
    public void setPosition(double x, double y, double z) {
        this.position = new HologramPos(x, y, z, position.dimension(), position.server());
    }

    // ===== 启用/禁用 =====

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) this.disableCause = DisableCause.NONE;
    }

    public void disable(DisableCause cause) {
        this.enabled = false;
        this.disableCause = cause;
    }

    public DisableCause getDisableCause() { return disableCause; }

    // ===== 生长原点和面向 =====

    public boolean isDownOrigin() { return downOrigin; }
    public void setDownOrigin(boolean downOrigin) { this.downOrigin = downOrigin; }

    public boolean isAlwaysFacePlayer() { return alwaysFacePlayer; }
    public void setAlwaysFacePlayer(boolean alwaysFacePlayer) { this.alwaysFacePlayer = alwaysFacePlayer; }

    // ===== 每玩家可见性控制 =====

    public void setHidePlayer(UUID playerId) { hidePlayers.add(playerId); }
    public void removeHidePlayer(UUID playerId) { hidePlayers.remove(playerId); }
    public boolean isHideState(UUID playerId) { return hidePlayers.contains(playerId); }

    public void setShowPlayer(UUID playerId) { showPlayers.add(playerId); }
    public void removeShowPlayer(UUID playerId) { showPlayers.remove(playerId); }
    public boolean isShowState(UUID playerId) { return showPlayers.contains(playerId); }

    // ===== 朝向 =====

    public float getFacingYaw() { return facingYaw; }
    public float getFacingPitch() { return facingPitch; }
    public void setFacing(float yaw, float pitch) {
        this.facingYaw = yaw;
        this.facingPitch = pitch;
    }

    public double getViewDistance() { return viewDistance; }
    public void setViewDistance(double viewDistance) { this.viewDistance = viewDistance; }

    public double getUpdateDistance() { return updateDistance; }
    public void setUpdateDistance(double updateDistance) { this.updateDistance = updateDistance; }

    public double getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(double lineSpacing) { this.lineSpacing = lineSpacing; }

    public int getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(int updateInterval) { this.updateInterval = updateInterval; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    // ===== Flag 系统 =====

    public java.util.Set<String> getFlags() { return flags; }
    public void addFlag(String flag) { flags.add(flag.toLowerCase()); }
    public void removeFlag(String flag) { flags.remove(flag.toLowerCase()); }
    public boolean hasFlag(String flag) { return flags.contains(flag.toLowerCase()); }

    // ===== 页管理 =====

    public int getPageCount() { return pages.size(); }

    public Page getPage(int index) {
        if (index >= 0 && index < pages.size()) return pages.get(index);
        return null;
    }

    public Page getCurrentEditPage() {
        return getPage(editPageIndex);
    }

    public int getEditPageIndex() { return editPageIndex; }

    public void setEditPageIndex(int index) {
        if (index >= 0 && index < pages.size()) {
            this.editPageIndex = index;
        }
    }

    /**
     * 添加新页。
     */
    public Page addPage() {
        Page page = new Page(pages.size());
        pages.add(page);
        return page;
    }

    /**
     * 在指定位置插入新页。
     */
    public Page insertPage(int index) {
        if (index < 0 || index > pages.size()) return null;
        Page page = new Page(index);
        pages.add(index, page);
        // 重建索引
        for (int i = 0; i < pages.size(); i++) {
            // Page 的 pageIndex 是构造时固定的
        }
        // 调整编辑页码
        if (editPageIndex >= index) editPageIndex++;
        // 调整玩家页码
        playerPages.replaceAll((pid, pageIdx) -> pageIdx >= index ? pageIdx + 1 : pageIdx);
        return page;
    }

    /**
     * 交换两页。
     */
    public boolean swapPages(int a, int b) {
        if (a < 0 || a >= pages.size() || b < 0 || b >= pages.size()) return false;
        Page tmp = pages.get(a);
        pages.set(a, pages.get(b));
        pages.set(b, tmp);
        // 调整编辑页码
        if (editPageIndex == a) editPageIndex = b;
        else if (editPageIndex == b) editPageIndex = a;
        // 调整玩家页码
        playerPages.replaceAll((pid, pageIdx) -> {
            if (pageIdx == a) return b;
            if (pageIdx == b) return a;
            return pageIdx;
        });
        return true;
    }

    /**
     * 删除指定页。
     */
    public boolean removePage(int index) {
        if (index < 0 || index >= pages.size() || pages.size() <= 1) return false;
        Page removed = pages.remove(index);
        removed.unregisterClickActions(clickHandler);
        // 重建索引
        for (int i = 0; i < pages.size(); i++) {
            // Page 的 pageIndex 是构造时固定的，这里不需要改
        }
        // 调整编辑页码
        if (editPageIndex >= pages.size()) editPageIndex = pages.size() - 1;
        // 调整玩家页码
        playerPages.replaceAll((pid, page) -> Math.min(page, pages.size() - 1));
        return true;
    }

    /**
     * 获取玩家当前页码。
     */
    public int getPlayerPage(UUID playerId) {
        return playerPages.getOrDefault(playerId, 0);
    }

    /**
     * 设置玩家页码。
     */
    public void setPlayerPage(UUID playerId, int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            playerPages.put(playerId, pageIndex);
        }
    }

    /**
     * 玩家翻到下一页。
     */
    public boolean nextPage(UUID playerId) {
        int current = getPlayerPage(playerId);
        if (current < pages.size() - 1) {
            switchPage(playerId, current + 1);
            return true;
        }
        return false;
    }

    /**
     * 玩家翻到上一页。
     */
    public boolean prevPage(UUID playerId) {
        int current = getPlayerPage(playerId);
        if (current > 0) {
            switchPage(playerId, current - 1);
            return true;
        }
        return false;
    }

    /**
     * 切换玩家到指定页（despawn 旧页 → spawn 新页）。
     */
    public void switchPage(UUID playerId, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) return;
        if (!observers.contains(playerId)) return;

        int oldPage = getPlayerPage(playerId);
        if (oldPage == pageIndex) return;

        // despawn 旧页
        Page old = getPage(oldPage);
        if (old != null) old.hideFrom(playerId, displayRegistry, playerResolver);

        // spawn 新页
        playerPages.put(playerId, pageIndex);
        Page page = pages.get(pageIndex);
        if (page != null) {
            page.showTo(playerId, position.x(), position.y(), position.z(),
                    lineSpacing, displayRegistry, placeholderManager, playerResolver);
        }
    }

    // ===== 兼容旧 API：操作当前编辑页 =====

    @Override
    public List<IHologramLine> getLines() {
        Page page = getCurrentEditPage();
        return page != null ? Collections.unmodifiableList(page.getLines()) : Collections.emptyList();
    }

    public HologramLine getLine(int index) {
        Page page = getCurrentEditPage();
        return page != null ? page.getLine(index) : null;
    }

    public int getLineCount() {
        Page page = getCurrentEditPage();
        return page != null ? page.getLineCount() : 0;
    }

    @Override
    public void addLine(String text) {
        Page page = getCurrentEditPage();
        if (page != null) page.addLine(text);
    }

    public void addLine(DisplayConfig config) {
        Page page = getCurrentEditPage();
        if (page != null) page.addLine(config);
    }

    @Override
    public void setLine(int index, String text) {
        Page page = getCurrentEditPage();
        if (page != null) page.setLine(index, text);
    }

    @Override
    public void removeLine(int index) {
        Page page = getCurrentEditPage();
        if (page == null) return;
        HologramLine removed = page.removeLine(index);
        if (removed != null && clickHandler != null) {
            clickHandler.unregisterClickAction(removed.getEntityId());
            // 对所有观察者销毁该行
            for (UUID playerId : observers) {
                DisplayEntityFactory factory = displayRegistry.getOrNull(removed.getEntityType());
                Object player = playerResolver.apply(playerId);
                if (factory != null && player != null) {
                    factory.despawn(player, removed.getEntityId());
                }
            }
        }
    }

    /**
     * 在指定位置插入行。
     */
    public boolean insertLine(int index, String text) {
        Page page = getCurrentEditPage();
        return page != null && page.insertLine(index, text) != null;
    }

    /**
     * 交换两行。
     */
    public boolean swapLines(int a, int b) {
        Page page = getCurrentEditPage();
        return page != null && page.swapLines(a, b);
    }

    public void setLineAction(int index, Action leftClick, Action rightClick) {
        Page page = getCurrentEditPage();
        if (page != null) page.setLineAction(index, leftClick, rightClick, clickHandler);
    }

    // ===== 显示控制 =====

    @Override
    public void showTo(UUID playerId) {
        if (observers.add(playerId)) {
            int pageIndex = getPlayerPage(playerId);
            Page page = getPage(pageIndex);
            if (page != null) {
                page.showTo(playerId, position.x(), position.y(), position.z(),
                        lineSpacing, displayRegistry, placeholderManager, playerResolver);
            }
        }
    }

    @Override
    public void hideFrom(UUID playerId) {
        if (observers.remove(playerId)) {
            // despawn 所有页（防止翻页后残留）
            for (Page page : pages) {
                page.hideFrom(playerId, displayRegistry, playerResolver);
            }
            playerPages.remove(playerId);
        }
    }

    @Override
    public void update() {
        for (UUID playerId : observers) {
            int pageIndex = getPlayerPage(playerId);
            Page page = getPage(pageIndex);
            if (page != null) {
                page.update(playerId, position.x(), position.y(), position.z(),
                        lineSpacing, displayRegistry, placeholderManager, playerResolver);
            }
        }
    }

    public boolean tickAnimation() {
        boolean changed = false;
        for (Page page : pages) {
            if (page.tickAnimation()) changed = true;
        }
        if (changed) update();
        return changed;
    }

    /**
     * 刷新显示（despawn 全页再 spawn）。
     * <p>用于 addline/removeLine 等结构性变更。
     */
    public void refresh() {
        for (UUID playerId : observers) {
            int pageIndex = getPlayerPage(playerId);
            Page page = getPage(pageIndex);
            if (page != null) {
                page.hideFrom(playerId, displayRegistry, playerResolver);
                page.showTo(playerId, position.x(), position.y(), position.z(),
                        lineSpacing, displayRegistry, placeholderManager, playerResolver);
            }
        }
    }

    @Override
    public void destroy() {
        for (UUID playerId : observers) {
            for (Page page : pages) {
                page.hideFrom(playerId, displayRegistry, playerResolver);
            }
        }
        for (Page page : pages) {
            page.unregisterClickActions(clickHandler);
        }
        observers.clear();
        playerPages.clear();
    }

    public Set<UUID> getObservers() { return observers; }
    public boolean isObserver(UUID playerId) { return observers.contains(playerId); }

    /**
     * 检查实体 ID 属于哪个页（用于点击处理）。
     */
    public int findPageByEntityId(int entityId) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).containsEntity(entityId)) return i;
        }
        return -1;
    }

    // ===== 批量操作（对标 DecentHolograms） =====

    /**
     * 获取页面数量。
     */
    public int size() { return pages.size(); }

    /**
     * 检查可见状态（是否有观察者）。
     */
    public boolean isVisibleState() { return !observers.isEmpty(); }

    /**
     * 获取指定页的观看者。
     */
    public Set<UUID> getViewerPlayers(int pageIndex) {
        return observers.stream()
                .filter(uuid -> getPlayerPage(uuid) == pageIndex)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 获取所有观看者。
     */
    public Set<UUID> getViewerPlayers() {
        return new java.util.HashSet<>(observers);
    }

    /**
     * 显示给指定玩家（指定页）。
     */
    public boolean show(UUID playerId, int pageIndex) {
        if (!enabled) return false;
        if (isHideState(playerId)) return false;
        if (!defaultVisibleState && !isShowState(playerId)) return false;

        Page page = getPage(pageIndex);
        if (page == null || page.getLineCount() == 0) return false;

        // 先隐藏当前页
        int currentPage = getPlayerPage(playerId);
        if (currentPage != pageIndex) {
            Page oldPage = getPage(currentPage);
            if (oldPage != null) {
                oldPage.hideFrom(playerId, displayRegistry, playerResolver);
            }
        }

        // 显示新页
        playerPages.put(playerId, pageIndex);
        page.showTo(playerId, position.x(), position.y(), position.z(),
                lineSpacing, displayRegistry, placeholderManager, playerResolver);

        if (observers.add(playerId)) {
            // 新观察者
        }
        return true;
    }

    /**
     * 显示给所有在线玩家。
     * <p>注意：在代理端，需要通过 HologramManager 调用。
     */
    public void showAll() {
        // 代理端实现：遍历所有观察者
        for (UUID playerId : new java.util.HashSet<>(observers)) {
            show(playerId, getPlayerPage(playerId));
        }
    }

    /**
     * 隐藏指定玩家。
     */
    public void hide(UUID playerId) {
        if (observers.contains(playerId)) {
            int pageIndex = getPlayerPage(playerId);
            Page page = getPage(pageIndex);
            if (page != null) {
                page.hideFrom(playerId, displayRegistry, playerResolver);
            }
            observers.remove(playerId);
        }
    }

    /**
     * 隐藏所有玩家。
     */
    public void hideAll() {
        for (UUID playerId : new java.util.HashSet<>(observers)) {
            hide(playerId);
        }
    }

    /**
     * 更新指定玩家。
     */
    public void update(UUID playerId) {
        if (hasFlag("disable_updates")) return;
        if (!observers.contains(playerId)) return;
        if (isHideState(playerId)) return;

        int pageIndex = getPlayerPage(playerId);
        Page page = getPage(pageIndex);
        if (page != null) {
            page.update(playerId, position.x(), position.y(), position.z(),
                    lineSpacing, displayRegistry, placeholderManager, playerResolver);
        }
    }

    /**
     * 更新所有玩家。
     */
    public void updateAll() {
        if (hasFlag("disable_updates")) return;
        for (UUID playerId : observers) {
            update(playerId);
        }
    }

    /**
     * 更新指定玩家的动画。
     */
    public void updateAnimations(UUID playerId) {
        if (hasFlag("disable_animations")) return;
        if (!observers.contains(playerId)) return;
        if (isHideState(playerId)) return;

        int pageIndex = getPlayerPage(playerId);
        Page page = getPage(pageIndex);
        if (page != null) {
            if (page.tickAnimation()) {
                page.update(playerId, position.x(), position.y(), position.z(),
                        lineSpacing, displayRegistry, placeholderManager, playerResolver);
            }
        }
    }

    /**
     * 更新所有玩家的动画。
     */
    public void updateAnimationsAll() {
        if (hasFlag("disable_animations")) return;
        for (UUID playerId : observers) {
            updateAnimations(playerId);
        }
    }

    /**
     * 重新对齐行（用于传送后）。
     */
    public void realignLines() {
        for (Page page : pages) {
            page.realignLines();
        }
    }

    // ===== 范围检查 =====

    /**
     * 检查玩家是否在显示范围内。
     */
    public boolean isInDisplayRange(double playerX, double playerY, double playerZ) {
        double dx = position.x() - playerX;
        double dy = position.y() - playerY;
        double dz = position.z() - playerZ;
        return dx * dx + dy * dy + dz * dz <= viewDistance * viewDistance;
    }

    /**
     * 检查玩家是否在更新范围内。
     */
    public boolean isInUpdateRange(double playerX, double playerY, double playerZ) {
        double dx = position.x() - playerX;
        double dy = position.y() - playerY;
        double dz = position.z() - playerZ;
        return dx * dx + dy * dy + dz * dz <= updateDistance * updateDistance;
    }

    // ===== 点击处理 =====

    /**
     * 处理点击事件。
     */
    public boolean onClick(UUID playerId, int entityId, String clickType) {
        if (hasFlag("disable_actions")) return false;

        int pageIndex = findPageByEntityId(entityId);
        if (pageIndex < 0) return false;

        Page page = getPage(pageIndex);
        if (page == null) return false;

        // 执行页面动作
        page.executeActions(playerId, clickType);

        // 找到对应的行并执行动作
        for (HologramLine line : page.getLines()) {
            if (line.getEntityId() == entityId) {
                Action action = null;
                switch (clickType) {
                    case "left": action = line.getLeftClickAction(); break;
                    case "right": action = line.getRightClickAction(); break;
                    case "shift_left": action = line.getShiftLeftClickAction(); break;
                    case "shift_right": action = line.getShiftRightClickAction(); break;
                }
                if (action != null) {
                    action.execute(playerId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 处理玩家退出。
     */
    public void onQuit(UUID playerId) {
        hide(playerId);
        removeHidePlayer(playerId);
        removeShowPlayer(playerId);
        playerPages.remove(playerId);
    }

    /**
     * 启用悬浮字。
     */
    public void enable() {
        this.enabled = true;
        this.disableCause = DisableCause.NONE;
    }

    /**
     * 删除悬浮字。
     */
    public void delete() {
        destroy();
    }

    // ===== 默认可见状态 =====

    private boolean defaultVisibleState = true;

    public boolean isDefaultVisibleState() { return defaultVisibleState; }
    public void setDefaultVisibleState(boolean defaultVisibleState) { this.defaultVisibleState = defaultVisibleState; }

    /**
     * 检查玩家是否可见（考虑 hide/show 状态）。
     */
    public boolean canShow(UUID playerId) {
        if (isHideState(playerId)) return false;
        if (!defaultVisibleState && !isShowState(playerId)) return false;
        return true;
    }
}
