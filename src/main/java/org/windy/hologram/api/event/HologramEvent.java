package org.windy.hologram.api.event;

import org.windy.hologram.hologram.Hologram;

/**
 * 悬浮字事件基类。
 */
public abstract class HologramEvent {

    private final Hologram hologram;

    protected HologramEvent(Hologram hologram) {
        this.hologram = hologram;
    }

    public Hologram getHologram() { return hologram; }
}
