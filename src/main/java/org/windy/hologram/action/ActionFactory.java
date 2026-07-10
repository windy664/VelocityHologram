package org.windy.hologram.action;

import org.windy.hologram.hologram.Hologram;

/**
 * 动作工厂。
 * <p>从配置字符串解析动作。
 */
public class ActionFactory {

    private ActionFactory() {}

    /**
     * 解析动作字符串（不支持 page 动作）。
     */
    public static Action parse(String input) {
        return parse(input, null);
    }

    /**
     * 解析动作字符串（支持 page 动作需要 Hologram 引用）。
     */
    public static Action parse(String input, Hologram hologram) {
        return ActionChain.parseSingle(input, hologram);
    }

    /**
     * 解析动作链。
     */
    public static ActionChain parseChain(String input, Hologram hologram) {
        return ActionChain.parse(input, hologram);
    }

    /**
     * 序列化动作为配置字符串。
     */
    public static String serialize(Action action) {
        return action.serialize();
    }
}
