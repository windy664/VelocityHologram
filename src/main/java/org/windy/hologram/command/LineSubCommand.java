package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramLine;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerTracker;

import java.util.List;

/**
 * 行相关子命令。
 * <p>处理 add、set、remove、insert、swap、align、height、offset 等命令。
 */
public class LineSubCommand {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;
    private final HologramLoader hologramLoader;

    public LineSubCommand(HologramManager hologramManager, PlayerTracker playerTracker,
                          HologramLoader hologramLoader) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.hologramLoader = hologramLoader;
    }

    public void handleAddLine(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addline <名称> <文本>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        String text = joinArgs(args, 2);
        hologram.addLine(text);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加文本行到悬浮字 '" + args[1] + "'");
    }

    public void handleAddItem(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo additem <名称> <物品ID>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        String itemId = args[2];
        DisplayConfig config = DisplayConfig.builder(DisplayEntityType.ITEM_DISPLAY)
                .itemId(itemId)
                .build();
        hologram.addLine(config);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加物品行 '" + itemId + "' 到悬浮字 '" + args[1] + "'");
    }

    public void handleAddBlock(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addblock <名称> <方块ID>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        String blockId = args[2];
        DisplayConfig config = DisplayConfig.builder(DisplayEntityType.BLOCK_DISPLAY)
                .blockId(blockId)
                .build();
        hologram.addLine(config);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加方块行 '" + blockId + "' 到悬浮字 '" + args[1] + "'");
    }

    public void handleAddEntity(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addentity <名称> <实体ID>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        String entityName = args[2];
        DisplayConfig config = DisplayConfig.builder(DisplayEntityType.ENTITY)
                .blockId(entityName)
                .build();
        hologram.addLine(config);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加实体行 '" + entityName + "' 到悬浮字 '" + args[1] + "'");
    }

    public void handleAddHead(CommandSource source, String[] args, boolean small) {
        String label = small ? "smallhead" : "head";
        if (args.length < 3) {
            msg(source, "§c用法: /holo " + label + " <名称> <物品/方块ID>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        String itemId = args[2];
        DisplayConfig config = DisplayConfig.builder(small ? DisplayEntityType.SMALLHEAD : DisplayEntityType.HEAD)
                .blockId(itemId)
                .build();
        hologram.addLine(config);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加 " + label + " 行 '" + itemId + "' 到悬浮字 '" + args[1] + "'");
    }

    public void handleAddIcon(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addicon <名称> <物品ID>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        String itemId = args[2];
        DisplayConfig config = DisplayConfig.builder(DisplayEntityType.ICON)
                .blockId(itemId)
                .build();
        hologram.addLine(config);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加 ICON 行 '" + itemId + "' 到悬浮字 '" + args[1] + "'");
    }

    public void handleSetLine(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo setline <名称> <行号> <文本>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        try {
            int index = Integer.parseInt(args[2]);
            String text = joinArgs(args, 3);
            hologram.setLine(index, text);
            hologram.update();
            hologramLoader.save(hologram);
            msg(source, "§a已更新悬浮字 '" + args[1] + "' 第 " + index + " 行");
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleRemoveLine(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo removeline <名称> <行号>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        try {
            int index = Integer.parseInt(args[2]);
            hologram.removeLine(index);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已删除悬浮字 '" + args[1] + "' 第 " + index + " 行");
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleInsertLine(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo insertline <名称> <行号> <文本>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[2]);
            String text = joinArgs(args, 3);
            if (hologram.insertLine(index, text)) {
                hologram.refresh();
                hologramLoader.save(hologram);
                msg(source, "§a已在位置 " + index + " 插入行");
            } else {
                msg(source, "§c插入位置无效");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleSwapLines(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo swaplines <名称> <行号A> <行号B>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int a = Integer.parseInt(args[2]);
            int b = Integer.parseInt(args[3]);
            if (hologram.swapLines(a, b)) {
                hologram.refresh();
                hologramLoader.save(hologram);
                msg(source, "§a已交换第 " + a + " 行和第 " + b + " 行");
            } else {
                msg(source, "§c行号无效");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleSetOffset(CommandSource source, String[] args) {
        if (args.length < 6) {
            msg(source, "§c用法: /holo setoffset <名称> <行号> <x> <y> <z>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        try {
            int index = Integer.parseInt(args[2]);
            double x = Double.parseDouble(args[3]);
            double y = Double.parseDouble(args[4]);
            double z = Double.parseDouble(args[5]);

            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }

            line.setOffsetX(x);
            line.setOffsetY(y);
            line.setOffsetZ(z);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已设置悬浮字 '" + args[1] + "' 第 " + index + " 行的偏移");
        } catch (NumberFormatException e) {
            msg(source, "§c行号和坐标必须是数字");
        }
    }

    public void handleLineAlign(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line align <名称> <行号> <left|center|right>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            String align = args[4].toLowerCase();
            double offset;
            switch (align) {
                case "left": offset = -0.5; break;
                case "center": offset = 0; break;
                case "right": offset = 0.5; break;
                default:
                    msg(source, "§c对齐方式必须是 left、center 或 right");
                    return;
            }
            line.setOffsetX(offset);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已对齐行 " + index + " 到 " + align);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleLineHeight(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line height <名称> <行号> <高度>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            double height = Double.parseDouble(args[4]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            line.setLineHeight(height);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已设置行 " + index + " 的高度为 " + height);
        } catch (NumberFormatException e) {
            msg(source, "§c行号和高度必须是数字");
        }
    }

    public void handleLineOffsetX(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line offsetx <名称> <行号> <x>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            double x = Double.parseDouble(args[4]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            line.setOffsetX(x);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已设置行 " + index + " 的 X 偏移为 " + x);
        } catch (NumberFormatException e) {
            msg(source, "§c行号和偏移必须是数字");
        }
    }

    public void handleLineOffsetZ(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line offsetz <名称> <行号> <z>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            double z = Double.parseDouble(args[4]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            line.setOffsetZ(z);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已设置行 " + index + " 的 Z 偏移为 " + z);
        } catch (NumberFormatException e) {
            msg(source, "§c行号和偏移必须是数字");
        }
    }

    public void handleLineSetPermission(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line setpermission <名称> <行号> <权限节点|->");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            String perm = args[4];
            if (perm.equals("-")) {
                line.setPermission(null);
                msg(source, "§a已清除行 " + index + " 的权限");
            } else {
                line.setPermission(perm);
                msg(source, "§a已设置行 " + index + " 的权限为: " + perm);
            }
            hologram.refresh();
            hologramLoader.save(hologram);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleLineAddFlag(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line addflag <名称> <行号> <flag>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            line.addFlag(args[4]);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已添加 flag '" + args[4] + "' 到行 " + index);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    public void handleLineRemoveFlag(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line removeflag <名称> <行号> <flag>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            HologramLine line = hologram.getLine(index);
            if (line == null) {
                msg(source, "§c行号无效");
                return;
            }
            line.removeFlag(args[4]);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已移除 flag '" + args[4] + "' 从行 " + index);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
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
