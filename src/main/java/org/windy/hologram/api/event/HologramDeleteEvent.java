package org.windy.hologram.api.event;

import org.windy.hologram.hologram.Hologram;

/**
 * 悬浮字删除事件。
 */
public class HologramDeleteEvent extends HologramEvent {
    public HologramDeleteEvent(Hologram hologram) { super(hologram); }
}
