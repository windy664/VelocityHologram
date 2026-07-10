package org.windy.hologram.api.event;

import org.windy.hologram.hologram.Hologram;

/**
 * 悬浮字创建事件。
 */
public class HologramCreateEvent extends HologramEvent {
    public HologramCreateEvent(Hologram hologram) { super(hologram); }
}
