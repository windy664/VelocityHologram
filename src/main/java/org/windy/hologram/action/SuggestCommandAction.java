package org.windy.hologram.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.UUID;

/**
 * 建议命令（填入聊天框但不执行）。
 */
public class SuggestCommandAction implements Action {

    private final String command;

    public SuggestCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(UUID playerId) {
        ActionContext.suggestCommand(playerId, command);
    }

    @Override
    public ActionType getType() { return ActionType.SUGGEST_COMMAND; }

    @Override
    public String serialize() { return "suggest:" + command; }

    public static SuggestCommandAction deserialize(String data) {
        return new SuggestCommandAction(data);
    }
}
