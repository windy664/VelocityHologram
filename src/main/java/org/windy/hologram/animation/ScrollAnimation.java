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
        super(AnimationType.SCROLL, generateFrames(baseText, width, speed), speed);
        this.baseText = baseText;
        this.width = width;
        this.speed = speed;
        this.tick = 0;
        this.position = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        if (tick % speed == 0) {
            position++;
            return true;
        }
        return false;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText == null || baseText.isEmpty()) return "";
        return generateScrollText(baseText, width, position);
    }

    /**
     * 生成滚动文本。
     * <p>文字在固定宽度内滚动。
     */
    private static String generateScrollText(String text, int width, int position) {
        // 添加空白填充
        String padded = " ".repeat(width) + text + " ".repeat(width);
        int totalLength = padded.length();

        // 计算滚动位置
        int start = position % totalLength;
        int end = Math.min(start + width, totalLength);

        // 处理循环
        if (end > totalLength) {
            return padded.substring(start) + padded.substring(0, end - totalLength);
        }

        return padded.substring(start, end);
    }

    /**
     * 生成帧列表（用于初始化）。
     */
    private static List<String> generateFrames(String text, int width, int speed) {
        List<String> frames = new ArrayList<>();
        String padded = " ".repeat(width) + text + " ".repeat(width);
        int totalLength = padded.length();

        for (int i = 0; i < totalLength; i += speed) {
            frames.add(generateScrollText(text, width, i));
        }
        return frames;
    }
}
