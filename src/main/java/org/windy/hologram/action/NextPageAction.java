package org.windy.hologram.action;

import org.windy.hologram.hologram.Hologram;

import java.util.UUID;

/**
 * 翻到下一页。
 */
public class NextPageAction implements Action {

    private final Hologram hologram;

    public NextPageAction(Hologram hologram) {
        this.hologram = hologram;
    }

    @Override
    public void execute(UUID playerId) {
        hologram.nextPage(playerId);
    }

    @Override
    public ActionType getType() { return ActionType.NEXT_PAGE; }

    @Override
    public String serialize() { return "nextpage"; }
}
