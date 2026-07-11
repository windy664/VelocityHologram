package org.windy.hologram.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 燃烧动画。
 * <p>语法：{burn:duration|文本}
 * <p>文字逐步变为随机火焰颜色。
 */
public class BurnAnimation extends TextAnimation {

    private static final char[] FIRE_COLORS = {'c', '6', 'e', 'f'};

    private final String baseText;
    private final int duration;
    private int tick;

    public BurnAnimation(String baseText, int duration) {
        super(
                AnimationType.BURN,
                generateFrames(baseText, normalizeDuration(duration)),
                1
        );
        this.baseText = baseText != null ? baseText : "";
        this.duration = normalizeDuration(duration);
        this.tick = 0;
    }

    @Override
    public boolean tick() {
        tick++;
        return tick % 2 == 0;
    }

    @Override
    public String getCurrentFrame() {
        if (baseText.isEmpty()) return "";
        return generateBurnText(baseText, duration, tick);
    }

    private static String generateBurnText(String text, int duration, int tick) {
        if (text == null || text.isEmpty()) return "";

        int normalizedDuration = normalizeDuration(duration);
        int cycleLength = normalizedDuration * 2;
        int cycleTick = Math.floorMod(tick, cycleLength);
        int visibleCharacters = countVisibleCharacters(text);
        int burnLength = (int) (
                cycleTick / (double) cycleLength * visibleCharacters
        );

        StringBuilder result = new StringBuilder();
        int visibleIndex = 0;

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

            if (visibleIndex < burnLength && !Character.isWhitespace(character)) {
                char color = FIRE_COLORS[
                        ThreadLocalRandom.current().nextInt(FIRE_COLORS.length)
                ];
                result.append('§').append(color);
            }

            result.append(character);
            visibleIndex++;
        }

        return result.toString();
    }

    private static int countVisibleCharacters(String text) {
        int count = 0;

        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '§' && index + 1 < text.length()) {
                index++;
                continue;
            }
            count++;
        }

        return count;
    }

    private static List<String> generateFrames(String text, int duration) {
        int normalizedDuration = normalizeDuration(duration);
        List<String> frames = new ArrayList<>();

        for (int frame = 0; frame < normalizedDuration * 2; frame++) {
            frames.add(generateBurnText(text, normalizedDuration, frame));
        }

        return frames;
    }

    private static int normalizeDuration(int duration) {
        return Math.max(1, duration);
    }
}
