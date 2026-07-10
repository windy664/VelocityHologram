package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerTracker;

import java.nio.file.Path;
import java.util.List;

/**
 * /holo 命令实现。
 * <p>作为命令分发器，将命令委托给对应的子命令处理器。
 *
 * <p>权限节点：
 * <ul>
 *   <li>velocityhologram.command.create - 创建悬浮字</li>
 *   <li>velocityhologram.command.delete - 删除悬浮字</li>
 *   <li>velocityhologram.command.edit - 编辑悬浮字</li>
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
    private final ProxyServer proxy;
    private final Path dataDir;

    // 子命令处理器
    private final HologramSubCommand hologramSub;
    private final LineSubCommand lineSub;
    private final PageSubCommand pageSub;
    private final FeatureSubCommand featureSub;

    public HologramCommand(HologramManager hologramManager, PlayerTracker playerTracker,
                           HologramLoader hologramLoader, ClickHandler clickHandler,
                           ProxyServer proxy, Path dataDir) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.hologramLoader = hologramLoader;
        this.clickHandler = clickHandler;
        this.proxy = proxy;
        this.dataDir = dataDir;

        // 初始化子命令处理器
        this.hologramSub = new HologramSubCommand(hologramManager, playerTracker, hologramLoader);
        this.lineSub = new LineSubCommand(hologramManager, playerTracker, hologramLoader);
        this.pageSub = new PageSubCommand(hologramManager, hologramLoader);
        this.featureSub = new FeatureSubCommand(hologramManager, hologramLoader, proxy, dataDir);
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
            // 悬浮字操作
            case "create":
                if (!checkPerm(source, "velocityhologram.command.create")) return;
                hologramSub.handleCreate(source, args);
                break;
            case "delete":
                if (!checkPerm(source, "velocityhologram.command.delete")) return;
                hologramSub.handleDelete(source, args);
                break;
            case "rename":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                hologramSub.handleRename(source, args);
                break;
            case "enable":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleEnable(source, args, true);
                break;
            case "disable":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleEnable(source, args, false);
                break;
            case "move":
            case "movehere":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleMove(source, args);
                break;
            case "clone":
                if (!checkPerm(source, "velocityhologram.command.create")) return;
                hologramSub.handleClone(source, args);
                break;
            case "center":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleCenter(source, args);
                break;
            case "align":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleAlign(source, args);
                break;
            case "setfacing":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleSetFacing(source, args);
                break;
            case "downorigin":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleDownOrigin(source, args);
                break;
            case "alwaysface":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                hologramSub.handleAlwaysFace(source, args);
                break;
            case "permission":
            case "perm":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                hologramSub.handlePermission(source, args);
                break;
            case "addflag":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                hologramSub.handleAddFlag(source, args);
                break;
            case "removeflag":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                hologramSub.handleRemoveFlag(source, args);
                break;
            case "list":
                if (!checkPerm(source, "velocityhologram.command.list")) return;
                hologramSub.handleList(source);
                break;
            case "near":
                if (!checkPerm(source, "velocityhologram.command.list")) return;
                hologramSub.handleNear(source, args);
                break;

            // 行操作
            case "addline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddLine(source, args);
                break;
            case "setline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleSetLine(source, args);
                break;
            case "removeline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleRemoveLine(source, args);
                break;
            case "insertline":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleInsertLine(source, args);
                break;
            case "swaplines":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleSwapLines(source, args);
                break;
            case "additem":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddItem(source, args);
                break;
            case "addblock":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddBlock(source, args);
                break;
            case "addentity":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddEntity(source, args);
                break;
            case "addhead":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddHead(source, args, false);
                break;
            case "addsmallhead":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddHead(source, args, true);
                break;
            case "addicon":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleAddIcon(source, args);
                break;
            case "setoffset":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                lineSub.handleSetOffset(source, args);
                break;
            case "line":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handleLineCommand(source, args);
                break;

            // 页面操作
            case "addpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                pageSub.handleAddPage(source, args);
                break;
            case "insertpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                pageSub.handleInsertPage(source, args);
                break;
            case "removepage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                pageSub.handleRemovePage(source, args);
                break;
            case "swappages":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                pageSub.handleSwapPages(source, args);
                break;
            case "switchpage":
            case "editpage":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                pageSub.handleSwitchPage(source, args);
                break;
            case "page":
                if (!checkPerm(source, "velocityhologram.command.edit")) return;
                handlePageCommand(source, args);
                break;

            // 功能操作
            case "hide":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                featureSub.handleHidePlayer(source, args, true);
                break;
            case "show":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                featureSub.handleHidePlayer(source, args, false);
                break;
            case "convert":
                if (!checkPerm(source, "velocityhologram.command.admin")) return;
                featureSub.handleConvert(source, args);
                break;
            case "save":
                if (!checkPerm(source, "velocityhologram.command.save")) return;
                featureSub.handleSave(source, args);
                break;
            case "reload":
                if (!checkPerm(source, "velocityhologram.command.reload")) return;
                featureSub.handleReload(source);
                break;

            // 调试
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

            default:
                sendHelp(source);
                break;
        }
    }

    private void handleLineCommand(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo line <add|set|remove|insert|swap|align|height|offsetx|offsetz|setpermission|addflag|removeflag> ...");
            return;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add": lineSub.handleAddLine(source, args); break;
            case "set": lineSub.handleSetLine(source, args); break;
            case "remove": lineSub.handleRemoveLine(source, args); break;
            case "insert": lineSub.handleInsertLine(source, args); break;
            case "swap": lineSub.handleSwapLines(source, args); break;
            case "align": lineSub.handleLineAlign(source, args); break;
            case "height": lineSub.handleLineHeight(source, args); break;
            case "offsetx": lineSub.handleLineOffsetX(source, args); break;
            case "offsetz": lineSub.handleLineOffsetZ(source, args); break;
            case "setpermission": lineSub.handleLineSetPermission(source, args); break;
            case "addflag": lineSub.handleLineAddFlag(source, args); break;
            case "removeflag": lineSub.handleLineRemoveFlag(source, args); break;
            default: msg(source, "§c未知的行子命令: " + sub); break;
        }
    }

    private void handlePageCommand(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo page <add|insert|remove|swap|switch|addaction|removeaction|clearactions> ...");
            return;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add": pageSub.handleAddPage(source, args); break;
            case "insert": pageSub.handleInsertPage(source, args); break;
            case "remove": pageSub.handleRemovePage(source, args); break;
            case "swap": pageSub.handleSwapPages(source, args); break;
            case "switch": pageSub.handleSwitchPage(source, args); break;
            case "addaction": pageSub.handlePageAddAction(source, args); break;
            case "removeaction": pageSub.handlePageRemoveAction(source, args); break;
            case "clearactions": pageSub.handlePageClearActions(source, args); break;
            default: msg(source, "§c未知的页面子命令: " + sub); break;
        }
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
        msg(source, "§7向下生长: §f" + (hologram.isDownOrigin() ? "是" : "否"));
        msg(source, "§7始终面向: §f" + (hologram.isAlwaysFacePlayer() ? "是" : "否"));

        for (int pi = 0; pi < hologram.getPageCount(); pi++) {
            var page = hologram.getPage(pi);
            if (page == null) continue;
            msg(source, "  §6--- 第 " + pi + " 页 ---");
            for (int i = 0; i < page.getLineCount(); i++) {
                var line = page.getLine(i);
                if (line != null) {
                    msg(source, "    §e[" + i + "] §7类型=" + line.getEntityType().toConfig()
                            + " §f" + truncate(line.getText(), 40)
                            + (line.getAnimation() != null ? " §d[动画]" : "")
                            + (line.getLeftClickAction() != null ? " §a[L]" : "")
                            + (line.getRightClickAction() != null ? " §b[R]" : ""));
                }
            }
        }
    }

    private void handleTeleport(CommandSource source, String[] args) {
        if (!(source instanceof com.velocitypowered.api.proxy.Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
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
        String tpCmd = "execute in " + pos.dimension() + " run tp "
                + playerName + " " + pos.x() + " " + pos.y() + " " + pos.z();

        boolean rconOk = false;
        if (org.windy.hologram.action.ActionContext.rconAvailable()) {
            org.windy.hologram.action.ActionContext.executeRcon(tpCmd, pos.server());
            rconOk = true;
        }

        if (rconOk) {
            msg(source, "§a已传送到悬浮字 '" + args[1] + "' (" + pos.server() + ")");
        } else {
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
        msg(source, "§7版本: §f1.2.0");
    }

    // ===== Tab 补全 =====

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "addline", "additem", "addblock", "addentity",
            "addhead", "addsmallhead", "addicon", "setline", "removeline", "insertline", "swaplines",
            "addpage", "removepage", "switchpage", "editpage", "insertpage", "swappages",
            "addflag", "removeflag",
            "move", "movehere", "clone", "center", "setoffset", "near",
            "rename", "enable", "disable", "align", "setfacing",
            "downorigin", "alwaysface", "hide", "show", "convert",
            "line", "page",
            "list", "save", "reload", "debug", "tp", "info", "permission", "perm"
    );

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(java.util.stream.Collectors.toList());
        }

        String cmd = args[0].toLowerCase();
        String prefix = args[args.length - 1].toLowerCase();

        // 大部分命令的第二个参数是悬浮字名称
        if (args.length == 2) {
            switch (cmd) {
                case "delete": case "addline": case "setline": case "removeline":
                case "insertline": case "swaplines": case "save": case "debug": case "tp":
                case "addpage": case "removepage": case "switchpage": case "editpage":
                case "insertpage": case "swappages":
                case "removeflag": case "permission": case "perm":
                case "additem": case "addblock": case "addentity":
                case "addhead": case "addsmallhead": case "addicon":
                case "setoffset": case "addflag":
                case "move": case "movehere": case "clone": case "center":
                case "rename": case "enable": case "disable":
                case "align": case "setfacing": case "downorigin": case "alwaysface":
                case "hide": case "show":
                    return hologramManager.getAllHolograms().stream()
                            .map(Hologram::getName)
                            .filter(n -> n.toLowerCase().startsWith(prefix))
                            .collect(java.util.stream.Collectors.toList());
            }
        }

        return List.of();
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
        msg(source, "§e/holo downorigin <名称> §7- 切换向下生长");
        msg(source, "§e/holo alwaysface <名称> §7- 切换始终面向");
        msg(source, "§e/holo near [半径] §7- 列出附近悬浮字");
        msg(source, "§6--- 工具 ---");
        msg(source, "§e/holo tp <名称> §7- RCON 传送");
        msg(source, "§e/holo debug <名称> §7- 调试信息");
        msg(source, "§e/holo info §7- 统计信息");
        msg(source, "§e/holo permission <名称> <节点> §7- 设置权限");
        msg(source, "§e/holo addflag/removeflag <名称> <flag> §7- Flag");
        msg(source, "§e/holo hide/show <名称> <玩家> §7- 每玩家可见性");
        msg(source, "§e/holo convert <dh|hd> <路径> §7- 从其他插件转换");
    }

    // ===== 工具方法 =====

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
