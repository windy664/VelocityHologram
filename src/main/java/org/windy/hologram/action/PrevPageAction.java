package org.windy.hologram.action;

import org.windy.hologram.hologram.Hologram;

import java.util.UUID;

/**
 * 翻到上一页。
 */
public class PrevPageAction implements Action {

    private final Hologram hologram;

    public PrevPageAction(Hologram hologram) {
        this.hologram = hologram;
    }

    @Override
    public void execute(UUID playerId) {
        hologram.prevPage(playerId);
    }

    @Override
    public ActionType getType() { return ActionType.PREV_PAGE; }

    @Override
    public String serialize() { return "prevpage"; }
}
