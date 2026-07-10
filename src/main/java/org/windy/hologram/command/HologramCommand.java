package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramLine;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.hologram.Page;
import org.windy.hologram.tracker.PlayerTracker;
import org.windy.hologram.tracker.PlayerState;

import java.util.List;
import java.util.UUID;

/**
 * /holo 命令实现。
 * <p>支持创建、删除、编辑、调试、权限等操作。
 *
 * <p>权限节点：
 * <ul>
 *   <li>velocityhologram.command.create - 创建悬浮字</li>
 *   <li>velocityhologram.command.delete - 删除悬浮字</li>
 *   <li>velocityhologram.command.edit - 编辑悬浮字（addline/setline/removeline）</li>
 *   <li>velocityhologram.command.save - 保存悬浮字</li>
 *   <li>velocityhologram.command.reload - 重载配置</li>
 *   <li>velocityhologram.command.list - 列出悬浮字</li>
 *   <li>velocityhologram.command.debug - 调试信息</li>
 *   <li>velocityhologram.command.admin - 管理权限（包含所有）</li>
 * </ul>
 */
public class HologramCommand implements SimpleCommand {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;
    private final HologramLoader hologramLoader;
    private final ClickHandler clickHandler;

    public HologramCommand(HologramManager hologramManager, PlayerTracker playerTracker,
                           HologramLoader hologramLoader, ClickHandler clickHandler) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.hologramLoader = hologramLoader;
        this.clickHandler = clickHandler;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        String cmd = args[0].toLowerCase();
        switch (cmd) {
            case "create":
                if (!checkPerm(source, "velocityhologram.command.create")) return;
                handleCreate(source, args);
                break;
            case "delete":
                if (!checkPerm(source, "velocityhologram.command.delete")) return;
                handleDelete(source, args);
                break;
            case "addline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddLine(source, args);
                break;
            case "setline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSetLine(source, args);
                break;
            case "removeline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleRemoveLine(source, args);
                break;
            case "additem":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddItem(source, args);
                break;
            case "addblock":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddBlock(source, args);
                break;
            case "addentity":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddEntity(source, args);
                break;
            case "addhead":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddHead(source, args, false);
                break;
            case "addsmallhead":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddHead(source, args, true);
                break;
            case "addpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAddPage(source, args);
                break;
            case "removepage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleRemovePage(source, args);
                break;
            case "switchpage":
            case "editpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSwitchPage(source, args);
                break;
            case "insertline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleInsertLine(source, args);
                break;
            case "swaplines":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSwapLines(source, args);
                break;
            case "list":
                if (!checkPerm(source, "velocityhologram.command.list")) return;
                handleList(source);
                break;
            case "save":
                if (!checkPerm(source, "velocityhologram.command.save")) return;
                handleSave(source, args);
                break;
            case "reload":
                if (!checkPerm(source, "velocityhologram.command.reload")) return;
                handleReload(source);
                break;
            case "debug":
                if (!checkPerm(source, "velocityhologram.command.debug")) return;
                handleDebug(source, args);
                break;
            case "tp":
                if (!checkPerm(source, "velocityhologram.command.debug")) return;
                handleTeleport(source, args);
                break;
            case "info":
                if (!checkPerm(source, "velocityhologram.command.debug")) return;
                handleInfo(source);
                break;
            case "permission":
            case "perm":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                handlePermission(source, args);
                break;
            case "addflag":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                handleAddFlag(source, args);
                break;
            case "removeflag":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                handleRemoveFlag(source, args);
                break;
            case "move":
            case "movehere":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleMove(source, args);
                break;
            case "clone":
                if (!checkPerm(source, "velocityhologram.command.create")) return;
                handleClone(source, args);
                break;
            case "center":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleCenter(source, args);
                break;
            case "setoffset":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSetOffset(source, args);
                break;
            case "near":
                if (!checkPerm(source, "velocityhologram.command.list")) return;
                handleNear(source, args);
                break;
            case "rename":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                handleRename(source, args);
                break;
            case "enable":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleEnable(source, args, true);
                break;
            case "disable":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleEnable(source, args, false);
                break;
            case "align":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleAlign(source, args);
                break;
            case "setfacing":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSetFacing(source, args);
                break;
            case "insertpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleInsertPage(source, args);
                break;
            case "swappages":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleSwapPages(source, args);
                break;
            case "line":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleLineCommand(source, args);
                break;
            case "page":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handlePageCommand(source, args);
                break;
            default:
                sendHelp(source);
                break;
        }
    }

    // ===== Tab 补全 =====

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "addline", "additem", "addblock", "addentity",
            "addhead", "addsmallhead", "setline", "removeline", "insertline", "swaplines",
            "addpage", "removepage", "switchpage", "editpage", "insertpage", "swappages",
            "addflag", "removeflag",
            "move", "movehere", "clone", "center", "setoffset", "near",
            "rename", "enable", "disable", "align", "setfacing",
            "line", "page",
            "list", "save", "reload", "debug", "tp", "info", "permission", "perm"
    );

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            // 补全子命令
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(java.util.stream.Collectors.toList());
        }

        String cmd = args[0].toLowerCase();
        String prefix = args[args.length - 1].toLowerCase();

        switch (cmd) {
            case "delete": case "addline": case "setline": case "removeline":
            case "insertline": case "swaplines": case "save": case "debug": case "tp":
            case "addpage": case "removepage": case "switchpage": case "editpage":
            case "removeflag": case "permission": case "perm":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                break;
            case "additem":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                if (args.length == 3) {
                    return filterByPrefix(List.of(
                            "diamond_sword", "diamond_pickaxe", "netherite_sword",
                            "golden_apple", "ender_pearl", "blaze_rod", "emerald",
                            "experience_bottle", "totem_of_undying", "elytra"
                    ), prefix);
                }
                break;
            case "addblock":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                if (args.length == 3) {
                    return filterByPrefix(List.of(
                            "grass_block", "stone", "diamond_block", "gold_block",
                            "emerald_block", "netherite_block", "glass", "obsidian",
                            "bedrock", "beacon", "chest", "crafting_table"
                    ), prefix);
                }
                break;
            case "addentity":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                if (args.length == 3) {
                    return filterByPrefix(List.of(
                            "pig", "cow", "chicken", "sheep", "zombie", "skeleton",
                            "creeper", "spider", "enderman", "blaze", "axolotl",
                            "allay", "frog", "warden", "armor_stand"
                    ), prefix);
                }
                break;
            case "addhead": case "addsmallhead":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                if (args.length == 3) {
                    return filterByPrefix(List.of(
                            "grass_block", "stone", "diamond_block", "diamond_sword",
                            "player_head", "skeleton_skull", "zombie_head",
                            "creeper_head", "dragon_head", "nether_star"
                    ), prefix);
                }
                break;
            case "addflag":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                if (args.length == 3) {
                    return filterByPrefix(List.of(
                            "always_visible", "disable_placeholders",
                            "disable_actions", "disable_updates"
                    ), prefix);
                }
                break;
            case "move": case "movehere": case "clone": case "center":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                break;
            case "setoffset":
                if (args.length == 2) {
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
                }
                break;
            case "near":
                // 无需补全
                break;
        }

        return List.of();
    }

    private List<String> filterByPrefix(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(java.util.stream.Collectors.toList());
    }

    // ===== 子命令实现 =====

    private void handleCreate(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;

        if (args.length < 2) {
            msg(source, "§c用法: /holo create <名称> [文字]");
            return;
        }

        String name = args[1];
        if (hologramManager.getHologram(name) != null) {
            msg(source, Lang.get("holo_already_exists", "%name%", name));
            return;
        }

        UUID playerId = player.getUniqueId();
        PlayerState state = playerTracker.get(playerId);
        if (state == null) {
            // 兜底：插件启动前就进服的玩家，手动注册
            playerTracker.register(playerId, player.getUsername());
            state = playerTracker.get(playerId);
        }
        if (state == null) {
            msg(source, "§c无法获取你的位置信息");
            return;
        }

        // 位置为 0 说明包拦截还没收到移动包，提示玩家移动一下
        boolean posReady = !(state.getX() == 0 && state.getY() == 0 && state.getZ() == 0);

        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        Hologram hologram = hologramManager.createHologram(name, state.getX(), state.getY(), state.getZ(),
                state.getDimension(), server);

        // 如果提供了初始文字，自动添加为第一行
        if (args.length >= 3) {
            String text = joinArgs(args, 2);
            hologram.addLine(text);
        }
        hologramLoader.save(hologram);

        if (posReady) {
            // 立即显示给创建者
            hologram.showTo(player.getUniqueId());
            msg(source, "§a已创建悬浮字 '" + name + "' 在你的位置");
        } else {
            msg(source, "§a已创建悬浮字 '" + name + "'，移动一下即可看到");
        }
    }

    private void handleDelete(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo delete <名称>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, Lang.get("holo_not_found", "%name%", name));
            return;
        }

        hologramManager.removeHologram(name);
        hologramLoader.delete(name);
        msg(source, "§a已删除悬浮字 '" + name + "'");
    }

    private void handleAddLine(CommandSource source, String[] args) {
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

    private void handleAddItem(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo additem <名称> <物品ID>");
            msg(source, "§7例如: /holo additem test diamond_sword");
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

    private void handleAddBlock(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addblock <名称> <方块ID>");
            msg(source, "§7例如: /holo addblock test grass_block");
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

    private void handleAddEntity(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addentity <名称> <实体ID>");
            msg(source, "§7例如: /holo addentity test pig");
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

    private void handleAddHead(CommandSource source, String[] args, boolean small) {
        String label = small ? "smallhead" : "head";
        if (args.length < 3) {
            msg(source, "§c用法: /holo " + label + " <名称> <物品/方块ID>");
            msg(source, "§7例如: /holo " + label + " test grass_block");
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

    private void handleSetLine(CommandSource source, String[] args) {
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

    private void handleRemoveLine(CommandSource source, String[] args) {
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

    private void handleAddPage(CommandSource source, String[] args) {
        if (args.length < 2) { msg(source, "§c用法: /holo addpage <名称>"); return; }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.addPage();
        hologramLoader.save(hologram);
        msg(source, "§a已添加第 " + hologram.getPageCount() + " 页到悬浮字 '" + args[1] + "'");
    }

    private void handleRemovePage(CommandSource source, String[] args) {
        if (args.length < 3) { msg(source, "§c用法: /holo removepage <名称> <页码>"); return; }
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
        } catch (NumberFormatException e) { msg(source, "§c页码必须是数字"); }
    }

    private void handleSwitchPage(CommandSource source, String[] args) {
        if (args.length < 3) { msg(source, "§c用法: /holo switchpage <名称> <页码>"); return; }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[2]);
            hologram.setEditPageIndex(page);
            msg(source, "§a编辑页已切换到第 " + page + " 页（悬浮字 '" + args[1] + "'）");
        } catch (NumberFormatException e) { msg(source, "§c页码必须是数字"); }
    }

    private void handleInsertLine(CommandSource source, String[] args) {
        if (args.length < 4) { msg(source, "§c用法: /holo insertline <名称> <行号> <文本>"); return; }
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
        } catch (NumberFormatException e) { msg(source, "§c行号必须是数字"); }
    }

    private void handleSwapLines(CommandSource source, String[] args) {
        if (args.length < 4) { msg(source, "§c用法: /holo swaplines <名称> <行号A> <行号B>"); return; }
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
        } catch (NumberFormatException e) { msg(source, "§c行号必须是数字"); }
    }

    private void handleList(CommandSource source) {
        var holograms = hologramManager.getAllHolograms();
        if (holograms.isEmpty()) {
            msg(source, "§7当前没有悬浮字");
            return;
        }

        msg(source, "§a=== 悬浮字列表 (" + holograms.size() + ") ===");
        for (Hologram hologram : holograms) {
            msg(source, "§e- " + hologram.getName()
                    + " §7(" + fmtCoord(hologram.getPosition().x())
                    + ", " + fmtCoord(hologram.getPosition().y())
                    + ", " + fmtCoord(hologram.getPosition().z())
                    + ") [" + hologram.getPosition().server() + "]"
                    + " §8" + hologram.getPageCount() + "页/" + hologram.getPage(0).getLineCount() + "行"
                    + (hologram.getPermission() != null ? " §c🔒" : ""));
        }
    }

    private void handleSave(CommandSource source, String[] args) {
        if (args.length < 2) {
            // 保存所有
            for (Hologram holo : hologramManager.getAllHolograms()) {
                hologramLoader.save(holo);
            }
            msg(source, "§a已保存所有悬浮字到配置文件");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, Lang.get("holo_not_found", "%name%", name));
            return;
        }

        hologramLoader.save(hologram);
        msg(source, "§a已保存悬浮字 '" + name + "' 到配置文件");
    }

    private void handleReload(CommandSource source) {
        // 先收集名称，再逐个移除（removeHologram 内部会 destroy）
        var names = hologramManager.getAllHolograms().stream()
                .map(Hologram::getName).collect(java.util.stream.Collectors.toList());
        for (String name : names) {
            hologramManager.removeHologram(name);
        }

        hologramLoader.loadAll(hologramManager);
        msg(source, "§a配置已重新加载，共 " + hologramManager.getAllHolograms().size() + " 个悬浮字");
    }

    private void handleDebug(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo debug <名称>");
            return;
        }

        Hologram hologram = hologramManager.getHologram(args[1]);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + args[1] + "' 不存在");
            return;
        }

        var pos = hologram.getPosition();
        msg(source, "§a=== 悬浮字调试: " + hologram.getName() + " ===");
        msg(source, "§7位置: §f" + fmtCoord(pos.x()) + ", " + fmtCoord(pos.y()) + ", " + fmtCoord(pos.z()));
        msg(source, "§7维度: §f" + pos.dimension());
        msg(source, "§7服务器: §f" + pos.server());
        msg(source, "§7视距: §f" + hologram.getViewDistance());
        msg(source, "§7行间距: §f" + hologram.getLineSpacing());
        msg(source, "§7页数: §f" + hologram.getPageCount() + " §7(编辑页: " + hologram.getEditPageIndex() + ")");
        msg(source, "§7当前页行数: §f" + hologram.getLineCount());
        msg(source, "§7观察者: §f" + hologram.getObservers().size());
        msg(source, "§7权限: §f" + (hologram.getPermission() != null ? hologram.getPermission() : "无"));
        msg(source, "§7更新间隔: §f" + hologram.getUpdateInterval() + " tick");
        msg(source, "§7更新范围: §f" + hologram.getUpdateDistance());
        msg(source, "§7Flag: §f" + (hologram.getFlags().isEmpty() ? "无" : String.join(", ", hologram.getFlags())));

        for (int pi = 0; pi < hologram.getPageCount(); pi++) {
            var page = hologram.getPage(pi);
            if (page == null) continue;
            msg(source, "  §6--- 第 " + pi + " 页 ---");
            for (int i = 0; i < page.getLineCount(); i++) {
                HologramLine line = page.getLine(i);
                if (line != null) {
                    msg(source, "    §e[" + i + "] §7类型=" + line.getEntityType().toConfig()
                            + " §f" + truncate(line.getText(), 40)
                            + (line.getAnimation() != null ? " §d[动画]" : "")
                            + (line.getLeftClickAction() != null ? " §a[L]" : "")
                            + (line.getRightClickAction() != null ? " §b[R]" : "")
                            + (line.getShiftLeftClickAction() != null ? " §a[SL]" : "")
                            + (line.getShiftRightClickAction() != null ? " §b[SR]" : ""));
                }
            }
        }
    }

    private void handleTeleport(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;
        if (args.length < 2) {
            msg(source, "§c用法: /holo tp <名称>");
            return;
        }

        Hologram hologram = hologramManager.getHologram(args[1]);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + args[1] + "' 不存在");
            return;
        }

        var pos = hologram.getPosition();
        String playerName = player.getUsername();

        // 构造 tp 命令（带维度）
        String tpCmd = "execute in " + pos.dimension() + " run tp "
                + playerName + " " + pos.x() + " " + pos.y() + " " + pos.z();

        // 优先用 RCON 发到目标子服
        boolean rconOk = false;
        if (org.windy.hologram.action.ActionContext.rconAvailable()) {
            org.windy.hologram.action.ActionContext.executeRcon(tpCmd, pos.server());
            rconOk = true;
        }

        if (rconOk) {
            msg(source, "§a已传送到悬浮字 '" + args[1] + "' (" + pos.server() + ")");
        } else {
            // RCON 不可用，提示手动执行
            msg(source, "§cRCON 未配置，无法自动传送");
            msg(source, "§7请在子服执行: " + tpCmd);
        }
    }

    private void handleInfo(CommandSource source) {
        int totalHolograms = hologramManager.getAllHolograms().size();
        int totalLines = hologramManager.getAllHolograms().stream()
                .mapToInt(Hologram::getLineCount).sum();
        int totalObservers = hologramManager.getAllHolograms().stream()
                .mapToInt(h -> h.getObservers().size()).sum();

        msg(source, "§a=== VelocityHologram 信息 ===");
        msg(source, "§7悬浮字数: §f" + totalHolograms);
        msg(source, "§7总行数: §f" + totalLines);
        msg(source, "§7总观察者: §f" + totalObservers);
        msg(source, "§7可见性更新: §f500ms");
        msg(source, "§7动画更新: §f200ms");
    }

    private void handleAddFlag(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addflag <名称> <flag>");
            msg(source, "§7可用: always_visible, disable_placeholders, disable_actions, disable_updates");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.addFlag(args[2]);
        hologramLoader.save(hologram);
        msg(source, "§a已添加 flag '" + args[2] + "' 到悬浮字 '" + args[1] + "'");
    }

    private void handleRemoveFlag(CommandSource source, String[] args) {
        if (args.length < 3) { msg(source, "§c用法: /holo removeflag <名称> <flag>"); return; }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.removeFlag(args[2]);
        hologramLoader.save(hologram);
        msg(source, "§a已移除 flag '" + args[2] + "' 从悬浮字 '" + args[1] + "'");
    }

    private void handleMove(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;
        if (args.length < 2) {
            msg(source, "§c用法: /holo move <名称>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        UUID playerId = player.getUniqueId();
        PlayerState state = playerTracker.get(playerId);
        if (state == null) {
            msg(source, "§c无法获取你的位置信息");
            return;
        }

        // 更新悬浮字位置
        hologram.setPosition(state.getX(), state.getY(), state.getZ());

        // 刷新显示
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已将悬浮字 '" + args[1] + "' 移动到你的位置");
    }

    private void handleClone(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo clone <名称> <新名称>");
            return;
        }

        Hologram original = getHologramOrWarn(source, args[1]);
        if (original == null) return;

        String newName = args[2];
        if (hologramManager.getHologram(newName) != null) {
            msg(source, Lang.get("holo_already_exists", "%name%", newName));
            return;
        }

        // 克隆悬浮字
        Hologram cloned = hologramManager.createHologram(newName,
                original.getPosition().x(), original.getPosition().y(), original.getPosition().z(),
                original.getPosition().dimension(), original.getPosition().server());

        // 复制配置
        cloned.setViewDistance(original.getViewDistance());
        cloned.setUpdateDistance(original.getUpdateDistance());
        cloned.setLineSpacing(original.getLineSpacing());
        cloned.setUpdateInterval(original.getUpdateInterval());
        cloned.setPermission(original.getPermission());

        // 复制 flags
        for (String flag : original.getFlags()) {
            cloned.addFlag(flag);
        }

        // 复制所有页和行
        for (int pi = 0; pi < original.getPageCount(); pi++) {
            Page origPage = original.getPage(pi);
            if (origPage == null) continue;

            Page clonedPage = (pi == 0) ? cloned.getPage(0) : cloned.addPage();
            if (clonedPage == null) continue;

            for (var line : origPage.getLines()) {
                if (!(line instanceof HologramLine)) continue;
                HologramLine origLine = (HologramLine) line;
                DisplayConfig config = origLine.getDisplayConfig();
                HologramLine newLine = clonedPage.addLine(config);

                // 复制偏移
                newLine.setOffsetY(origLine.getOffsetY());
                newLine.setOffsetX(origLine.getOffsetX());
                newLine.setOffsetZ(origLine.getOffsetZ());

                // 复制点击动作
                if (origLine.getLeftClickAction() != null || origLine.getRightClickAction() != null
                        || origLine.getShiftLeftClickAction() != null || origLine.getShiftRightClickAction() != null) {
                    clonedPage.setLineAction(clonedPage.getLineCount() - 1,
                            origLine.getLeftClickAction(), origLine.getRightClickAction(),
                            origLine.getShiftLeftClickAction(), origLine.getShiftRightClickAction(), clickHandler);
                }
            }
        }

        hologramLoader.save(cloned);
        msg(source, "§a已克隆悬浮字 '" + args[1] + "' 为 '" + newName + "'");
    }

    private void handleCenter(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;
        if (args.length < 2) {
            msg(source, "§c用法: /holo center <名称>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        // 计算区块中心坐标
        double x = Math.floor(hologram.getPosition().x() / 16) * 16 + 8.5;
        double z = Math.floor(hologram.getPosition().z() / 16) * 16 + 8.5;
        double y = hologram.getPosition().y();

        hologram.setPosition(x, y, z);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已将悬浮字 '" + args[1] + "' 居中到区块中心");
    }

    private void handleSetOffset(CommandSource source, String[] args) {
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

    private void handleNear(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;

        double radius = 50.0; // 默认半径
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                msg(source, "§c半径必须是数字");
                return;
            }
        }

        UUID playerId = player.getUniqueId();
        PlayerState state = playerTracker.get(playerId);
        if (state == null) {
            msg(source, "§c无法获取你的位置信息");
            return;
        }

        double radiusSq = radius * radius;
        var nearby = hologramManager.getAllHolograms().stream()
                .filter(h -> {
                    var pos = h.getPosition();
                    if (!pos.server().equals(state.getServer())) return false;
                    if (!pos.dimension().equals(state.getDimension())) return false;
                    double dx = pos.x() - state.getX();
                    double dy = pos.y() - state.getY();
                    double dz = pos.z() - state.getZ();
                    return dx * dx + dy * dy + dz * dz <= radiusSq;
                })
                .sorted(java.util.Comparator.comparingDouble(h -> {
                    var pos = h.getPosition();
                    double dx = pos.x() - state.getX();
                    double dy = pos.y() - state.getY();
                    double dz = pos.z() - state.getZ();
                    return dx * dx + dy * dy + dz * dz;
                }))
                .collect(java.util.stream.Collectors.toList());

        if (nearby.isEmpty()) {
            msg(source, "§7半径 " + fmtCoord(radius) + " 内没有悬浮字");
            return;
        }

        msg(source, "§a=== 附近悬浮字 (半径 " + fmtCoord(radius) + ") ===");
        for (Hologram holo : nearby) {
            var pos = holo.getPosition();
            double dx = pos.x() - state.getX();
            double dy = pos.y() - state.getY();
            double dz = pos.z() - state.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            msg(source, "§e- " + holo.getName()
                    + " §7(" + fmtCoord(pos.x()) + ", " + fmtCoord(pos.y()) + ", " + fmtCoord(pos.z()) + ")"
                    + " §8距离=" + fmtCoord(dist));
        }
    }

    private void handlePermission(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo permission <名称> <权限节点|->");
            msg(source, "§7- 表示清除权限（所有人可见）");
            return;
        }

        Hologram hologram = hologramManager.getHologram(args[1]);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + args[1] + "' 不存在");
            return;
        }

        String perm = args[2];
        if (perm.equals("-")) {
            hologram.setPermission(null);
            hologramLoader.save(hologram);
            msg(source, "§a已清除悬浮字 '" + args[1] + "' 的权限限制");
        } else {
            hologram.setPermission(perm);
            hologramLoader.save(hologram);
            msg(source, "§a已设置悬浮字 '" + args[1] + "' 的权限为: " + perm);
        }
    }

    // ===== 新增命令 =====

    private void handleRename(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo rename <旧名称> <新名称>");
            return;
        }
        String oldName = args[1];
        String newName = args[2];
        if (hologramManager.getHologram(oldName) == null) {
            msg(source, Lang.get("holo_not_found", "%name%", oldName));
            return;
        }
        if (hologramManager.getHologram(newName) != null) {
            msg(source, Lang.get("holo_already_exists", "%name%", newName));
            return;
        }
        // 删除旧文件
        hologramLoader.delete(oldName);
        // 重命名
        if (hologramManager.renameHologram(oldName, newName)) {
            hologramLoader.save(hologramManager.getHologram(newName));
            msg(source, "§a已将悬浮字 '" + oldName + "' 重命名为 '" + newName + "'");
        } else {
            msg(source, "§c重命名失败");
        }
    }

    private void handleEnable(CommandSource source, String[] args, boolean enabled) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo " + (enabled ? "enable" : "disable") + " <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.setEnabled(enabled);
        hologramLoader.save(hologram);
        msg(source, "§a已" + (enabled ? "启用" : "禁用") + "悬浮字 '" + args[1] + "'");
    }

    private void handleAlign(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        if (args.length < 3) {
            msg(source, "§c用法: /holo align <名称> <x|y|z|center>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        String axis = args[2].toLowerCase();
        double x = hologram.getPosition().x();
        double y = hologram.getPosition().y();
        double z = hologram.getPosition().z();

        switch (axis) {
            case "x":
                x = Math.floor(x) + 0.5;
                break;
            case "y":
                y = Math.floor(y) + 0.5;
                break;
            case "z":
                z = Math.floor(z) + 0.5;
                break;
            case "center":
                x = Math.floor(x) + 0.5;
                z = Math.floor(z) + 0.5;
                break;
            default:
                msg(source, "§c轴必须是 x、y、z 或 center");
                return;
        }

        hologram.setPosition(x, y, z);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已对齐悬浮字 '" + args[1] + "' 到 " + axis + " 轴");
    }

    private void handleSetFacing(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        if (args.length < 2) {
            msg(source, "§c用法: /holo setfacing <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        Player player = (Player) source;
        PlayerState state = playerTracker.get(player.getUniqueId());
        if (state == null) {
            msg(source, "§c无法获取你的朝向信息");
            return;
        }

        hologram.setFacing(state.getYaw(), state.getPitch());
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已设置悬浮字 '" + args[1] + "' 的朝向 (yaw=" +
            String.format("%.1f", state.getYaw()) + ", pitch=" +
            String.format("%.1f", state.getPitch()) + ")");
    }

    private void handleInsertPage(CommandSource source, String[] args) {
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

    private void handleSwapPages(CommandSource source, String[] args) {
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

    // ===== Line 子命令 =====

    private void handleLineCommand(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo line <add|set|remove|insert|swap|align|height|offsetx|offsetz|setpermission|addflag|removeflag|setfacing> ...");
            return;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add":
                handleLineAdd(source, args);
                break;
            case "set":
                handleLineSet(source, args);
                break;
            case "remove":
                handleLineRemove(source, args);
                break;
            case "insert":
                handleLineInsert(source, args);
                break;
            case "swap":
                handleLineSwap(source, args);
                break;
            case "align":
                handleLineAlign(source, args);
                break;
            case "height":
                handleLineHeight(source, args);
                break;
            case "offsetx":
                handleLineOffsetX(source, args);
                break;
            case "offsetz":
                handleLineOffsetZ(source, args);
                break;
            case "setpermission":
                handleLineSetPermission(source, args);
                break;
            case "addflag":
                handleLineAddFlag(source, args);
                break;
            case "removeflag":
                handleLineRemoveFlag(source, args);
                break;
            case "setfacing":
                handleLineSetFacing(source, args);
                break;
            default:
                msg(source, "§c未知的行子命令: " + sub);
                break;
        }
    }

    private void handleLineAdd(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo line add <名称> <文本>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        String text = joinArgs(args, 3);
        hologram.addLine(text);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已添加行到悬浮字 '" + args[2] + "'");
    }

    private void handleLineSet(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line set <名称> <行号> <文本>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            String text = joinArgs(args, 4);
            hologram.setLine(index, text);
            hologram.update();
            hologramLoader.save(hologram);
            msg(source, "§a已更新行 " + index);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    private void handleLineRemove(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo line remove <名称> <行号>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            hologram.removeLine(index);
            hologram.refresh();
            hologramLoader.save(hologram);
            msg(source, "§a已删除行 " + index);
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    private void handleLineInsert(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line insert <名称> <行号> <文本>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int index = Integer.parseInt(args[3]);
            String text = joinArgs(args, 4);
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

    private void handleLineSwap(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo line swap <名称> <行号A> <行号B>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int a = Integer.parseInt(args[3]);
            int b = Integer.parseInt(args[4]);
            if (hologram.swapLines(a, b)) {
                hologram.refresh();
                hologramLoader.save(hologram);
                msg(source, "§a已交换行 " + a + " 和 " + b);
            } else {
                msg(source, "§c行号无效");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    private void handleLineAlign(CommandSource source, String[] args) {
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
                case "left":
                    offset = -0.5;
                    break;
                case "center":
                    offset = 0;
                    break;
                case "right":
                    offset = 0.5;
                    break;
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

    private void handleLineHeight(CommandSource source, String[] args) {
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

    private void handleLineOffsetX(CommandSource source, String[] args) {
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

    private void handleLineOffsetZ(CommandSource source, String[] args) {
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

    private void handleLineSetPermission(CommandSource source, String[] args) {
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

    private void handleLineAddFlag(CommandSource source, String[] args) {
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

    private void handleLineRemoveFlag(CommandSource source, String[] args) {
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

    private void handleLineSetFacing(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        if (args.length < 4) {
            msg(source, "§c用法: /holo line setfacing <名称> <行号>");
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
            // 行朝向暂不实现，需要 Display 实体支持
            msg(source, "§e行朝向功能暂未实现");
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    // ===== Page 子命令 =====

    private void handlePageCommand(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo page <add|insert|remove|swap|switch|addaction|removeaction|clearactions> ...");
            return;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add":
                handlePageAdd(source, args);
                break;
            case "insert":
                handlePageInsert(source, args);
                break;
            case "remove":
                handlePageRemove(source, args);
                break;
            case "swap":
                handlePageSwap(source, args);
                break;
            case "switch":
                handlePageSwitch(source, args);
                break;
            case "addaction":
                handlePageAddAction(source, args);
                break;
            case "removeaction":
                handlePageRemoveAction(source, args);
                break;
            case "clearactions":
                handlePageClearActions(source, args);
                break;
            default:
                msg(source, "§c未知的页面子命令: " + sub);
                break;
        }
    }

    private void handlePageAdd(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo page add <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        hologram.addPage();
        hologramLoader.save(hologram);
        msg(source, "§a已添加第 " + hologram.getPageCount() + " 页");
    }

    private void handlePageInsert(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo page insert <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[3]);
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

    private void handlePageRemove(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo page remove <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[3]);
            if (hologram.removePage(page)) {
                hologramLoader.save(hologram);
                msg(source, "§a已删除第 " + page + " 页");
            } else {
                msg(source, "§c无法删除（至少保留 1 页，或页码无效）");
            }
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    private void handlePageSwap(CommandSource source, String[] args) {
        if (args.length < 5) {
            msg(source, "§c用法: /holo page swap <名称> <页码A> <页码B>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int a = Integer.parseInt(args[3]);
            int b = Integer.parseInt(args[4]);
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

    private void handlePageSwitch(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo page switch <名称> <页码>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[2]);
        if (hologram == null) return;
        try {
            int page = Integer.parseInt(args[3]);
            hologram.setEditPageIndex(page);
            msg(source, "§a编辑页已切换到第 " + page + " 页");
        } catch (NumberFormatException e) {
            msg(source, "§c页码必须是数字");
        }
    }

    private void handlePageAddAction(CommandSource source, String[] args) {
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
        var action = org.windy.hologram.action.ActionFactory.parse(actionStr);
        if (action == null) {
            msg(source, "§c无效的动作: " + actionStr);
            return;
        }
        page.addAction(clickType, action);
        hologramLoader.save(hologram);
        msg(source, "§a已添加页面动作: " + clickType + " → " + actionStr);
    }

    private void handlePageRemoveAction(CommandSource source, String[] args) {
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

    private void handlePageClearActions(CommandSource source, String[] args) {
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

    // ===== 权限检查 =====

    private boolean checkPerm(CommandSource source, String permission) {
        if (source.hasPermission("velocityhologram.command.admin")) return true;
        if (source.hasPermission(permission)) return true;
        msg(source, Lang.get("no_permission", "%perm%", permission));
        return false;
    }

    // ===== 帮助 =====

    private void sendHelp(CommandSource source) {
        msg(source, "§a=== VelocityHologram v1.2.0 ===");
        msg(source, "§6--- 基础 ---");
        msg(source, "§e/holo create <名称> [文字] §7- 创建悬浮字");
        msg(source, "§e/holo delete <名称> §7- 删除悬浮字");
        msg(source, "§e/holo rename <旧名> <新名> §7- 重命名");
        msg(source, "§e/holo enable/disable <名称> §7- 启用/禁用");
        msg(source, "§e/holo list §7- 列出所有");
        msg(source, "§e/holo save [名称] §7- 保存");
        msg(source, "§e/holo reload §7- 重载配置");
        msg(source, "§6--- 行操作（/holo line ...）---");
        msg(source, "§e/holo line add/set/remove/insert/swap §7- 行操作");
        msg(source, "§e/holo line align <名称> <行号> <left|center|right> §7- 行对齐");
        msg(source, "§e/holo line height <名称> <行号> <高度> §7- 行高");
        msg(source, "§e/holo line offsetx/offsetz §7- 行偏移");
        msg(source, "§e/holo line setpermission §7- 行权限");
        msg(source, "§e/holo line addflag/removeflag §7- 行Flag");
        msg(source, "§6--- 页面（/holo page ...）---");
        msg(source, "§e/holo page add/insert/remove/swap/switch §7- 页面操作");
        msg(source, "§e/holo page addaction/removeaction/clearactions §7- 页面动作");
        msg(source, "§6--- 位置 ---");
        msg(source, "§e/holo move/movehere <名称> §7- 移动到你的位置");
        msg(source, "§e/holo clone <名称> <新名称> §7- 克隆悬浮字");
        msg(source, "§e/holo center <名称> §7- 居中到区块中心");
        msg(source, "§e/holo align <名称> <x|y|z|center> §7- 对齐");
        msg(source, "§e/holo setfacing <名称> §7- 设置朝向");
        msg(source, "§e/holo near [半径] §7- 列出附近悬浮字");
        msg(source, "§6--- 工具 ---");
        msg(source, "§e/holo tp <名称> §7- RCON 传送");
        msg(source, "§e/holo debug <名称> §7- 调试信息");
        msg(source, "§e/holo info §7- 统计信息");
        msg(source, "§e/holo permission <名称> <节点> §7- 设置权限");
        msg(source, "§e/holo addflag/removeflag <名称> <flag> §7- Flag");
    }

    // ===== 工具方法 =====

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

    private static String fmtCoord(double v) {
        return String.format("%.1f", v);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static void msg(CommandSource source, String legacy) {
        source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}
