package org.windy.hologram.display;

import java.util.EnumMap;
import java.util.Map;

/**
 * Display 实体工厂注册表。
 * <p>管理所有 {@link DisplayEntityFactory} 实例，按类型分发。
 * <p>扩展方式：实现 {@link DisplayEntityFactory}，调用 {@link #register} 注册。
 */
public class DisplayFactoryRegistry {

    private final Map<DisplayEntityType, DisplayEntityFactory> factories = new EnumMap<>(DisplayEntityType.class);

    /**
     * 注册工厂。
     */
    public void register(DisplayEntityType type, DisplayEntityFactory factory) {
        factories.put(type, factory);
    }

    /**
     * 获取工厂。
     *
     * @throws IllegalArgumentException 未注册的类型
     */
    public DisplayEntityFactory get(DisplayEntityType type) {
        DisplayEntityFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("未注册的 Display 类型: " + type);
        }
        return factory;
    }

    /**
     * 获取工厂（安全版，未注册返回 null）。
     */
    public DisplayEntityFactory getOrNull(DisplayEntityType type) {
        return factories.get(type);
    }

    /**
     * 检查类型是否已注册。
     */
    public boolean isRegistered(DisplayEntityType type) {
        return factories.containsKey(type);
    }
}
