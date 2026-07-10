package org.windy.hologram.api;

import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramLine;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.hologram.Page;

import java.util.Collection;
import java.util.UUID;

/**
 * VelocityHologram 简化 API 入口。
 * <p>提供静态方法快速操作悬浮字。
 */
public final class DHAPI {

    private DHAPI() {}

    private static HologramManager getManager() {
        return VelocityHologramAPI.getManager();
    }

    /**
     * 创建悬浮字。
     */
    public static Hologram createHologram(String name, double x, double y, double z,
                                           String dimension, String server) {
        return getManager().createHologram(name, x, y, z, dimension, server);
    }

    /**
     * 获取悬浮字。
     */
    public static Hologram getHologram(String name) {
        return getManager().getHologram(name);
    }

    /**
     * 获取所有悬浮字。
     */
    public static Collection<Hologram> getHolograms() {
        return getManager().getAllHolograms();
    }

    /**
     * 删除悬浮字。
     */
    public static boolean deleteHologram(String name) {
        Hologram hologram = getManager().getHologram(name);
        if (hologram == null) return false;
        getManager().removeHologram(name);
        return true;
    }

    /**
     * 显示悬浮字给玩家。
     */
    public static void showHologram(Hologram hologram, UUID playerId) {
        hologram.showTo(playerId);
    }

    /**
     * 隐藏悬浮字。
     */
    public static void hideHologram(Hologram hologram, UUID playerId) {
        hologram.hideFrom(playerId);
    }

    /**
     * 更新悬浮字。
     */
    public static void updateHologram(Hologram hologram) {
        hologram.update();
    }

    /**
     * 传送悬浮字到新位置。
     */
    public static void teleportHologram(Hologram hologram, double x, double y, double z) {
        hologram.setPosition(x, y, z);
        hologram.refresh();
    }

    /**
     * 移动悬浮字（别名）。
     */
    public static void moveHologram(Hologram hologram, double x, double y, double z) {
        teleportHologram(hologram, x, y, z);
    }

    /**
     * 添加文本行。
     */
    public static HologramLine addLine(Hologram hologram, String text) {
        Page page = hologram.getCurrentEditPage();
        if (page == null) return null;
        HologramLine line = page.addLine(text);
        hologram.refresh();
        return line;
    }

    /**
     * 设置行文本。
     */
    public static HologramLine setLine(Hologram hologram, int index, String text) {
        Page page = hologram.getCurrentEditPage();
        if (page == null) return null;
        page.setLine(index, text);
        hologram.update();
        return page.getLine(index);
    }

    /**
     * 删除行。
     */
    public static void removeLine(Hologram hologram, int index) {
        hologram.removeLine(index);
        hologram.refresh();
    }

    /**
     * 添加页。
     */
    public static Page addPage(Hologram hologram) {
        return hologram.addPage();
    }

    /**
     * 删除页。
     */
    public static boolean removePage(Hologram hologram, int index) {
        boolean result = hologram.removePage(index);
        if (result) hologram.refresh();
        return result;
    }

    /**
     * 重命名悬浮字。
     */
    public static boolean renameHologram(String oldName, String newName) {
        return getManager().renameHologram(oldName, newName);
    }

    /**
     * 克隆悬浮字。
     */
    public static Hologram cloneHologram(Hologram source, String newName, double x, double y, double z) {
        Hologram cloned = createHologram(newName, x, y, z,
                source.getPosition().dimension(), source.getPosition().server());

        // 复制配置
        cloned.setViewDistance(source.getViewDistance());
        cloned.setUpdateDistance(source.getUpdateDistance());
        cloned.setLineSpacing(source.getLineSpacing());
        cloned.setUpdateInterval(source.getUpdateInterval());
        cloned.setPermission(source.getPermission());
        cloned.setDownOrigin(source.isDownOrigin());
        cloned.setAlwaysFacePlayer(source.isAlwaysFacePlayer());

        // 复制 flags
        for (String flag : source.getFlags()) {
            cloned.addFlag(flag);
        }

        // 复制页和行
        for (int pi = 0; pi < source.getPageCount(); pi++) {
            Page srcPage = source.getPage(pi);
            if (srcPage == null) continue;

            Page dstPage = (pi == 0) ? cloned.getPage(0) : cloned.addPage();
            if (dstPage == null) continue;

            for (var line : srcPage.getLines()) {
                if (!(line instanceof HologramLine)) continue;
                HologramLine srcLine = (HologramLine) line;
                DisplayConfig config = srcLine.getDisplayConfig();
                HologramLine dstLine = dstPage.addLine(config);

                dstLine.setOffsetY(srcLine.getOffsetY());
                dstLine.setOffsetX(srcLine.getOffsetX());
                dstLine.setOffsetZ(srcLine.getOffsetZ());
                dstLine.setLineHeight(srcLine.getLineHeight());
                dstLine.setPermission(srcLine.getPermission());

                for (String flag : srcLine.getFlags()) {
                    dstLine.addFlag(flag);
                }
            }
        }

        return cloned;
    }
}
