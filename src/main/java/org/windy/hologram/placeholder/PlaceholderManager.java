package org.windy.hologram.placeholder;

import com.velocitypowered.api.proxy.ProxyServer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符管理器。
 * <p>命名遵循 PlaceholderAPI 惯例。
 *
 * <p>内置占位符：
 * <ul>
 *   <li>{@code %server_online%} - 在线玩家数</li>
 *   <li>{@code %server_max_players%} - 最大玩家数</li>
 *   <li>{@code %player_name%} - 玩家名</li>
 *   <li>{@code %player_ping%} - 玩家延迟</li>
 *   <li>{@code %player_server%} - 玩家所在服务器</li>
 *   <li>{@code %server_time%} - 当前时间（HH:mm:ss）</li>
 *   <li>{@code %server_time_HH:mm%} - 自定义时间格式</li>
 *   <li>{@code %server_date%} - 当前日期（yyyy-MM-dd）</li>
 *   <li>{@code %server_date_yyyy/MM/dd%} - 自定义日期格式</li>
 *   <li>{@code %server_motd%} - 服务器 MOTD</li>
 * </ul>
 */
public class PlaceholderManager {

    private final Map<String, Function<UUID, String>> placeholders = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<String, UUID, String>> parameterizedPlaceholders = new ConcurrentHashMap<>();
    private final ProxyServer proxy;

    // 带参数占位符的正则：%name:param%
    private static final Pattern PARAM_PATTERN = Pattern.compile("%(\\w+):([^%]+)%");

    public PlaceholderManager(ProxyServer proxy) {
        this.proxy = proxy;
        registerDefaults();
    }

    /**
     * 注册内置占位符（PAPI 命名）。
     */
    private void registerDefaults() {
        // server 域
        register("server_online", playerId -> String.valueOf(proxy.getPlayerCount()));
        register("server_max_players", playerId -> String.valueOf(proxy.getConfiguration().getShowMaxPlayers()));
        register("server_time", playerId -> LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        register("server_date", playerId -> LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        register("server_motd", playerId -> {
            var motd = proxy.getConfiguration().getMotd();
            return motd != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(motd) : "";
        });

        // player 域
        register("player_name", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            return player != null ? player.getUsername() : "";
        });
        register("player_displayname", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            return player != null ? player.getUsername() : "";
        });
        register("player_ping", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            return player != null ? String.valueOf(player.getPing()) : "0";
        });
        register("player_server", playerId -> {
            var player = proxy.getPlayer(playerId).orElse(null);
            if (player != null) {
                return player.getCurrentServer()
                        .map(s -> s.getServerInfo().getName())
                        .orElse("unknown");
            }
            return "unknown";
        });

        // 带参数占位符
        registerParameterized("server_time", (format, playerId) -> {
            try {
                return LocalTime.now().format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return "invalid:" + format;
            }
        });
        registerParameterized("server_date", (format, playerId) -> {
            try {
                return LocalDate.now().format(DateTimeFormatter.ofPattern(format));
            } catch (Exception e) {
                return "invalid:" + format;
            }
        });
    }

    /**
     * 注册简单占位符。
     */
    public void register(String placeholder, Function<UUID, String> provider) {
        placeholders.put(placeholder.toLowerCase(), provider);
    }

    /**
     * 注册带参数占位符。
     */
    public void registerParameterized(String placeholder, BiFunction<String, UUID, String> provider) {
        parameterizedPlaceholders.put(placeholder.toLowerCase(), provider);
    }

    /**
     * 替换文本中的占位符。
     */
    public String replace(String text, UUID playerId) {
        if (text == null || text.isEmpty()) return text;

        String result = text;

        // 先处理带参数占位符：%name:param%
        Matcher matcher = PARAM_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1).toLowerCase();
            String param = matcher.group(2);
            BiFunction<String, UUID, String> provider = parameterizedPlaceholders.get(name);
            if (provider != null) {
                String value = provider.apply(param, playerId);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : ""));
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // 再处理简单占位符
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
