package org.windy.hologram.action;

import java.util.UUID;

/**
 * 发送消息给玩家。
 */
public class MessageAction implements Action {

    private final String message;

    public MessageAction(String message) {
        this.message = message;
    }

    @Override
    public void execute(UUID playerId) {
        ActionContext.sendMessage(playerId, message);
    }

    @Override
    public ActionType getType() { return ActionType.MESSAGE; }

    @Override
    public String serialize() { return "message:" + message; }

    public static MessageAction deserialize(String data) {
        return new MessageAction(data);
    }
}
