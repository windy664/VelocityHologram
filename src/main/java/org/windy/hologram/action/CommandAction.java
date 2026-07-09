package org.windy.hologram.action;

import java.util.UUID;

/**
 * 以玩家身份执行命令。
 */
public class CommandAction implements Action {

    private final String command;

    public CommandAction(String command) {
        this.command = command.startsWith("/") ? command.substring(1) : command;
    }

    @Override
    public void execute(UUID playerId) {
        // 通过 Velocity 的 player.chat() 方法执行命令
        // 需要持有 proxy 引用
        ActionContext.executeAsPlayer(playerId, command);
    }

    @Override
    public ActionType getType() { return ActionType.COMMAND; }

    @Override
    public String serialize() { return "command:" + command; }

    public static CommandAction deserialize(String data) {
        return new CommandAction(data);
    }
}
