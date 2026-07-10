package org.windy.hologram.hologram;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ActionChain;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.animation.AnimationParser;
import org.windy.hologram.animation.GradientParser;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityFactory;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.placeholder.PlaceholderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 悬浮字的一页。
 * <p>每页包含一组行，翻页时切换显示。
 */
public class Page {

    private final List<HologramLine> lines = new ArrayList<>();
    private final int pageIndex;

    // 页面级动作（点击页面空白区域执行）
    private final Map<String, Action> pageActions = new ConcurrentHashMap<>();

    public Page(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageIndex() { return pageIndex; }
    public int getLineCount() { return lines.size(); }

    public List<HologramLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public HologramLine getLine(int index) {
        if (index >= 0 && index < lines.size()) return lines.get(index);
        return null;
    }

    /**
     * 添加文本行。
     */
    public HologramLine addLine(String text) {
        HologramLine line = new HologramLine(lines.size(), text);
        if (AnimationParser.hasAnimation(text)) {
            line.setAnimation(AnimationParser.parse(text));
        }
        lines.add(line);
        return line;
    }

    /**
     * 添加指定类型的行。
     */
    public HologramLine addLine(DisplayConfig config) {
        HologramLine line = new HologramLine(lines.size(), config);
        if (config.type() == DisplayEntityType.TEXT_DISPLAY && config.text() != null) {
            if (AnimationParser.hasAnimation(config.text())) {
                line.setAnimation(AnimationParser.parse(config.text()));
            }
        }
        lines.add(line);
        return line;
    }

    /**
     * 设置行文本。
     */
    public void setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            HologramLine line = lines.get(index);
            line.setText(text);
            if (AnimationParser.hasAnimation(text)) {
                line.setAnimation(AnimationParser.parse(text));
            } else {
                line.setAnimation(null);
            }
        }
    }

    /**
     * 移除行。
     */
    public HologramLine removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            HologramLine removed = lines.remove(index);
            for (int i = 0; i < lines.size(); i++) {
                lines.get(i).setIndex(i);
            }
            return removed;
        }
        return null;
    }

    /**
     * 在指定位置插入行。
     */
    public HologramLine insertLine(int index, String text) {
        if (index < 0 || index > lines.size()) return null;
        HologramLine line = new HologramLine(index, text);
        if (AnimationParser.hasAnimation(text)) {
            line.setAnimation(AnimationParser.parse(text));
        }
        lines.add(index, line);
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setIndex(i);
        }
        return line;
    }

    /**
     * 交换两行。
     */
    public boolean swapLines(int a, int b) {
        if (a < 0 || a >= lines.size() || b < 0 || b >= lines.size()) return false;
        HologramLine tmp = lines.get(a);
        lines.set(a, lines.get(b));
        lines.set(b, tmp);
        lines.get(a).setIndex(a);
        lines.get(b).setIndex(b);
        return true;
    }

    /**
     * 向观察者发送本页所有行（spawn）。
     */
    public void showTo(UUID playerId, double baseX, double baseY, double baseZ,
                       double lineSpacing, DisplayFactoryRegistry displayRegistry,
                       PlaceholderManager placeholderManager, Function<UUID, Object> playerResolver) {
        for (HologramLine line : lines) {
            double y = line.getWorldY(baseY, lineSpacing);
            DisplayConfig config = resolveDisplayConfig(line, playerId, placeholderManager);
            DisplayEntityFactory factory = displayRegistry.getOrNull(line.getEntityType());
            Object player = playerResolver.apply(playerId);
            if (factory != null && player != null) {
                factory.spawn(player, line.getEntityId(), baseX, y, baseZ, config);
            }
        }
    }

    /**
     * 向观察者销毁本页所有行（despawn）。
     */
    public void hideFrom(UUID playerId, DisplayFactoryRegistry displayRegistry,
                         Function<UUID, Object> playerResolver) {
        for (HologramLine line : lines) {
            DisplayEntityFactory factory = displayRegistry.getOrNull(line.getEntityType());
            Object player = playerResolver.apply(playerId);
            if (factory != null && player != null) {
                factory.despawn(player, line.getEntityId());
            }
        }
    }

    /**
     * 更新本页所有行的元数据。
     */
    public void update(UUID playerId, double baseX, double baseY, double baseZ,
                       double lineSpacing, DisplayFactoryRegistry displayRegistry,
                       PlaceholderManager placeholderManager, Function<UUID, Object> playerResolver) {
        for (HologramLine line : lines) {
            DisplayConfig config = resolveDisplayConfig(line, playerId, placeholderManager);
            DisplayEntityFactory factory = displayRegistry.getOrNull(line.getEntityType());
            Object player = playerResolver.apply(playerId);
            if (factory != null && player != null) {
                factory.updateMetadata(player, line.getEntityId(), config);
            }
        }
    }

    /**
     * 推进动画。
     *
     * @return true 如果有帧变化
     */
    public boolean tickAnimation() {
        boolean changed = false;
        for (HologramLine line : lines) {
            if (line.tickAnimation()) changed = true;
        }
        return changed;
    }

    /**
     * 解析显示配置（占位符 + 渐变）。
     */
    private DisplayConfig resolveDisplayConfig(HologramLine line, UUID playerId,
                                               PlaceholderManager placeholderManager) {
        DisplayConfig base = line.getDisplayConfig();
        if (base.type() != DisplayEntityType.TEXT_DISPLAY) return base;

        String text = line.getAnimationText();
        if (placeholderManager != null) {
            text = placeholderManager.replace(text, playerId);
        }
        if (GradientParser.hasGradient(text)) {
            text = GradientParser.applyGradient(text);
        }
        if (text.equals(base.text())) return base;

        return DisplayConfig.builder(base.type())
                .text(text)
                .billboard(base.billboard())
                .scale(base.scaleX(), base.scaleY(), base.scaleZ())
                .backgroundColor(base.backgroundColor())
                .textOpacity(base.textOpacity())
                .styleFlags(base.styleFlags())
                .lineWidth(base.lineWidth())
                .build();
    }

    /**
     * 设置行的点击动作。
     */
    public void setLineAction(int index, Action leftClick, Action rightClick, ClickHandler clickHandler) {
        setLineAction(index, leftClick, rightClick, null, null, clickHandler);
    }

    /**
     * 设置行的点击动作（支持 4 种类型）。
     */
    public void setLineAction(int index, Action left, Action right,
                               Action shiftLeft, Action shiftRight, ClickHandler clickHandler) {
        if (index >= 0 && index < lines.size()) {
            HologramLine line = lines.get(index);
            line.setLeftClickAction(left);
            line.setRightClickAction(right);
            line.setShiftLeftClickAction(shiftLeft);
            line.setShiftRightClickAction(shiftRight);
            if (clickHandler != null) {
                // 通过 ActionChain 注册
                ActionChain lc = left != null ? new ActionChain(java.util.Collections.singletonList(left)) : null;
                ActionChain rc = right != null ? new ActionChain(java.util.Collections.singletonList(right)) : null;
                ActionChain slc = shiftLeft != null ? new ActionChain(java.util.Collections.singletonList(shiftLeft)) : null;
                ActionChain src = shiftRight != null ? new ActionChain(java.util.Collections.singletonList(shiftRight)) : null;
                clickHandler.registerClickAction(line.getEntityId(), lc, rc, slc, src);
            }
        }
    }

    /**
     * 注销本页所有点击动作。
     */
    public void unregisterClickActions(ClickHandler clickHandler) {
        if (clickHandler == null) return;
        for (HologramLine line : lines) {
            clickHandler.unregisterClickAction(line.getEntityId());
        }
    }

    // ===== 页面级动作 =====

    /**
     * 添加页面动作。
     */
    public void addAction(String clickType, Action action) {
        pageActions.put(clickType.toLowerCase(), action);
    }

    /**
     * 移除页面动作。
     */
    public void removeAction(String clickType) {
        pageActions.remove(clickType.toLowerCase());
    }

    /**
     * 获取页面动作。
     */
    public Action getAction(String clickType) {
        return pageActions.get(clickType.toLowerCase());
    }

    /**
     * 获取所有页面动作。
     */
    public Map<String, Action> getActions() {
        return Collections.unmodifiableMap(pageActions);
    }

    /**
     * 清除所有页面动作。
     */
    public void clearActions() {
        pageActions.clear();
    }

    /**
     * 清除指定类型的页面动作。
     */
    public void clearActions(String clickType) {
        pageActions.remove(clickType.toLowerCase());
    }

    /**
     * 检查是否有页面动作。
     */
    public boolean hasActions() {
        return !pageActions.isEmpty();
    }

    /**
     * 执行页面动作。
     */
    public void executeActions(UUID playerId, String clickType) {
        Action action = pageActions.get(clickType.toLowerCase());
        if (action != null) {
            action.execute(playerId);
        }
    }

    /**
     * 检查本页是否包含指定实体 ID。
     */
    public boolean containsEntity(int entityId) {
        for (HologramLine line : lines) {
            if (line.getEntityId() == entityId) return true;
        }
        return false;
    }

    /**
     * 检查是否有可点击的行。
     */
    public boolean isClickable() {
        for (HologramLine line : lines) {
            if (line.getLeftClickAction() != null || line.getRightClickAction() != null
                    || line.getShiftLeftClickAction() != null || line.getShiftRightClickAction() != null) {
                return true;
            }
        }
        return !pageActions.isEmpty();
    }

    /**
     * 获取可点击实体ID。
     */
    public int getClickableEntityId(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            return lines.get(lineIndex).getEntityId();
        }
        return -1;
    }

    /**
     * 获取页面高度。
     */
    public double getHeight(double lineSpacing) {
        if (lines.isEmpty()) return 0;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (HologramLine line : lines) {
            double y = line.getWorldY(0, lineSpacing);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return maxY - minY;
    }

    /**
     * 重新对齐行。
     */
    public void realignLines() {
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setIndex(i);
        }
    }

    /**
     * 获取页面中心位置。
     */
    public double[] getCenter(double baseX, double baseY, double baseZ, double lineSpacing) {
        if (lines.isEmpty()) return new double[]{baseX, baseY, baseZ};
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (HologramLine line : lines) {
            double y = line.getWorldY(baseY, lineSpacing);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new double[]{baseX, (minY + maxY) / 2, baseZ};
    }

    /**
     * 获取下一行位置。
     */
    public double getNextLineY(double baseY, double lineSpacing) {
        if (lines.isEmpty()) return baseY;
        HologramLine lastLine = lines.get(lines.size() - 1);
        return lastLine.getWorldY(baseY, lineSpacing) - lineSpacing;
    }

    /**
     * 克隆页面。
     */
    public Page clone(int newIndex) {
        Page cloned = new Page(newIndex);
        for (HologramLine line : lines) {
            HologramLine clonedLine = new HologramLine(line.getIndex(), line.getDisplayConfig());
            clonedLine.setOffsetX(line.getOffsetX());
            clonedLine.setOffsetY(line.getOffsetY());
            clonedLine.setOffsetZ(line.getOffsetZ());
            clonedLine.setLineHeight(line.getLineHeight());
            clonedLine.setPermission(line.getPermission());
            for (String flag : line.getFlags()) {
                clonedLine.addFlag(flag);
            }
            cloned.lines.add(clonedLine);
        }
        cloned.pageActions.putAll(pageActions);
        return cloned;
    }
}
