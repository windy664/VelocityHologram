package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import org.windy.hologram.action.ActionFactory;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.hologram.Page;

import java.util.List;

/**
 * 页面相关子命令。
 * <p>处理 add、insert、remove、swap、switch、actions 等命令。
 */
public class PageSubCommand {

    private final HologramManager hologramManager;
    private final HologramLoader hologramLoader;

    public PageSubCommand(HologramManager hologramManager, HologramLoader hologramLoader) {
        this.hologramManager = hologramManager;
        this.hologramLoader = hologramLoader;
    }

    public void handleAddPage(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo addpage <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.addPage();
        hologramLoader.save(hologram);
        msg(source, "§a已添加第 " + hologram.getPageCount() + " 页到悬浮字 '" + args[1] + "'");
    }

    public void handleInsertPage(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo insertpage <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[2]);
            if (hologram.insertPage(page) != null) {
                hologramLoader.save(hologram);
                msg(source, "§a已在位置 " + page + " 插入新页");
            } else {
                msg(source, "§c页码无效");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    public void handleRemovePage(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo removepage <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[2]);
            if (hologram.removePage(page)) {
                hologramLoader.save(hologram);
                msg(source, "§a已删除悬浮字 '" + args[1] + "' 第 " + page + " 页");
            } else {
                msg(source, "§c无法删除（至少保留 1 页，或页码无效）");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    public void handleSwapPages(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo swappages <名称> <页码A> <页码B>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int a = Integer.parseInt(args[2]);
            int b = Integer.parseInt(args[3]);
            if (hologram.swapPages(a, b)) {
                hologramLoader.save(hologram);
                msg(source, "§a已交换第 " + a + " 页和第 " + b + " 页");
            } else {
                msg(source, "§c页码无效");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    public void handleSwitchPage(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo switchpage <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[2]);
            hologram.setEditPageIndex(page);
            msg(source, "§a编辑页已切换到第 " + page + " 页（悬浮字 '" + args[1] + "'）");
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    public void handlePageAddAction(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo page addaction <名称> <click> <action>");
            msg(source, "§7click: left, right, shift-left, shift-right");
            msg(source, "§7action: command:/say hi, connect:lobby, nextpage, prevpage");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        Page page = hologram.getCurrentEditPage();
        if (page == null) {
            msg(source, "§c当前编辑页无效");
            return;
        }
        String clickType = args[3].toLowerCase();
        String actionStr = joinArgs(args, 4);
        var action = ActionFactory.parse(actionStr);
        if (action == null) {
            msg(source, "§c无效的动作: " + actionStr);
            return;
        }
        page.addAction(clickType, action);
        hologramLoader.save(hologram);
        msg(source, "§a已添加页面动作: " + clickType + " → " + actionStr);
    }

    public void handlePageRemoveAction(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo page removeaction <名称> <click>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        Page page = hologram.getCurrentEditPage();
        if (page == null) {
            msg(source, "§c当前编辑页无效");
            return;
        }
        String clickType = args[3].toLowerCase();
        page.removeAction(clickType);
        hologramLoader.save(hologram);
        msg(source, "§a已移除页面动作: " + clickType);
    }

    public void handlePageClearActions(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo page clearactions <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        Page page = hologram.getCurrentEditPage();
        if (page == null) {
            msg(source, "§c当前编辑页无效");
            return;
        }
        page.clearActions();
        hologramLoader.save(hologram);
        msg(source, "§a已清除所有页面动作");
    }

    private Hologram getHologramOrWarn(CommandSource source, String name) {
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, Lang.get("holo_not_found", "%name%", name));
        }
        return hologram;
    }

    private static String joinArgs(String[] args, int from) {
        return String.join(" ", List.of(args).subList(from, args.length));
    }

    private static void msg(CommandSource source, String legacy) {
        source.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}
