package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 动画解析器。
 * <p>从配置字符串解析动画。
 *
 * <p>语法：
 * <ul>
 *   <li>{@code {cycle:20|帧1|帧2|帧3}} - 每 20 tick 循环</li>
 *   <li>{@code {random:40|帧1|帧2|帧3}} - 每 40 tick 随机</li>
 *   <li>{@code {typewriter:5|完整文本}} - 打字机效果</li>
 * </ul>
 */
public class AnimationParser {

    private AnimationParser() {}

    /**
     * 检查文本是否包含动画或渐变。
     */
    public static boolean hasAnimation(String text) {
        if (text == null) return false;
        return text.contains("{cycle:")
                || text.contains("{random:")
                || text.contains("{typewriter:")
                || text.contains("{wave:")
                || text.contains("{burn:")
                || text.contains("{scroll:")
                || text.contains("{colors:")
                || GradientParser.hasGradient(text);
    }

    /**
     * 解析动画。
     *
     * @return 动画对象，如果文本不包含动画则返回 null
     */
    public static TextAnimation parse(String text) {
        if (text == null || text.isEmpty()) return null;

        if (text.startsWith("{cycle:") && text.endsWith("}")) {
            return parseCycle(text);
        }
        if (text.startsWith("{random:") && text.endsWith("}")) {
            return parseRandom(text);
        }
        if (text.startsWith("{typewriter:") && text.endsWith("}")) {
            return parseTypewriter(text);
        }
        if (text.startsWith("{wave:") && text.endsWith("}")) {
            return parseWave(text);
        }
        if (text.startsWith("{burn:") && text.endsWith("}")) {
            return parseBurn(text);
        }
        if (text.startsWith("{scroll:") && text.endsWith("}")) {
            return parseScroll(text);
        }
        if (text.startsWith("{colors:") && text.endsWith("}")) {
            return parseColors(text);
        }

        return null;
    }

    private static TextAnimation parseCycle(String text) {
        String content = text.substring(7, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int interval = requirePositive(parts[0]);
            List<String> frames = parseFrames(parts[1]);
            if (frames.isEmpty()) return null;
            return new TextAnimation(
                    TextAnimation.AnimationType.CYCLE,
                    frames,
                    interval
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseRandom(String text) {
        String content = text.substring(8, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int interval = requirePositive(parts[0]);
            List<String> frames = parseFrames(parts[1]);
            if (frames.isEmpty()) return null;
            return new TextAnimation(
                    TextAnimation.AnimationType.RANDOM,
                    frames,
                    interval
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseTypewriter(String text) {
        String content = text.substring(12, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int delay = requirePositive(parts[0]);
            String fullText = parts[1];
            if (fullText.isEmpty()) return null;

            List<String> frames = new ArrayList<>();
            for (int index = 1; index <= fullText.length(); index++) {
                frames.add(fullText.substring(0, index));
            }
            frames.add(fullText);

            return new TextAnimation(
                    TextAnimation.AnimationType.TYPEWRITER,
                    frames,
                    delay
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseWave(String text) {
        String content = text.substring(6, text.length() - 1);
        String[] parts = content.split("\\|", 3);
        if (parts.length < 3) return null;

        try {
            int amplitude = requirePositive(parts[0]);
            int speed = requirePositive(parts[1]);
            return new WaveAnimation(parts[2], amplitude, speed);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseBurn(String text) {
        String content = text.substring(6, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int duration = requirePositive(parts[0]);
            return new BurnAnimation(parts[1], duration);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseScroll(String text) {
        String content = text.substring(8, text.length() - 1);
        String[] parts = content.split("\\|", 3);
        if (parts.length < 3) return null;

        try {
            int width = requirePositive(parts[0]);
            int speed = requirePositive(parts[1]);
            return new ScrollAnimation(parts[2], width, speed);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static TextAnimation parseColors(String text) {
        String content = text.substring(8, text.length() - 1);
        String[] parts = content.split("\\|", 2);
        if (parts.length < 2) return null;

        try {
            int speed = requirePositive(parts[0]);
            return new ColorsAnimation(parts[1], speed);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static List<String> parseFrames(String input) {
        List<String> frames = new ArrayList<>();

        for (String frame : input.split("\\|", -1)) {
            frames.add(frame.trim());
        }

        return frames;
    }

    private static int requirePositive(String value) {
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("参数必须是整数", exception);
        }

        if (parsed <= 0) {
            throw new IllegalArgumentException("参数必须大于零");
        }

        return parsed;
    }
}
