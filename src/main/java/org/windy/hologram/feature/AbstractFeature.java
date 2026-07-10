package org.windy.hologram.feature;

/**
 * 功能特性抽象基类。
 * <p>所有可选功能（如伤害显示、治疗显示）都应继承此类。
 */
public abstract class AbstractFeature {

    protected final String name;
    protected boolean enabled;

    protected AbstractFeature(String name) {
        this.name = name;
        this.enabled = false;
    }

    /**
     * 获取功能名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 检查功能是否启用。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置启用状态。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 启用功能。
     */
    public abstract void enable();

    /**
     * 禁用功能。
     */
    public abstract void disable();

    /**
     * 重新加载配置。
     */
    public abstract void reload();

    /**
     * 销毁功能（清理资源）。
     */
    public abstract void destroy();

    /**
     * 获取功能描述。
     */
    public abstract String getDescription();
}
