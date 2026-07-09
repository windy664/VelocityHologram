package org.windy.hologram.action;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

/**
 * 动作执行上下文。
 * <p>持有 ProxyServer 引用，供 Action 实现调用。
 */
public class ActionContext {

    private static ProxyServer proxy;

    public static void init(ProxyServer proxy) {
        ActionContext.proxy = proxy;
    }

    /**
     * 以玩家身份执行命令。
     */
    public static void executeAsPlayer(UUID playerId, String command) {
        if (proxy == null) return;
        Player player = proxy.getPlayer(playerId).orElse(null);
        if (player != null) {
            // Velocity 3.3.0+ 的 player.chat() 可能不存在
            // 使用 player.spoofChatInput() 代替
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
     * <p>需要目标服务器的 RCON 连接信息。
     * TODO: 实现 RCON 客户端
     */
    public static void executeRcon(String command, String targetServer) {
        // RCON 需要单独的 TCP 连接到服务器的 RCON 端口
        // 这里先用控制台命令代替，后续实现真正的 RCON
        executeConsole(command);
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
