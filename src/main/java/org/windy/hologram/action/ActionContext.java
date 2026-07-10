package org.windy.hologram.action;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.windy.hologram.rcon.RconPool;

import java.util.UUID;

/**
 * 动作执行上下文。
 * <p>持有 ProxyServer 和 RconPool 引用，供 Action 实现调用。
 */
public class ActionContext {

    private static ProxyServer proxy;
    private static RconPool rconPool;

    public static void init(ProxyServer proxy) {
        ActionContext.proxy = proxy;
    }

    public static ProxyServer getProxy() {
        return proxy;
    }

    public static void setRconPool(RconPool pool) {
        ActionContext.rconPool = pool;
    }

    /**
     * RCON 是否可用。
     */
    public static boolean rconAvailable() {
        return rconPool != null;
    }

    /**
     * 以玩家身份执行命令。
     */
    public static void executeAsPlayer(UUID playerId, String command) {
        if (proxy == null) return;
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player != null) {
            player.spoofChatInput("/" + command);
        }
    }

    /**
     * 以控制台身份执行命令。
     */
    public static void executeConsole(String command) {
        if (proxy == null) return;
        proxy.getCommandManager().executeAsync(proxy.getConsoleCommandSource(), command);
    }

    /**
     * 通过 RCON 执行命令。
     *
     * @param command      命令
     * @param targetServer 目标子服，null 表示所有子服
     */
    public static void executeRcon(String command, String targetServer) {
        if (rconPool == null) {
            // RCON 未配置，降级为控制台
            executeConsole(command);
            return;
        }

        if (targetServer != null && !targetServer.isEmpty()) {
            String result = rconPool.execute(targetServer, command);
            if (result == null) {
                // RCON 失败，降级为控制台
                executeConsole(command);
            }
        } else {
            rconPool.executeAll(command);
        }
    }

    /**
     * 打开 URL。
     */
    public static void openUrl(UUID playerId, String url) {
        if (proxy == null) return;
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player != null) {
            Component msg = Component.text("§a§l[点击打开链接]")
                    .clickEvent(ClickEvent.openUrl(url));
            player.sendMessage(msg);
        }
    }

    /**
     * 发送消息给玩家。
     */
    public static void sendMessage(UUID playerId, String message) {
        if (proxy == null) return;
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player != null) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }
    }

    /**
     * 建议命令（填入聊天框但不执行）。
     */
    public static void suggestCommand(UUID playerId, String command) {
        if (proxy == null) return;
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player != null) {
            Component msg = Component.text("§a§l[点击填入命令]")
                    .clickEvent(ClickEvent.suggestCommand(command));
            player.sendMessage(msg);
        }
    }
}
