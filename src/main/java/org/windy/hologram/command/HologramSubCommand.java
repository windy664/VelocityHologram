package org.windy.hologram.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerTracker;
import org.windy.hologram.tracker.PlayerState;

import java.util.UUID;

/**
 * 悬浮字相关子命令。
 * <p>处理 create、delete、rename、enable/disable、move、clone 等命令。
 */
public class HologramSubCommand {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;
    private final HologramLoader hologramLoader;

    public HologramSubCommand(HologramManager hologramManager, PlayerTracker playerTracker,
                              HologramLoader hologramLoader) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.hologramLoader = hologramLoader;
    }

    public void handleCreate(CommandSource source, String[] args) {
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
            playerTracker.register(playerId, player.getUsername());
            state = playerTracker.get(playerId);
        }
        if (state == null) {
            msg(source, "§c无法获取你的位置信息");
            return;
        }

        boolean posReady = !(state.getX() == 0 && state.getY() == 0 && state.getZ() == 0);
        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        Hologram hologram = hologramManager.createHologram(name, state.getX(), state.getY(), state.getZ(),
                state.getDimension(), server);

        if (args.length >= 3) {
            String text = joinArgs(args, 2);
            hologram.addLine(text);
        }
        hologramLoader.save(hologram);

        if (posReady) {
            hologram.showTo(player.getUniqueId());
            msg(source, "§a已创建悬浮字 '" + name + "' 在你的位置");
        } else {
            msg(source, "§a已创建悬浮字 '" + name + "'，移动一下即可看到");
        }
    }

    public void handleDelete(CommandSource source, String[] args) {
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

    public void handleRename(CommandSource source, String[] args) {
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
        hologramLoader.delete(oldName);
        if (hologramManager.renameHologram(oldName, newName)) {
            hologramLoader.save(hologramManager.getHologram(newName));
            msg(source, "§a已将悬浮字 '" + oldName + "' 重命名为 '" + newName + "'");
        } else {
            msg(source, "§c重命名失败");
        }
    }

    public void handleEnable(CommandSource source, String[] args, boolean enabled) {
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

    public void handleMove(CommandSource source, String[] args) {
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

        hologram.setPosition(state.getX(), state.getY(), state.getZ());
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已将悬浮字 '" + args[1] + "' 移动到你的位置");
    }

    public void handleClone(CommandSource source, String[] args) {
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

        Hologram cloned = hologramManager.createHologram(newName,
                original.getPosition().x(), original.getPosition().y(), original.getPosition().z(),
                original.getPosition().dimension(), original.getPosition().server());

        cloned.setViewDistance(original.getViewDistance());
        cloned.setUpdateDistance(original.getUpdateDistance());
        cloned.setLineSpacing(original.getLineSpacing());
        cloned.setUpdateInterval(original.getUpdateInterval());
        cloned.setPermission(original.getPermission());
        cloned.setDownOrigin(original.isDownOrigin());
        cloned.setAlwaysFacePlayer(original.isAlwaysFacePlayer());

        for (String flag : original.getFlags()) {
            cloned.addFlag(flag);
        }

        for (int pi = 0; pi < original.getPageCount(); pi++) {
            var origPage = original.getPage(pi);
            if (origPage == null) continue;

            var clonedPage = (pi == 0) ? cloned.getPage(0) : cloned.addPage();
            if (clonedPage == null) continue;

            for (var line : origPage.getLines()) {
                if (!(line instanceof org.windy.hologram.hologram.HologramLine)) continue;
                var origLine = (org.windy.hologram.hologram.HologramLine) line;
                var config = origLine.getDisplayConfig();
                var newLine = clonedPage.addLine(config);

                newLine.setOffsetY(origLine.getOffsetY());
                newLine.setOffsetX(origLine.getOffsetX());
                newLine.setOffsetZ(origLine.getOffsetZ());
                newLine.setLineHeight(origLine.getLineHeight());
                newLine.setPermission(origLine.getPermission());

                for (String flag : origLine.getFlags()) {
                    newLine.addFlag(flag);
                }
            }
        }

        hologramLoader.save(cloned);
        msg(source, "§a已克隆悬浮字 '" + args[1] + "' 为 '" + newName + "'");
    }

    public void handleCenter(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        if (args.length < 2) {
            msg(source, "§c用法: /holo center <名称>");
            return;
        }

        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;

        double x = Math.floor(hologram.getPosition().x() / 16) * 16 + 8.5;
        double z = Math.floor(hologram.getPosition().z() / 16) * 16 + 8.5;
        double y = hologram.getPosition().y();

        hologram.setPosition(x, y, z);
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已将悬浮字 '" + args[1] + "' 居中到区块中心");
    }

    public void handleAlign(CommandSource source, String[] args) {
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
            case "x": x = Math.floor(x) + 0.5; break;
            case "y": y = Math.floor(y) + 0.5; break;
            case "z": z = Math.floor(z) + 0.5; break;
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

    public void handleSetFacing(CommandSource source, String[] args) {
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
        msg(source, "§a已设置悬浮字 '" + args[1] + "' 的朝向");
    }

    public void handleDownOrigin(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo downorigin <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.setDownOrigin(!hologram.isDownOrigin());
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已" + (hologram.isDownOrigin() ? "启用" : "禁用") + "悬浮字 '" + args[1] + "' 的向下生长");
    }

    public void handleAlwaysFace(CommandSource source, String[] args) {
        if (args.length < 2) {
            msg(source, "§c用法: /holo alwaysface <名称>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.setAlwaysFacePlayer(!hologram.isAlwaysFacePlayer());
        hologram.refresh();
        hologramLoader.save(hologram);
        msg(source, "§a已" + (hologram.isAlwaysFacePlayer() ? "启用" : "禁用") + "悬浮字 '" + args[1] + "' 的始终面向玩家");
    }

    public void handlePermission(CommandSource source, String[] args) {
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

    public void handleAddFlag(CommandSource source, String[] args) {
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

    public void handleRemoveFlag(CommandSource source, String[] args) {
        if (args.length < 3) {
            msg(source, "§c用法: /holo removeflag <名称> <flag>");
            return;
        }
        Hologram hologram = getHologramOrWarn(source, args[1]);
        if (hologram == null) return;
        hologram.removeFlag(args[2]);
        hologramLoader.save(hologram);
        msg(source, "§a已移除 flag '" + args[2] + "' 从悬浮字 '" + args[1] + "'");
    }

    public void handleList(CommandSource source) {
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

    public void handleNear(CommandSource source, String[] args) {
        if (!(source instanceof Player)) {
            msg(source, Lang.get("player_only"));
            return;
        }
        Player player = (Player) source;

        double radius = 50.0;
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

    private Hologram getHologramOrWarn(CommandSource source, String name) {
        Hologram hologram = hologramManager.getHologram(name);
        if (hologram == null) {
            msg(source, Lang.get("holo_not_found", "%name%", name));
        }
        return hologram;
    }

    private static String joinArgs(String[] args, int from) {
        return String.join(" ", java.util.List.of(args).subList(from, args.length));
    }

    private static String fmtCoord(double v) {
        return String.format("%.1f", v);
    }

    static void msg(CommandSource source, String legacy) {
        source.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy));
    }
}
