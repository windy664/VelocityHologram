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
        super(
                AnimationType.COLORS,
                generateFrames(baseText, normalizeSpeed(speed)),
                normalizeSpeed(speed)
        );
        this.baseText = baseText != null ? baseText : "";
        this.speed = normalizeSpeed(speed);
        this.tick = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        return tick % speed == 0;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText.isEmpty()) return "";
        return generateColorsText(baseText, tick);
    }

    private static String generateColorsText(String text, int tick) {
        if (text == null || text.isEmpty()) return "";

        String[] colors = {
                "§4", "§c", "§6", "§e", "§2", "§a",
                "§b", "§3", "§9", "§1", "§5", "§d"
        };
        int colorOffset = Math.floorDiv(tick, 2);

        StringBuilder result = new StringBuilder();
        int visibleCharacterIndex = 0;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '§') {
                result.append(character);
                if (index + 1 < text.length()) {
                    result.append(text.charAt(index + 1));
                    index++;
                }
                continue;
            }

            if (Character.isWhitespace(character)) {
                result.append(character);
                continue;
            }

            int colorIndex = Math.floorMod(
                    visibleCharacterIndex + colorOffset,
                    colors.length
            );
            result.append(colors[colorIndex]).append(character);
            visibleCharacterIndex++;
        }

        return result.toString();
    }

    private static List<String> generateFrames(String text, int speed) {
        List<String> frames = new ArrayList<>();
        for (int frame = 0; frame < 24; frame++) {
            frames.add(generateColorsText(text, frame * speed));
        }
        return frames;
    }

    private static int normalizeSpeed(int speed) {
        return Math.max(1, speed);
    }
}
