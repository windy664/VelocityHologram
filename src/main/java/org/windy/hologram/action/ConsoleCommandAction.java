package org.windy.hologram.action;

import java.util.UUID;

/**
 * 以控制台身份执行命令。
 */
public class ConsoleCommandAction implements Action {

    private final String command;

    public ConsoleCommandAction(String command) {
        this.command = command.startsWith("/") ? command.substring(1) : command;
    }

    @Override
    public void execute(UUID playerId) {
        ActionContext.executeConsole(command);
    }

    @Override
    public ActionType getType() { return ActionType.CONSOLE_COMMAND; }

    @Override
    public String serialize() { return "console:" + command; }

    public static ConsoleCommandAction deserialize(String data) {
        return new ConsoleCommandAction(data);
    }
}
