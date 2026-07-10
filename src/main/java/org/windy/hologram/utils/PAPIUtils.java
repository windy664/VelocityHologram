package org.windy.hologram.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 占位符工具类。
 * <p>内置占位符解析，不依赖 PlaceholderAPI。
 */
public final class PAPIUtils {

    // 自定义占位符注册表
    private static final Map<String, Function<UUID, String>> CUSTOM_PLACEHOLDERS = new ConcurrentHashMap<>();

    // 日期时间格式
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PAPIUtils() {}

    /**
     * 解析占位符。
     *
     * @param text     包含占位符的文本
     * @param playerId 玩家UUID（用于玩家相关占位符）
     * @return 解析后的文本
     */
    public static String replace(String text, UUID playerId) {
        if (text == null || text.isEmpty()) return text;

        String result = text;

        // 时间日期占位符
        result = result.replace("%server_time%", LocalDateTime.now().format(TIME_FORMAT));
        result = result.replace("%server_date%", LocalDateTime.now().format(DATE_FORMAT));
        result = result.replace("%server_datetime%", LocalDateTime.now().format(DATETIME_FORMAT));

        // 自定义时间格式
        result = replaceCustomTimeFormat(result);

        // 自定义占位符
        for (Map.Entry<String, Function<UUID, String>> entry : CUSTOM_PLACEHOLDERS.entrySet()) {
            String placeholder = entry.getKey();
            if (result.contains(placeholder)) {
                try {
                    String value = entry.getValue().apply(playerId);
                    if (value != null) {
                        result = result.replace(placeholder, value);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    /**
     * 替换自定义时间格式。
     * <p>例如: %server_time:HH:mm% → 当前时间
     */
    private static String replaceCustomTimeFormat(String text) {
        if (text == null) return text;

        int start = text.indexOf("%server_time:");
        if (start == -1) return text;

        int end = text.indexOf("%", start + 13);
        if (end == -1) return text;

        String format = text.substring(start + 13, end);
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            String time = LocalDateTime.now().format(formatter);
            return text.substring(0, start) + time + text.substring(end + 1);
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * 注册自定义占位符。
     *
     * @param placeholder 占位符（如 %myplugin_value%）
     * @param resolver    解析函数
     */
    public static void registerPlaceholder(String placeholder, Function<UUID, String> resolver) {
        if (placeholder != null && resolver != null) {
            CUSTOM_PLACEHOLDERS.put(placeholder, resolver);
        }
    }

    /**
     * 注销自定义占位符。
     */
    public static void unregisterPlaceholder(String placeholder) {
        CUSTOM_PLACEHOLDERS.remove(placeholder);
    }

    /**
     * 检查文本是否包含占位符。
     */
    public static boolean containsPlaceholders(String text) {
        if (text == null) return false;
        return text.contains("%");
    }

    /**
     * 获取所有注册的自定义占位符。
     */
    public static Map<String, Function<UUID, String>> getRegisteredPlaceholders() {
        return CUSTOM_PLACEHOLDERS;
    }

    /**
     * 清除所有自定义占位符。
     */
    public static void clearPlaceholders() {
        CUSTOM_PLACEHOLDERS.clear();
    }
}
