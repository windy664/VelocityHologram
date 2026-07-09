package org.windy.hologram.hologram;

import org.windy.hologram.action.Action;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.api.IHologramLine;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 悬浮字单行文本实现。
 * <p>每行对应一个 Text Display 实体，拥有独立的实体 ID。
 * <p>支持动作（点击执行命令）和动画（文本循环）。
 */
public class HologramLine implements IHologramLine {

    // 实体 ID 从高位递减，避开子服真实实体
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 10000);

    private final int entityId;
    private int index;
    private volatile String text;
    private volatile String rawText; // 原始文本（含占位符和动画语法）

    // 动作
    private Action leftClickAction;
    private Action rightClickAction;

    // 动画
    private TextAnimation animation;

    public HologramLine(int index, String text) {
        this.entityId = ENTITY_ID_COUNTER.decrementAndGet();
        this.index = index;
        this.text = text;
        this.rawText = text;
    }

    @Override
    public int getIndex() { return index; }

    void setIndex(int index) { this.index = index; }

    @Override
    public String getText() { return text; }

    @Override
    public void setText(String text) {
        this.text = text;
        this.rawText = text;
    }

    /**
     * 获取原始文本（含占位符和动画语法）。
     */
    public String getRawText() { return rawText; }

    /**
     * 设置显示文本（已替换占位符和动画）。
     */
    public void setDisplayText(String text) {
        this.text = text;
    }

    @Override
    public int getEntityId() { return entityId; }

    // ===== 动作 =====

    public Action getLeftClickAction() { return leftClickAction; }
    public void setLeftClickAction(Action action) { this.leftClickAction = action; }

    public Action getRightClickAction() { return rightClickAction; }
    public void setRightClickAction(Action action) { this.rightClickAction = action; }

    // ===== 动画 =====

    public TextAnimation getAnimation() { return animation; }
    public void setAnimation(TextAnimation animation) { this.animation = animation; }

    /**
     * 推进动画。
     *
     * @return true 如果帧变化了
     */
    public boolean tickAnimation() {
        if (animation == null) return false;
        return animation.tick();
    }

    /**
     * 获取当前动画帧文本。
     */
    public String getAnimationText() {
        if (animation == null) return rawText;
        return animation.getCurrentFrame();
    }

    /**
     * 计算该行在世界中的 Y 坐标。
     */
    public double getWorldY(double baseY) {
        return baseY - (index * 0.3);
    }
}
