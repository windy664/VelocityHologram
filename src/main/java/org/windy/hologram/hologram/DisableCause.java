package org.windy.hologram.hologram;

/**
 * 悬浮字禁用原因。
 */
public enum DisableCause {
    /** 通过 API 禁用 */
    API,
    /** 通过命令禁用 */
    COMMAND,
    /** 世界卸载 */
    WORLD_UNLOAD,
    /** 无/未禁用 */
    NONE
}
