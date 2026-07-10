package org.windy.hologram.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;

/**
 * BungeeCord/Velocity 跨服工具类。
 * <p>处理玩家跨服传送、服务器连接等。
 */
public final class BungeeUtils {

    private BungeeUtils() {}

    /**
     * 将玩家传送到指定服务器。
     *
     * @param proxy      代理服务器
     * @param player     玩家
     * @param serverName 服务器名称
     * @return true 如果传送成功
     */
    public static boolean connect(ProxyServer proxy, Player player, String serverName) {
        if (proxy == null || player == null || serverName == null) {
            return false;
        }

        Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
        if (serverOpt.isEmpty()) {
            return false;
        }

        player.createConnectionRequest(serverOpt.get()).fireAndForget();
        return true;
    }

    /**
     * 将玩家传送到指定服务器（异步）。
     *
     * @param proxy      代理服务器
     * @param player     玩家
     * @param serverName 服务器名称
     * @param callback   回调（成功/失败）
     */
    public static void connectAsync(ProxyServer proxy, Player player, String serverName, ConnectCallback callback) {
        if (proxy == null || player == null || serverName == null) {
            if (callback != null) callback.onResult(false);
            return;
        }

        Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
        if (serverOpt.isEmpty()) {
            if (callback != null) callback.onResult(false);
            return;
        }

        player.createConnectionRequest(serverOpt.get()).connect()
                .thenAccept(result -> {
                    if (callback != null) {
                        callback.onResult(result.isSuccessful());
                    }
                });
    }

    /**
     * 检查服务器是否在线。
     */
    public static boolean isServerOnline(ProxyServer proxy, String serverName) {
        if (proxy == null || serverName == null) return false;
        return proxy.getServer(serverName).isPresent();
    }

    /**
     * 获取服务器在线玩家数。
     */
    public static int getServerPlayerCount(ProxyServer proxy, String serverName) {
        if (proxy == null || serverName == null) return 0;
        Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
        return serverOpt.map(server -> server.getPlayersConnected().size()).orElse(0);
    }

    /**
     * 获取玩家当前服务器名称。
     */
    public static String getPlayerServer(Player player) {
        if (player == null) return null;
        return player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse(null);
    }

    /**
     * 检查玩家是否在指定服务器。
     */
    public static boolean isPlayerOnServer(Player player, String serverName) {
        if (player == null || serverName == null) return false;
        String currentServer = getPlayerServer(player);
        return serverName.equals(currentServer);
    }

    /**
     * 连接回调接口。
     */
    @FunctionalInterface
    public interface ConnectCallback {
        void onResult(boolean success);
    }
}
