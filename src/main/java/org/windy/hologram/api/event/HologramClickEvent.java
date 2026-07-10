package org.windy.hologram.api.event;

import org.windy.hologram.action.Action;
import org.windy.hologram.hologram.Hologram;

import java.util.UUID;

/**
 * 悬浮字点击事件。
 */
public class HologramClickEvent extends HologramEvent {

    private final UUID playerId;
    private final ClickType clickType;
    private final int lineIndex;
    private boolean cancelled = false;

    public HologramClickEvent(Hologram hologram, UUID playerId, ClickType clickType, int lineIndex) {
        super(hologram);
        this.playerId = playerId;
        this.clickType = clickType;
        this.lineIndex = lineIndex;
    }

    public UUID getPlayerId() { return playerId; }
    public ClickType getClickType() { return clickType; }
    public int getLineIndex() { return lineIndex; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /**
     * 点击类型。
     */
    public enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT
    }
}
