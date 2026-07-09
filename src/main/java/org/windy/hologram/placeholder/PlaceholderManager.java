package org.windy.hologram.placeholder;

import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 占位符管理器。
 * <p>支持自定义占位符替换。
 *
 * <p>内置占位符：
 * <ul>
 *   <li>{@code %online%} - 在线玩家数</li>
 *   <li>{@code %max%} - 最大玩家数</li>
 *   <li>{@code %player%} - 玩家名</li>
 *   <li>{@code %ping%} - 玩家延迟</li>
 *   <li>{@code %server%} - 玩家所在服务器</li>
 *   <li>{@code %time%} - 当前时间</li>
 * </ul>
 */
public class PlaceholderManager {

    private final Map<String, Function<UUID, String>> placeholders = new ConcurrentHashMap<>();
    private final ProxyServer proxy;

    public PlaceholderManager(ProxyServer proxy) {
        this.proxy = proxy;
        registerDefaults();
    }

    /**
     * 注册内置占位符。
     */
    private void registerDefaults() {
        register("online", playerId -> String.valueOf(proxy.getPlayerCount()));
        register("max", playerId -> String.valueOf(proxy.getConfiguration().getShowMaxPlayers()));
        register("player", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            return player != null ? player.getUsername() : "";
        });
        register("ping", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            return player != null ? String.valueOf(player.getPing()) : "0";
        });
        register("server", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            if (player != null) {
                return player.getCurrentServer()
                        .map(s -> s.getServerInfo().getName())
                        .orElse("unknown");
            }
            return "unknown";
        });
        register("time", playerId -> java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /**
     * 注册自定义占位符。
     *
     * @param placeholder 占位符名称（不含 %%）
     * @param provider    提供值的函数
     */
    public void register(String placeholder, Function<UUID, String> provider) {
        placeholders.put(placeholder.toLowerCase(), provider);
    }

    /**
     * 替换文本中的占位符。
     *
     * @param text     原始文本
     * @param playerId 玩家 UUID（用于玩家相关占位符）
     * @return 替换后的文本
     */
    public String replace(String text, UUID playerId) {
        if (text == null || text.isEmpty()) return text;

        String result = text;
        for (Map.Entry<String, Function<UUID, String>> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                String value = entry.getValue().apply(playerId);
                result = result.replace(placeholder, value != null ? value : "");
            }
        }
        return result;
    }

    /**
     * 替换文本中的占位符（不指定玩家）。
     */
    public String replaceGlobal(String text) {
        return replace(text, null);
    }
}
