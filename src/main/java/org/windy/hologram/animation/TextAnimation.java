package org.windy.hologram.animation;

import java.util.List;

/**
 * 文本动画。
 * <p>支持文本循环、渐变、打字机效果等。
 */
public class TextAnimation {

    private final AnimationType type;
    private final List<String> frames;
    private final int intervalTicks; // 每帧间隔（tick）
    private int currentFrame;
    private int tickCounter;

    public TextAnimation(AnimationType type, List<String> frames, int intervalTicks) {
        this.type = type;
        this.frames = frames;
        this.intervalTicks = intervalTicks;
        this.currentFrame = 0;
        this.tickCounter = 0;
    }

    /**
     * 获取当前帧文本。
     */
    public String getCurrentFrame() {
        if (frames.isEmpty()) return "";
        return frames.get(currentFrame % frames.size());
    }

    /**
     * 推进动画。
     *
     * @return true 如果帧变化了
     */
    public boolean tick() {
        if (frames.size() <= 1) return false;

        tickCounter++;
        if (tickCounter >= intervalTicks) {
            tickCounter = 0;
            currentFrame = (currentFrame + 1) % frames.size();
            return true;
        }
        return false;
    }

    /**
     * 重置动画。
     */
    public void reset() {
        currentFrame = 0;
        tickCounter = 0;
    }

    public AnimationType getType() { return type; }
    public List<String> getFrames() { return frames; }
    public int getIntervalTicks() { return intervalTicks; }

    /**
     * 动画类型。
     */
    public enum AnimationType {
        /** 帧循环：依次显示每一帧 */
        CYCLE,
        /** 随机：随机选择一帧 */
        RANDOM,
        /** 打字机：逐字显示 */
        TYPEWRITER,
        /** 渐变：颜色渐变 */
        GRADIENT
    }
}
