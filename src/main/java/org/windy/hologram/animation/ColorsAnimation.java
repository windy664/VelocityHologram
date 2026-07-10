package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 颜色循环动画。
 * <p>语法：{colors:speed|文本}
 * <p>文字颜色循环变化。
 */
public class ColorsAnimation extends TextAnimation {

    private final String baseText;
    private final int speed;
    private int tick;

    public ColorsAnimation(String baseText, int speed) {
        super(AnimationType.COLORS, generateFrames(baseText, speed), speed);
        this.baseText = baseText;
        this.speed = speed;
        this.tick = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        return tick % speed == 0;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText == null || baseText.isEmpty()) return "";
        return generateColorsText(baseText, tick);
    }

    /**
     * 生成颜色循环文本。
     */
    private static String generateColorsText(String text, int tick) {
        String[] colors = {"§4", "§c", "§6", "§e", "§2", "§a", "§b", "§3", "§9", "§1", "§5", "§d"};
        int colorOffset = tick / 2;

        StringBuilder sb = new StringBuilder();
        int colorIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                sb.append(c);
                if (i + 1 < text.length()) {
                    sb.append(text.charAt(i + 1));
                    i++;
                }
                continue;
            }

            if (!Character.isWhitespace(c)) {
                int idx = (colorIndex + colorOffset) % colors.length;
                sb.append(colors[idx]).append(c);
                colorIndex++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 生成帧列表（用于初始化）。
     */
    private static List<String> generateFrames(String text, int speed) {
        List<String> frames = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            frames.add(generateColorsText(text, i * speed));
        }
        return frames;
    }
}
