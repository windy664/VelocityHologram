package org.windy.hologram.hologram;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.animation.AnimationParser;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.api.IHologram;
import org.windy.hologram.api.IHologramLine;
import org.windy.hologram.display.TextDisplayFactory;
import org.windy.hologram.placeholder.PlaceholderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 悬浮字核心实现。
 * <p>管理一组 {@link HologramLine}，维护观察者列表，通过 packetevents 发包。
 * <p>支持动作、动画、占位符。
 */
public class Hologram implements IHologram {

    private final UUID id;
    private final String name;
    private final HologramPos position;
    private final List<HologramLine> lines = new ArrayList<>();
    private final Set<UUID> observers = ConcurrentHashMap.newKeySet();

    // 外部依赖
    private final ClickHandler clickHandler;
    private final PlaceholderManager placeholderManager;

    // 视距（默认 48 格）
    private double viewDistance = 48.0;

    public Hologram(String name, HologramPos position, ClickHandler clickHandler,
                    PlaceholderManager placeholderManager) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.position = position;
        this.clickHandler = clickHandler;
        this.placeholderManager = placeholderManager;
    }

    @Override public UUID getId() { return id; }
    @Override public String getName() { return name; }
    @Override public HologramPos getPosition() { return position; }

    public double getViewDistance() { return viewDistance; }
    public void setViewDistance(double viewDistance) { this.viewDistance = viewDistance; }

    @Override
    public List<IHologramLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * 获取指定索引的行。
     */
    public HologramLine getLine(int index) {
        if (index >= 0 && index < lines.size()) {
            return lines.get(index);
        }
        return null;
    }

    @Override
    public void addLine(String text) {
        HologramLine line = new HologramLine(lines.size(), text);

        // 检查是否有动画
        if (AnimationParser.hasAnimation(text)) {
            TextAnimation animation = AnimationParser.parse(text);
            line.setAnimation(animation);
        }

        lines.add(line);
    }

    @Override
    public void setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            HologramLine line = lines.get(index);
            line.setText(text);

            // 更新动画
            if (AnimationParser.hasAnimation(text)) {
                line.setAnimation(AnimationParser.parse(text));
            } else {
                line.setAnimation(null);
            }
        }
    }

    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            HologramLine removed = lines.remove(index);

            // 注销点击动作
            if (clickHandler != null) {
                clickHandler.unregisterClickAction(removed.getEntityId());
            }

            // 先对所有观察者销毁该行
            for (UUID playerId : observers) {
                TextDisplayFactory.despawn(playerId, removed.getEntityId());
            }

            // 重建索引
            for (int i = 0; i < lines.size(); i++) {
                lines.get(i).setIndex(i);
            }
        }
    }

    /**
     * 设置行的点击动作。
     */
    public void setLineAction(int index, Action leftClick, Action rightClick) {
        if (index >= 0 && index < lines.size()) {
            HologramLine line = lines.get(index);
            line.setLeftClickAction(leftClick);
            line.setRightClickAction(rightClick);

            // 注册到点击处理器
            if (clickHandler != null) {
                clickHandler.registerClickAction(line.getEntityId(), leftClick, rightClick);
            }
        }
    }

    @Override
    public void showTo(UUID playerId) {
        if (observers.add(playerId)) {
            for (HologramLine line : lines) {
                double y = line.getWorldY(position.y());
                String text = resolveText(line, playerId);
                TextDisplayFactory.spawn(playerId, line.getEntityId(),
                        position.x(), y, position.z(), text);
            }
        }
    }

    @Override
    public void hideFrom(UUID playerId) {
        if (observers.remove(playerId)) {
            for (HologramLine line : lines) {
                TextDisplayFactory.despawn(playerId, line.getEntityId());
            }
        }
    }

    @Override
    public void update() {
        for (UUID playerId : observers) {
            for (HologramLine line : lines) {
                String text = resolveText(line, playerId);
                TextDisplayFactory.updateText(playerId, line.getEntityId(), text);
            }
        }
    }

    /**
     * 推进动画并更新显示。
     *
     * @return true 如果有帧变化
     */
    public boolean tickAnimation() {
        boolean changed = false;
        for (HologramLine line : lines) {
            if (line.tickAnimation()) {
                changed = true;
            }
        }

        if (changed) {
            update();
        }

        return changed;
    }

    /**
     * 替换占位符和动画。
     */
    private String resolveText(HologramLine line, UUID playerId) {
        String text = line.getAnimationText();

        // 替换占位符
        if (placeholderManager != null) {
            text = placeholderManager.replace(text, playerId);
        }

        return text;
    }

    @Override
    public void destroy() {
        for (UUID playerId : observers) {
            for (HologramLine line : lines) {
                TextDisplayFactory.despawn(playerId, line.getEntityId());
            }
        }

        // 注销所有点击动作
        if (clickHandler != null) {
            for (HologramLine line : lines) {
                clickHandler.unregisterClickAction(line.getEntityId());
            }
        }

        observers.clear();
    }

    public Set<UUID> getObservers() { return observers; }
    public boolean isObserver(UUID playerId) { return observers.contains(playerId); }
}
