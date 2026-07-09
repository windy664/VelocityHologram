package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerTracker;
import org.windy.hologram.tracker.PlayerState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * /holo 命令实现。
 * <p>支持创建、删除、添加行、列出、保存等操作。
 */
public class HologramCommand implements SimpleCommand {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;
    private final HologramLoader hologramLoader;

    public HologramCommand(HologramManager hologramManager, PlayerTracker playerTracker,
                           HologramLoader hologramLoader) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.hologramLoader = hologramLoader;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(source, args);
            case "delete" -> handleDelete(source, args);
            case "addline" -> handleAddLine(source, args);
            case "setline" -> handleSetLine(source, args);
            case "removeline" -> handleRemoveLine(source, args);
            case "list" -> handleList(source);
            case "save" -> handleSave(source, args);
            case "reload" -> handleReload(source);
            default -> sendHelp(source);
        }
    }

    private void handleCreate(CommandSource source, String[] args) {
        if (!(source instanceof Player player)) {
            msg(source, "§c只有玩家可以使用此命令");
            return;
        }

        if (args.length < 2) {
            msg(source, "§c用法: /holo create <名称>");
            return;
        }

        String name = args[1];
        if (hologramManager.getHologram(name) != null) {
            msg(source, "§c悬浮字 '" + name + "' 已存在");
            return;
        }

        // 获取玩家当前位置
        UUID playerId = player.getUniqueId();
        PlayerState state = playerTracker.get(playerId);
        if (state == null) {
            msg(source, "§c无法获取你的位置信息");
            return;
        }

        // 获取玩家当前服务器
        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        Hologram hologram = hologramManager.createHologram(
                name,
                state.getX(), state.getY(), state.getZ(),
                state.getDimension(),
                server
        );

        msg(source, "§a已创建悬浮字 '" + name + "' 在你的位置");
    }

    private void handleDelete(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo delete <名称>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + name + "' 不存在");
            return;
        }

        hologramManager.removeHologram(name);
        msg(source, "§a已删除悬浮字 '" + name + "'");
    }

    private void handleAddLine(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo addline <名称> <文本>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + name + "' 不存在");
            return;
        }

        // 合并剩余参数作为文本
        String text = String.join(" ", List.of(args).subList(2, args.length));
        hologram.addLine(text);
        hologram.update();

        msg(source, "§a已添加行到悬浮字 '" + name + "'");
    }

    private void handleSetLine(CommandSource source, String[] args) {
        if (args.length < 4) {
            msg(source, "§c用法: /holo setline <名称> <行号> <文本>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + name + "' 不存在");
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            String text = String.join(" ", List.of(args).subList(3, args.length));
            hologram.setLine(index, text);
            hologram.update();
            msg(source, "§a已更新悬浮字 '" + name + "' 第 " + index + " 行");
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    private void handleRemoveLine(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo removeline <名称> <行号>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + name + "' 不存在");
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            hologram.removeLine(index);
            hologram.update();
            msg(source, "§a已删除悬浮字 '" + name + "' 第 " + index + " 行");
        } catch (NumberFormatException e) {
            msg(source, "§c行号必须是数字");
        }
    }

    private void handleList(CommandSource source) {
        var holograms = hologramManager.getAllHolograms();
        if (holograms.isEmpty()) {
            msg(source, "§7当前没有悬浮字");
            return;
        }

        msg(source, "§a=== 悬浮字列表 ===");
        for (Hologram hologram : holograms) {
            msg(source, "§e- " + hologram.getName()
                    + " §7(" + hologram.getPosition().x()
                    + ", " + hologram.getPosition().y()
                    + ", " + hologram.getPosition().z()
                    + ") [" + hologram.getPosition().server() + "]");
        }
    }

    private void handleSave(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo save <名称>");
            return;
        }

        String name = args[1];
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, "§c悬浮字 '" + name + "' 不存在");
            return;
        }

        hologramLoader.save(hologram);
        msg(source, "§a已保存悬浮字 '" + name + "' 到配置文件");
    }

    private void handleReload(CommandSource source) {
        // 重新加载所有悬浮字
        for (Hologram hologram : hologramManager.getAllHolograms()) {
            hologram.destroy();
        }
        hologramLoader.loadAll(hologramManager);
        msg(source, "§a配置已重新加载，共 " + hologramManager.getAllHolograms().size() + " 个悬浮字");
    }

    private void sendHelp(CommandSource source) {
        msg(source, "§a=== VelocityHologram 命令 ===");
        msg(source, "§e/holo create <名称> §7- 在你位置创建悬浮字");
        msg(source, "§e/holo delete <名称> §7- 删除悬浮字");
        msg(source, "§e/holo addline <名称> <文本> §7- 添加一行");
        msg(source, "§e/holo setline <名称> <行号> <文本> §7- 设置某行");
        msg(source, "§e/holo removeline <名称> <行号> §7- 删除某行");
        msg(source, "§e/holo list §7- 列出所有悬浮字");
        msg(source, "§e/holo save <名称> §7- 保存悬浮字到配置文件");
        msg(source, "§e/holo reload §7- 重新加载配置");
    }

    private static void msg(CommandSource source, String legacy) {
        source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}
