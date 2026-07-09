package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画解析器。
 * <p>从配置字符串解析动画。
 *
 * <p>语法：
 * <ul>
 *   <li>{@code {cycle:20|帧1|帧2|帧3}} - 每20tick循环</li>
 *   <li>{@code {random:40|帧1|帧2|帧3}} - 每40tick随机</li>
 *   <li>{@code {typewriter:5|完整文本}} - 打字机效果</li>
 * </ul>
 */
public class AnimationParser {

    private AnimationParser() {}

    /**
     * 检查文本是否包含动画。
     */
    public static boolean hasAnimation(String text) {
        return text != null && text.contains("{cycle:") || text.contains("{random:") || text.contains("{typewriter:");
    }

    /**
     * 解析动画。
     *
     * @return 动画对象，如果文本不包含动画则返回 null
     */
    public static TextAnimation parse(String text) {
        if (text == null || text.isEmpty()) return null;

        // 解析 {cycle:interval|frame1|frame2|...}
        if (text.startsWith("{cycle:") && text.endsWith("}")) {
            return parseCycle(text);
        }

        // 解析 {random:interval|frame1|frame2|...}
        if (text.startsWith("{random:") && text.endsWith("}")) {
            return parseRandom(text);
        }

        // 解析 {typewriter:delay|text}
        if (text.startsWith("{typewriter:") && text.endsWith("}")) {
            return parseTypewriter(text);
        }

        return null;
    }

    private static TextAnimation parseCycle(String text) {
        String content = text.substring(7, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int interval = Integer.parseInt(parts[0].trim());
            String[] frames = parts[1].split("\\|");
            List<String> frameList = new ArrayList<>();
            for (String frame : frames) {
                frameList.add(frame.trim());
            }
            return new TextAnimation(TextAnimation.AnimationType.CYCLE, frameList, interval);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TextAnimation parseRandom(String text) {
        String content = text.substring(8, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int interval = Integer.parseInt(parts[0].trim());
            String[] frames = parts[1].split("\\|");
            List<String> frameList = new ArrayList<>();
            for (String frame : frames) {
                frameList.add(frame.trim());
            }
            return new TextAnimation(TextAnimation.AnimationType.RANDOM, frameList, interval);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TextAnimation parseTypewriter(String text) {
        String content = text.substring(12, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int delay = Integer.parseInt(parts[0].trim());
            String fullText = parts[1];

            // 生成逐字帧
            List<String> frames = new ArrayList<>();
            for (int i = 1; i <= fullText.length(); i++) {
                frames.add(fullText.substring(0, i));
            }
            // 最后一帧停留
            frames.add(fullText);

            return new TextAnimation(TextAnimation.AnimationType.TYPEWRITER, frames, delay);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
