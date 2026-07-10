package org.windy.hologram.api.event;

import org.windy.hologram.hologram.Hologram;

import java.util.UUID;

/**
 * 悬浮字翻页事件。
 */
public class HologramPageSwitchEvent extends HologramEvent {

    private final UUID playerId;
    private final int oldPage;
    private final int newPage;

    public HologramPageSwitchEvent(Hologram hologram, UUID playerId, int oldPage, int newPage) {
        super(hologram);
        this.playerId = playerId;
        this.oldPage = oldPage;
        this.newPage = newPage;
    }

    public UUID getPlayerId() { return playerId; }
    public int getOldPage() { return oldPage; }
    public int getNewPage() { return newPage; }
}
