package org.windy.hologram.animation;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文本动画。
 * <p>支持文本循环、渐变、打字机效果等。
 */
public class TextAnimation {

    private final AnimationType type;
    private final List<String> frames;
    private final int intervalTicks;
    private int currentFrame;
    private int tickCounter;

    public TextAnimation(AnimationType type, List<String> frames, int intervalTicks) {
        this.type = type != null ? type : AnimationType.CYCLE;
        this.frames = frames != null ? frames : Collections.emptyList();
        this.intervalTicks = Math.max(1, intervalTicks);
        this.currentFrame = 0;
        this.tickCounter = 0;
    }

    /**
     * 获取当前帧文本。
     */
    public String getCurrentFrame() {
        if (frames.isEmpty()) return "";
        return frames.get(Math.floorMod(currentFrame, frames.size()));
    }

    /**
     * 推进动画。
     *
     * @return true 如果帧变化了
     */
    public boolean tick() {
        if (frames.size() <= 1) return false;

        tickCounter++;
        if (tickCounter < intervalTicks) {
            return false;
        }

        tickCounter = 0;

        if (type == AnimationType.RANDOM) {
            int previousFrame = currentFrame;
            currentFrame = selectDifferentRandomFrame(previousFrame);
            return currentFrame != previousFrame;
        }

        currentFrame = (currentFrame + 1) % frames.size();
        return true;
    }

    private int selectDifferentRandomFrame(int previousFrame) {
        if (frames.size() <= 1) return previousFrame;

        int candidate = ThreadLocalRandom.current().nextInt(frames.size() - 1);
        if (candidate >= previousFrame) {
            candidate++;
        }
        return candidate;
    }

    /**
     * 重置动画。
     */
    public void reset() {
        currentFrame = 0;
        tickCounter = 0;
    }

    public AnimationType getType() {
        return type;
    }

    public List<String> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    /**
     * 动画类型。
     */
    public enum AnimationType {
        CYCLE,
        RANDOM,
        TYPEWRITER,
        GRADIENT,
        WAVE,
        BURN,
        SCROLL,
        COLORS
    }
}
