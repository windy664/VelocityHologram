package org.windy.hologram.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色工具类。
 * <p>处理颜色代码、渐变、十六进制颜色等。
 */
public final class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final Pattern STRIP_PATTERN = Pattern.compile("§[0-9a-fk-orxA-fK-ORX]");

    private ColorUtils() {}

    /**
     * 将十六进制颜色转换为 Minecraft 颜色代码。
     * <p>例如: #FF5555 → §x§f§f§5§5§5§5
     */
    public static String hexToMinecraft(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return hex;
        }

        StringBuilder sb = new StringBuilder("§x");
        for (int i = 1; i < 7; i++) {
            sb.append("§").append(Character.toLowerCase(hex.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * 将包含十六进制颜色的文本转换为 Minecraft 格式。
     * <p>例如: "#FF5555Hello" → "§x§f§f§5§5§5§5Hello"
     */
    public static String translateHexColors(String text) {
        if (text == null) return null;

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            sb.append(hexToMinecraft(matcher.group()));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));

        return sb.toString();
    }

    /**
     * 翻译颜色代码（& → §）。
     */
    public static String translateAlternateColorCodes(String text) {
        if (text == null) return null;
        return text.replace('&', '§');
    }

    /**
     * 去除所有颜色代码。
     */
    public static String stripColor(String text) {
        if (text == null) return null;
        return STRIP_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 检查文本是否包含颜色代码。
     */
    public static boolean hasColor(String text) {
        if (text == null) return false;
        return COLOR_CODE_PATTERN.matcher(text).find() || HEX_PATTERN.matcher(text).find();
    }

    /**
     * 生成渐变色文本。
     *
     * @param text   文本
     * @param start  起始颜色（十六进制，如 #FF0000）
     * @param end    结束颜色（十六进制，如 #0000FF）
     * @return 渐变色文本
     */
    public static String gradient(String text, String start, String end) {
        if (text == null || text.isEmpty()) return text;

        int[] startRgb = hexToRgb(start);
        int[] endRgb = hexToRgb(end);
        if (startRgb == null || endRgb == null) return text;

        StringBuilder sb = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '§') {
                sb.append(c);
                if (i + 1 < length) {
                    sb.append(text.charAt(i + 1));
                    i++;
                }
                continue;
            }

            double ratio = (double) i / (length - 1);
            int r = (int) (startRgb[0] + (endRgb[0] - startRgb[0]) * ratio);
            int g = (int) (startRgb[1] + (endRgb[1] - startRgb[1]) * ratio);
            int b = (int) (startRgb[2] + (endRgb[2] - startRgb[2]) * ratio);

            String hex = String.format("#%02x%02x%02x", r, g, b);
            sb.append(hexToMinecraft(hex)).append(c);
        }

        return sb.toString();
    }

    /**
     * 生成彩虹色文本。
     */
    public static String rainbow(String text) {
        String[] colors = {
                "#FF0000", "#FF7F00", "#FFFF00", "#00FF00",
                "#0000FF", "#4B0082", "#9400D3"
        };
        return rainbow(text, colors);
    }

    /**
     * 使用自定义颜色列表生成彩虹色文本。
     */
    public static String rainbow(String text, String[] colors) {
        if (text == null || text.isEmpty() || colors == null || colors.length == 0) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        int length = text.length();
        int colorCount = colors.length;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '§') {
                sb.append(c);
                if (i + 1 < length) {
                    sb.append(text.charAt(i + 1));
                    i++;
                }
                continue;
            }

            int colorIndex = (i * colorCount / length) % colorCount;
            sb.append(hexToMinecraft(colors[colorIndex])).append(c);
        }

        return sb.toString();
    }

    /**
     * 解析十六进制颜色为 RGB 数组。
     */
    private static int[] hexToRgb(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
            return null;
        }
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
