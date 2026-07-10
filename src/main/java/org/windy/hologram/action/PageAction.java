package org.windy.hologram.action;

import org.windy.hologram.hologram.Hologram;

import java.util.UUID;

/**
 * 跳到指定页。
 */
public class PageAction implements Action {

    private final Hologram hologram;
    private final int pageIndex;

    public PageAction(Hologram hologram, int pageIndex) {
        this.hologram = hologram;
        this.pageIndex = pageIndex;
    }

    @Override
    public void execute(UUID playerId) {
        hologram.switchPage(playerId, pageIndex);
    }

    @Override
    public ActionType getType() { return ActionType.PAGE; }

    @Override
    public String serialize() { return "page:" + pageIndex; }
}
