package org.windy.hologram.utils;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * 通用工具类。
 * <p>提供常用的工具方法。
 */
public final class CommonUtils {

    private CommonUtils() {}

    /**
     * 检查字符串是否为空或空白。
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空且不为空白。
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 检查集合是否为空。
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 检查Map是否为空。
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 安全的字符串截断。
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 格式化数字。
     * <p>例如: 1234 → "1,234"
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * 格式化小数。
     */
    public static String formatDecimal(double value, int places) {
        return String.format("%." + places + "f", value);
    }

    /**
     * 格式化百分比。
     */
    public static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * 格式化文件大小。
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 格式化时间长度。
     */
    public static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "秒";
        if (seconds < 3600) return (seconds / 60) + "分" + (seconds % 60 > 0 ? (seconds % 60) + "秒" : "");
        if (seconds < 86400) return (seconds / 3600) + "时" + ((seconds % 3600) / 60 > 0 ? ((seconds % 3600) / 60) + "分" : "");
        return (seconds / 86400) + "天" + ((seconds % 86400) / 3600 > 0 ? ((seconds % 86400) / 3600) + "时" : "");
    }

    /**
     * 生成短UUID。
     */
    public static String shortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 检查是否是有效的UUID。
     */
    public static boolean isValidUUID(String str) {
        if (str == null) return false;
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 安全的整数解析。
     */
    public static int parseInt(String str, int defaultValue) {
        if (str == null || str.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全的长整数解析。
     */
    public static long parseLong(String str, long defaultValue) {
        if (str == null || str.isEmpty()) return defaultValue;
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全的浮点数解析。
     */
    public static double parseDouble(String str, double defaultValue) {
        if (str == null || str.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 安全的布尔值解析。
     */
    public static boolean parseBoolean(String str, boolean defaultValue) {
        if (str == null || str.isEmpty()) return defaultValue;
        String lower = str.trim().toLowerCase();
        return "true".equals(lower) || "yes".equals(lower) || "1".equals(lower);
    }

    /**
     * 限制值在指定范围内。
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 限制值在指定范围内。
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 计算百分比。
     */
    public static double percentage(double part, double total) {
        if (total == 0) return 0;
        return part / total;
    }
}
