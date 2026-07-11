package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 波浪动画。
 * <p>语法：{wave:amplitude|speed|文本}
 * <p>每个字符有独立的正弦波偏移，使用颜色代码模拟高度变化。
 */
public class WaveAnimation extends TextAnimation {

    private final String baseText;
    private final int amplitude;
    private final int speed;
    private int tick;

    public WaveAnimation(String baseText, int amplitude, int speed) {
        super(
                AnimationType.WAVE,
                generateFrames(baseText, normalizeAmplitude(amplitude), normalizeSpeed(speed)),
                normalizeSpeed(speed)
        );
        this.baseText = baseText != null ? baseText : "";
        this.amplitude = normalizeAmplitude(amplitude);
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
        return generateWaveText(baseText, amplitude, tick);
    }

    private static String generateWaveText(String text, int amplitude, int tick) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        String[] colors = {
                "§f", "§e", "§6", "§c", "§4", "§5", "§d",
                "§b", "§3", "§1", "§9", "§a", "§2"
        };

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

            double offset = Math.sin((tick + index * 0.5) * 0.3) * amplitude;
            double normalized = (offset + amplitude) / (2.0 * amplitude);
            int colorIndex = (int) Math.round(normalized * (colors.length - 1));
            colorIndex = Math.max(0, Math.min(colors.length - 1, colorIndex));

            result.append(colors[colorIndex]).append(character);
        }
        return result.toString();
    }

    private static List<String> generateFrames(String text, int amplitude, int speed) {
        List<String> frames = new ArrayList<>();
        for (int frame = 0; frame < 20; frame++) {
            frames.add(generateWaveText(text, amplitude, frame * speed));
        }
        return frames;
    }

    private static int normalizeAmplitude(int amplitude) {
        return Math.max(1, Math.abs(amplitude));
    }

    private static int normalizeSpeed(int speed) {
        return Math.max(1, speed);
    }
}
