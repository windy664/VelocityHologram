package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动动画。
 * <p>语法：{scroll:width|speed|文本}
 * <p>文字在固定宽度内滚动。
 */
public class ScrollAnimation extends TextAnimation {

    private final String baseText;
    private final int width;
    private final int speed;
    private int tick;
    private int position;

    public ScrollAnimation(String baseText, int width, int speed) {
        super(
                AnimationType.SCROLL,
                generateFrames(baseText, normalizeWidth(width)),
                normalizeSpeed(speed)
        );
        this.baseText = baseText != null ? baseText : "";
        this.width = normalizeWidth(width);
        this.speed = normalizeSpeed(speed);
        this.tick = 0;
        this.position = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        if (tick % speed != 0) {
            return false;
        }

        position++;
        return true;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText.isEmpty()) return " ".repeat(width);
        return generateScrollText(baseText, width, position);
    }

    private static String generateScrollText(String text, int width, int position) {
        int normalizedWidth = normalizeWidth(width);
        String safeText = text != null ? text : "";
        String padded = " ".repeat(normalizedWidth)
                + safeText
                + " ".repeat(normalizedWidth);

        int totalLength = padded.length();
        if (totalLength == 0) {
            return "";
        }

        int start = Math.floorMod(position, totalLength);
        StringBuilder result = new StringBuilder(normalizedWidth);

        for (int index = 0; index < normalizedWidth; index++) {
            result.append(padded.charAt((start + index) % totalLength));
        }

        return result.toString();
    }

    private static List<String> generateFrames(String text, int width) {
        int normalizedWidth = normalizeWidth(width);
        String safeText = text != null ? text : "";
        String padded = " ".repeat(normalizedWidth)
                + safeText
                + " ".repeat(normalizedWidth);

        List<String> frames = new ArrayList<>();
        for (int position = 0; position < padded.length(); position++) {
            frames.add(generateScrollText(safeText, normalizedWidth, position));
        }
        return frames;
    }

    private static int normalizeWidth(int width) {
        return Math.max(1, width);
    }

    private static int normalizeSpeed(int speed) {
        return Math.max(1, speed);
    }
}
