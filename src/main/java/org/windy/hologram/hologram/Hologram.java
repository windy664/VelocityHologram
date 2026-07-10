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
    private double viewDistance = 48.0;      // 显示范围（进入此范围才显示）
    private double updateDistance = 48.0;    // 更新范围（在此范围内才接收更新）
    private double lineSpacing = 0.3;
    private int updateInterval = 20;         // 更新间隔（tick，1秒=20tick）
    private String permission;
    private int editPageIndex = 0;
    private java.util.Set<String> flags = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

    /**
     * 设置悬浮字朝向。
     */
    public void setFacing(float yaw, float pitch) {
        // 朝向暂不持久化，仅用于对齐计算
    }

    // ===== 启用/禁用 =====

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

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
}
