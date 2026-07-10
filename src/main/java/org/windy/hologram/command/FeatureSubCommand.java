package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.util.UUID;

/**
 * 功能相关子命令。
 * <p>处理 hide、show、convert 等命令。
 */
public class FeatureSubCommand {

    private final HologramManager hologramManager;
    private final HologramLoader hologramLoader;
    private final com.velocitypowered.api.proxy.ProxyServer proxy;
    private final java.nio.file.Path dataDir;

    public FeatureSubCommand(HologramManager hologramManager, HologramLoader hologramLoader,
                             com.velocitypowered.api.proxy.ProxyServer proxy, java.nio.file.Path dataDir) {
        this.hologramManager = hologramManager;
        this.hologramLoader = hologramLoader;
        this.proxy = proxy;
        this.dataDir = dataDir;
    }

    public void handleHidePlayer(CommandSource source, String[] args, boolean hide) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo " + (hide ? "hide" : "show") + " <名称> <玩家名>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        String playerName = args[2];
        var playerOpt = proxy.getPlayer(playerName);
        if (playerOpt.isEmpty()) {
            msg(source, "§c玩家 '" + playerName + "' 不在线");
            return;
        }

        UUID playerId = playerOpt.get().getUniqueId();
        if (hide) {
            hologram.setHidePlayer(playerId);
            hologram.hideFrom(playerId);
            msg(source, "§a已对玩家 '" + playerName + "' 隐藏悬浮字 '" + args[1] + "'");
        } else {
            hologram.removeHidePlayer(playerId);
            hologram.showTo(playerId);
            msg(source, "§a已对玩家 '" + playerName + "' 显示悬浮字 '" + args[1] + "'");
        }
        hologramLoader.save(hologram);
    }

    public void handleConvert(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo convert <decentholograms|holographicdisplays> <路径>");
            return;
        }

        String type = args[1].toLowerCase();
        String path = args[2];

        org.windy.hologram.convertor.IConvertor convertor;
        switch (type) {
            case "decentholograms":
            case "dh":
                convertor = new org.windy.hologram.convertor.DecentHologramsConvertor(dataDir);
                break;
            case "holographicdisplays":
            case "hd":
                convertor = new org.windy.hologram.convertor.HolographicDisplaysConvertor(dataDir);
                break;
            default:
                msg(source, "§c不支持的类型: " + type);
                msg(source, "§7支持: decentholograms (dh), holographicdisplays (hd)");
                return;
        }

        msg(source, "§e正在从 " + convertor.getName() + " 转换...");
        if (convertor.convert(path)) {
            msg(source, "§a转换完成！");
        } else {
            msg(source, "§c转换失败，请检查路径和配置文件");
        }
    }

    public void handleSave(CommandSource source, String[] args) {
        if (args.length < 2) {
            for (var holo : hologramManager.getAllHolograms()) {
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

    public void handleReload(CommandSource source) {
        var names = hologramManager.getAllHolograms().stream()
                .map(Hologram::getName).collect(java.util.stream.Collectors.toList());
        for (String name : names) {
            hologramManager.removeHologram(name);
        }

        hologramLoader.loadAll(hologramManager);
        msg(source, "§a配置已重新加载，共 " + hologramManager.getAllHolograms().size() + " 个悬浮字");
    }

    private Hologram getHologramOrWarn(CommandSource source, String name) {
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, Lang.get("holo_not_found", "%name%", name));
        }
        return hologram;
    }

    private static void msg(CommandSource source, String legacy) {
        source.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}
