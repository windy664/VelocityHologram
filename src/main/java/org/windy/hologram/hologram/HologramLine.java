package org.windy.hologram.hologram;

import org.windy.hologram.api.IHologramLine;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 悬浮字单行文本实现。
 * <p>每行对应一个 Text Display 实体，拥有独立的实体 ID。
 */
public class HologramLine implements IHologramLine {

    // 实体 ID 从高位递减，避开子服真实实体
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 10000);

    private final int entityId;
    private int index;
    private volatile String text;

    public HologramLine(int index, String text) {
        this.entityId = ENTITY_ID_COUNTER.decrementAndGet();
        this.index = index;
        this.text = text;
    }

    @Override
    public int getIndex() { return index; }

    void setIndex(int index) { this.index = index; }

    @Override
    public String getText() { return text; }

    @Override
    public void setText(String text) { this.text = text; }

    @Override
    public int getEntityId() { return entityId; }

    /**
     * 计算该行在世界中的 Y 坐标。
     * <p>悬浮字基座在 (x, y, z)，每行向上偏移 0.3 格。
     */
    public double getWorldY(double baseY) {
        return baseY - (index * 0.3);
    }
}
