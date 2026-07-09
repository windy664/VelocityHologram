package org.windy.hologram.action;

import java.util.UUID;

/**
 * 通过 RCON 执行命令。
 * <p>可以指定目标服务器，不指定则发到所有子服。
 */
public class RconAction implements Action {

    private final String command;
    private final String targetServer; // null = 所有子服

    public RconAction(String command, String targetServer) {
        this.command = command.startsWith("/") ? command.substring(1) : command;
        this.targetServer = targetServer;
    }

    public RconAction(String command) {
        this(command, null);
    }

    @Override
    public void execute(UUID playerId) {
        ActionContext.executeRcon(command, targetServer);
    }

    @Override
    public ActionType getType() { return ActionType.RCON; }

    @Override
    public String serialize() {
        if (targetServer != null) {
            return "rcon:" + targetServer + ":" + command;
        }
        return "rcon:" + command;
    }

    public static RconAction deserialize(String data) {
        String[] parts = data.split(":", 2);
        if (parts.length == 2 && !parts[0].contains(" ")) {
            // rcon:server:command 格式
            return new RconAction(parts[1], parts[0]);
        }
        // rcon:command 格式（发到所有子服）
        return new RconAction(data);
    }
}
