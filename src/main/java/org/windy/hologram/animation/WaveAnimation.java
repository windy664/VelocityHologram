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
        super(AnimationType.WAVE, generateFrames(baseText, amplitude, speed), speed);
        this.baseText = baseText;
        this.amplitude = amplitude;
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
        return generateWaveText(baseText, amplitude, tick);
    }

    /**
     * 生成波浪文本。
     * <p>使用颜色代码模拟高度变化。
     */
    private static String generateWaveText(String text, int amplitude, int tick) {
        StringBuilder sb = new StringBuilder();
        String[] colors = {"§f", "§e", "§6", "§c", "§4", "§5", "§d", "§b", "§3", "§1", "§9", "§a", "§2"};

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§') {
                // 跳过颜色代码
                sb.append(c);
                if (i + 1 < text.length()) {
                    sb.append(text.charAt(i + 1));
                    i++;
                }
                continue;
            }

            // 计算波浪偏移
            double offset = Math.sin((tick + i * 0.5) * 0.3) * amplitude;
            int colorIndex = (int) ((offset + amplitude) / (2.0 * amplitude) * (colors.length - 1));
            colorIndex = Math.max(0, Math.min(colors.length - 1, colorIndex));

            sb.append(colors[colorIndex]).append(c);
        }
        return sb.toString();
    }

    /**
     * 生成帧列表（用于初始化）。
     */
    private static List<String> generateFrames(String text, int amplitude, int speed) {
        List<String> frames = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            frames.add(generateWaveText(text, amplitude, i * speed));
        }
        return frames;
    }
}
