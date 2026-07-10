package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 渐变色解析器。
 * <p>将渐变语法转换为 MC 1.16+ RGB 颜色格式。
 *
 * <p>语法：
 * <ul>
 *   <li>{@code {gradient:#FF0000:#0000FF|文本}} - 双色渐变</li>
 *   <li>{@code {gradient:#FF0000:#00FF00:#0000FF|文本}} - 多色渐变</li>
 *   <li>{@code {gradient:rainbow|文本}} - 彩虹渐变</li>
 *   <li>{@code {gradient-anim:#FF0000:#0000FF:10|文本}} - 动态渐变（每 10 tick 偏移）</li>
 * </ul>
 */
public class GradientParser {

    private GradientParser() {}

    /**
     * 检查文本是否包含渐变语法。
     */
    public static boolean hasGradient(String text) {
        return text != null && (text.contains("{gradient:") || text.contains("{gradient-anim:")
                || text.contains("&u") || text.contains("§u"));
    }

    /**
     * 应用静态渐变。
     *
     * @param text 包含渐变语法的文本
     * @return 应用渐变后的文本（RGB 颜色格式）
     */
    public static String applyGradient(String text) {
        if (text == null || text.isEmpty()) return text;

        // 处理 &u 或 §u（彩虹动画，等同于 <#ANIM:colors>）
        if (text.contains("&u") || text.contains("§u")) {
            text = text.replace("&u", "").replace("§u", "");
            int[] rainbow = {0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x0000FF, 0x4B0082, 0x9400D3};
            return buildGradient(text, rainbow, 0);
        }

        // 处理 {gradient:color1:color2|文本}
        int start = text.indexOf("{gradient:");
        if (start >= 0) {
            int end = text.indexOf("}", start);
            if (end > start) {
                String inner = text.substring(start + 10, end);
                String[] parts = inner.split("\\|", 2);
                if (parts.length == 2) {
                    String colorPart = parts[0];
                    String content = parts[1];
                    int[] colors = parseColors(colorPart);
                    if (colors.length >= 2) {
                        String gradient = buildGradient(content, colors, 0);
                        return text.substring(0, start) + gradient + text.substring(end + 1);
                    }
                }
            }
        }

        return text;
    }

    /**
     * 应用动态渐变（带动画偏移）。
     *
     * @param text   包含渐变语法的文本
     * @param offset 偏移量（tick 计数）
     * @return 应用渐变后的文本
     */
    public static String applyAnimatedGradient(String text, int offset) {
        if (text == null || text.isEmpty()) return text;

        int start = text.indexOf("{gradient-anim:");
        if (start < 0) return text;

        int end = text.indexOf("}", start);
        if (end <= start) return text;

        String inner = text.substring(start + 15, end);
        String[] parts = inner.split("\\|", 2);
        if (parts.length < 2) return text;

        String colorPart = parts[0];
        String content = parts[1];

        // 解析颜色和间隔
        String[] colorTokens = colorPart.split(":");
        List<Integer> colors = new ArrayList<>();
        int interval = 10; // 默认间隔

        for (String token : colorTokens) {
            token = token.trim();
            if (token.startsWith("#")) {
                colors.add(parseHexColor(token));
            } else if (token.matches("\\d+")) {
                interval = Integer.parseInt(token);
            }
        }

        if (colors.size() < 2) return text;

        int effectiveOffset = offset / Math.max(1, interval);
        String gradient = buildGradient(content, colors.stream().mapToInt(Integer::intValue).toArray(), effectiveOffset);
        return text.substring(0, start) + gradient + text.substring(end + 1);
    }

    /**
     * 从颜色部分解析颜色数组。
     * <p>格式：#FF0000:#00FF00:#0000FF
     */
    private static int[] parseColors(String colorPart) {
        String[] tokens = colorPart.split(":");
        List<Integer> colors = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim();
            if (token.equalsIgnoreCase("rainbow")) {
                return new int[]{0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x0000FF, 0x4B0082, 0x9400D3};
            }
            if (token.startsWith("#")) {
                colors.add(parseHexColor(token));
            }
        }
        return colors.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 解析十六进制颜色。
     */
    private static int parseHexColor(String hex) {
        try {
            hex = hex.replace("#", "");
            if (hex.length() == 6) {
                return Integer.parseInt(hex, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0xFFFFFF;
    }

    /**
     * 构建渐变文本。
     * <p>使用 MC 1.16+ 的 §x§r§r§g§g§b§b 格式。
     *
     * @param text       原始文本
     * @param colors     颜色数组
     * @param tickOffset 动画偏移
     */
    private static String buildGradient(String text, int[] colors, int tickOffset) {
        if (text.isEmpty() || colors.length < 2) return text;

        // 去掉已有的颜色代码
        String cleanText = text.replaceAll("§[0-9a-fk-or]", "");
        if (cleanText.isEmpty()) return text;

        int len = cleanText.length();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < len; i++) {
            // 计算当前字符在渐变中的位置（带偏移）
            float progress = (float) (i + tickOffset) / Math.max(1, len - 1);
            // 循环渐变
            progress = progress - (float) Math.floor(progress);

            int color = interpolateColor(colors, progress);
            sb.append(colorToMcCode(color));
            sb.append(cleanText.charAt(i));
        }

        return sb.toString();
    }

    /**
     * 在颜色数组中插值。
     */
    private static int interpolateColor(int[] colors, float progress) {
        if (colors.length == 1) return colors[0];

        float segment = progress * (colors.length - 1);
        int index = (int) Math.floor(segment);
        float t = segment - index;

        if (index >= colors.length - 1) return colors[colors.length - 1];

        return lerpColor(colors[index], colors[index + 1], t);
    }

    /**
     * 线性插值两个颜色。
     */
    private static int lerpColor(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * 将 RGB 颜色转换为 MC §x 格式。
     * <p>格式：§x§r§r§g§g§b§b
     */
    private static String colorToMcCode(int color) {
        String hex = String.format("%06x", color);
        return "§x§" + hex.charAt(0) + "§" + hex.charAt(1)
                + "§" + hex.charAt(2) + "§" + hex.charAt(3)
                + "§" + hex.charAt(4) + "§" + hex.charAt(5);
    }
}
