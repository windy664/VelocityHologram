package org.windy.hologram.action;

import java.util.UUID;

/**
 * 悬浮字动作接口。
 * <p>玩家点击悬浮字时触发的动作。
 */
public interface Action {

    /**
     * 执行动作。
     *
     * @param playerId 触发的玩家 UUID
     */
    void execute(UUID playerId);

    /**
     * 动作类型。
     */
    ActionType getType();

    /**
     * 序列化为配置字符串。
     */
    String serialize();

    /**
     * 动作类型枚举。
     */
    enum ActionType {
        COMMAND,           // 以玩家身份执行命令
        CONSOLE_COMMAND,   // 以控制台身份执行命令
        RCON,              // 通过 RCON 执行命令
        URL,               // 打开 URL
        MESSAGE,           // 发送消息给玩家
        CLOSE,             // 关闭聊天栏
        SUGGEST_COMMAND    // 建议命令（填入聊天框但不执行）
    }
}
