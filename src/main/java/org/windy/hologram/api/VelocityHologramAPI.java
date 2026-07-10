package org.windy.hologram.api;

import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.util.Collection;
import java.util.UUID;

/**
 * VelocityHologram 公共 API。
 * <p>供其他插件编程创建和管理悬浮字。
 *
 * <pre>
 * // 获取管理器
 * HologramManager mgr = VelocityHologramAPI.getManager();
 *
 * // 创建悬浮字
 * Hologram holo = mgr.createHologram("test", 0, 100, 0, "minecraft:overworld", "lobby");
 * holo.addLine("§bHello");
 * holo.addPage();
 * holo.setLineAction(0, new ConnectAction("lobby"), null);
 *
 * // 查询
 * Collection&lt;Hologram&gt; all = mgr.getAllHolograms();
 * Hologram byName = mgr.getHologram("test");
 * </pre>
 */
public class VelocityHologramAPI {

    private static HologramManager manager;

    /**
     * 获取悬浮字管理器。
     */
    public static HologramManager getManager() {
        return manager;
    }

    /**
     * 注册管理器（内部使用）。
     */
    public static void setManager(HologramManager mgr) {
        manager = mgr;
    }

    /**
     * 获取指定名称的悬浮字。
     */
    public static Hologram getHologram(String name) {
        return manager != null ? manager.getHologram(name) : null;
    }

    /**
     * 获取所有悬浮字。
     */
    public static Collection<Hologram> getAllHolograms() {
        return manager != null ? manager.getAllHolograms() : java.util.Collections.emptyList();
    }

    /**
     * 创建悬浮字。
     */
    public static Hologram createHologram(String name, double x, double y, double z,
                                           String dimension, String server) {
        return manager != null ? manager.createHologram(name, x, y, z, dimension, server) : null;
    }

    /**
     * 删除悬浮字。
     */
    public static void removeHologram(String name) {
        if (manager != null) manager.removeHologram(name);
    }
}
