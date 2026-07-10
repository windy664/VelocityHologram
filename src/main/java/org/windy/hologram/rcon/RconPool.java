package org.windy.hologram.rcon;

import org.windy.hologram.config.PluginConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RCON 连接池。
 * <p>管理多个子服的 RCON 连接，按需连接、自动重连。
 */
public class RconPool {

    private final Map<String, PluginConfig.RconServerConfig> configs;
    private final Map<String, RconClient> clients = new ConcurrentHashMap<>();

    public RconPool(Map<String, PluginConfig.RconServerConfig> configs) {
        this.configs = configs;
    }

    /**
     * 在指定子服执行 RCON 命令。
     *
     * @param serverName 子服名称
     * @param command    命令
     * @return 服务器响应，失败返回 null
     */
    public String execute(String serverName, String command) {
        PluginConfig.RconServerConfig config = configs.get(serverName);
        if (config == null) return null;

        RconClient client = getOrCreateClient(serverName, config);
        if (client == null) return null;

        try {
            return client.execute(command);
        } catch (Exception e) {
            // 连接断开，移除并重试一次
            client.close();
            clients.remove(serverName);
            client = getOrCreateClient(serverName, config);
            if (client == null) return null;
            try {
                return client.execute(command);
            } catch (Exception e2) {
                client.close();
                clients.remove(serverName);
                return null;
            }
        }
    }

    /**
     * 在所有已配置的子服执行 RCON 命令。
     */
    public void executeAll(String command) {
        for (String server : configs.keySet()) {
            execute(server, command);
        }
    }

    private RconClient getOrCreateClient(String serverName, PluginConfig.RconServerConfig config) {
        RconClient client = clients.get(serverName);
        if (client != null && client.isConnected()) return client;

        // 创建新连接
        client = new RconClient(config.host, config.port, config.password);
        try {
            client.connect();
            clients.put(serverName, client);
            return client;
        } catch (Exception e) {
            client.close();
            return null;
        }
    }

    /**
     * 关闭所有连接。
     */
    public void closeAll() {
        for (RconClient client : clients.values()) {
            client.close();
        }
        clients.clear();
    }

    /**
     * 是否有指定服务器的 RCON 配置。
     */
    public boolean hasServer(String serverName) {
        return configs.containsKey(serverName);
    }
}
