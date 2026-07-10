package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * 燃烧动画。
 * <p>语法：{burn:duration|文本}
 * <p>文字从上到下逐行"燃烧"消失，使用随机字符模拟火焰。
 */
public class BurnAnimation extends TextAnimation {

    private final String baseText;
    private final int duration;
    private int tick;

    public BurnAnimation(String baseText, int duration) {
        super(AnimationType.BURN, generateFrames(baseText, duration), 1);
        this.baseText = baseText;
        this.duration = duration;
        this.tick = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        return tick % 2 == 0;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText == null || baseText.isEmpty()) return "";
        return generateBurnText(baseText, duration, tick);
    }

    /**
     * 生成燃烧文本。
     * <p>文字从上到下逐行"燃烧"消失。
     */
    private static String generateBurnText(String text, int duration, int tick) {
        int burnLength = (int) ((tick % (duration * 2)) / (double) (duration * 2) * text.length());

        StringBuilder sb = new StringBuilder();
        String fireChars = "§c§6§e§f";

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

            if (i < burnLength) {
                // 已燃烧部分：使用随机火焰字符
                char fireChar = fireChars.charAt((int) (Math.random() * fireChars.length()));
                sb.append("§").append(fireChar).append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 生成帧列表（用于初始化）。
     */
    private static List<String> generateFrames(String text, int duration) {
        List<String> frames = new ArrayList<>();
        for (int i = 0; i < duration * 2; i++) {
            frames.add(generateBurnText(text, duration, i));
        }
        return frames;
    }
}
