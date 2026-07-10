package org.windy.hologram.utils;

import org.slf4j.Logger;

/**
 * 启动横幅工具类。
 * <p>参考 GuildShelter 的方块艺术字风格。
 */
public final class BannerUtils {

    /** VELOCITYHOLOGRAM 方块艺术字（box-drawing）。 */
    private static final String[] LOGO = {
            "██╗   ██╗███████╗██╗      ██████╗ ██████╗██╗████████╗██╗   ██╗",
            "██║   ██║██╔════╝██║     ██╔═══██╗██╔══██╗██║╚══██╔══╝╚██╗ ██╔╝",
            "██║   ██║█████╗  ██║     ██║   ██║██████╔╝██║   ██║    ╚████╔╝",
            "╚██╗ ██╔╝██╔══╝  ██║     ██║   ██║██╔══██╗██║   ██║     ╚██╔╝",
            " ╚████╔╝ ███████╗███████╗╚██████╔╝██║  ██║██║   ██║      ██║",
            "  ╚═══╝  ╚══════╝╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚═╝   ╚═╝      ╚═╝",
            "              ██╗  ██╗ ██████╗ ██╗      ██████╗  ██████╗ █████╗ ███╗   ███╗",
            "              ██║  ██║██╔═══██╗██║     ██╔═══██╗██╔════╝██╔══██╗████╗ ████║",
            "              ███████║██║   ██║██║     ██║   ██║██║     ███████║██╔████╔██║",
            "              ██╔══██║██║   ██║██║     ██║   ██║██║     ██╔══██║██║╚██╔╝██║",
            "              ██║  ██║╚██████╔╝███████╗╚██████╔╝╚██████╗██║  ██║██║ ╚═╝ ██║",
            "              ╚═╝  ╚═╝ ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝"
    };

    private static final String BAR = "§b▌ §r";

    private BannerUtils() {}

    /**
     * 打印启动横幅。
     */
    public static void printBanner(Logger logger, String version) {
        StringBuilder sb = new StringBuilder("\n");

        // Logo
        for (String line : LOGO) {
            sb.append("§b").append(line).append('\n');
        }
        sb.append('\n');

        // 信息汇总
        sb.append(BAR).append("§3VelocityHologram §7v").append(version)
          .append("  §b❖ 纯代理端悬浮字系统").append('\n');
        sb.append(BAR).append("§7平台  §f").append("Velocity + packetevents").append('\n');
        sb.append(BAR).append("§7作者  §f").append("风吟").append('\n');
        sb.append(BAR).append("§7仓库  §f").append("github.com/windy664/VelocityHologram").append('\n');

        logger.info(sb.toString());
    }

    /**
     * 打印模块加载信息。
     */
    public static void printModule(Logger logger, String name, boolean enabled) {
        if (enabled) {
            logger.info(BAR + "§a✓ §7" + name + " §a已启用");
        } else {
            logger.info(BAR + "§8○ §7" + name + " §8未启用");
        }
    }

    /**
     * 打印加载统计。
     */
    public static void printStats(Logger logger, int hologramCount, int playerCount) {
        logger.info(BAR + "§7数据  §f" + hologramCount + " 个悬浮字 · " + playerCount + " 个在线玩家");
    }

    /**
     * 打印完成信息。
     */
    public static void printDone(Logger logger, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("");
        logger.info(BAR + "§a✔ 已启用  §7输入 §f/holo help §7查看命令");
        logger.info(BAR + "§7耗时 §b" + elapsed + "ms");
        logger.info("");
    }

    /**
     * 打印更新提示。
     */
    public static void printUpdate(Logger logger, String current, String latest) {
        logger.info("");
        logger.info(BAR + "§e§l⚡ 有新版本可用！");
        logger.info(BAR + "§7当前: §c" + current + " §7→ 最新: §a" + latest);
        logger.info(BAR + "§7下载: §bhttps://github.com/windy664/VelocityHologram/releases");
        logger.info("");
    }
}
