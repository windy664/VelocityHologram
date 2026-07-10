package org.windy.hologram.api.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 简易事件总线。
 * <p>第三方插件通过 {@link #register(Class, Consumer)} 监听事件。
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() { return INSTANCE; }

    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();

    /**
     * 注册事件监听器。
     *
     * @param type     事件类型
     * @param handler  处理函数
     * @param <T>      事件类型
     */
    public <T> void register(Class<T> type, Consumer<T> handler) {
        registrations.add(new Registration<>(type, handler));
    }

    /**
     * 注销事件监听器。
     */
    public <T> void unregister(Class<T> type, Consumer<T> handler) {
        registrations.removeIf(r -> r.type == type && r.handler == handler);
    }

    /**
     * 触发事件。
     */
    @SuppressWarnings("unchecked")
    public <T> void fire(T event) {
        for (Registration<?> reg : registrations) {
            if (reg.type.isInstance(event)) {
                try {
                    ((Consumer<T>) reg.handler).accept(event);
                } catch (Exception e) {
                    System.err.println("[VelocityHologram] 事件处理异常: " + e.getMessage());
                }
            }
        }
    }

    private static class Registration<T> {
        final Class<T> type;
        final Consumer<T> handler;

        Registration(Class<T> type, Consumer<T> handler) {
            this.type = type;
            this.handler = handler;
        }
    }
}
