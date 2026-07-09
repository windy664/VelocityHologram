package org.windy.hologram.action;

/**
 * 动作工厂。
 * <p>从配置字符串解析动作。
 */
public class ActionFactory {

    private ActionFactory() {}

    /**
     * 解析动作字符串。
     * <p>格式：
     * <ul>
     *   <li>{@code command:/tp spawn} - 以玩家身份执行命令</li>
     *   <li>{@code console:say hello} - 以控制台身份执行命令</li>
     *   <li>{@code rcon:save-all} - 通过 RCON 执行（发到所有子服）</li>
     *   <li>{@code rcon:lobby:save-all} - 通过 RCON 执行（指定子服）</li>
     *   <li>{@code url:https://example.com} - 打开 URL</li>
     *   <li>{@code message:§a你好} - 发送消息给玩家</li>
     * </ul>
     */
    public static Action parse(String input) {
        if (input == null || input.isEmpty()) return null;

        String[] parts = input.split(":", 2);
        if (parts.length < 2) return null;

        String type = parts[0].toLowerCase();
        String data = parts[1];

        return switch (type) {
            case "command", "cmd" -> new CommandAction(data);
            case "console", "server" -> new ConsoleCommandAction(data);
            case "rcon" -> RconAction.deserialize(data);
            case "url", "link" -> new UrlAction(data);
            case "message", "msg" -> new MessageAction(data);
            case "suggest" -> new SuggestCommandAction(data);
            default -> null;
        };
    }

    /**
     * 序列化动作为配置字符串。
     */
    public static String serialize(Action action) {
        return action.serialize();
    }
}
