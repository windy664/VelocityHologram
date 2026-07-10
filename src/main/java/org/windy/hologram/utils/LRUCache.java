package org.windy.hologram.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) 缓存实现。
 * <p>当缓存满时，自动移除最久未使用的条目。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    /**
     * 创建 LRU 缓存。
     *
     * @param maxSize 最大容量
     */
    public LRUCache(int maxSize) {
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }

    /**
     * 创建 LRU 缓存（默认大小 100）。
     */
    public LRUCache() {
        this(100);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    /**
     * 获取最大容量。
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * 获取缓存命中率。
     */
    public double getHitRate() {
        // 简化实现，实际需要追踪命中/未命中次数
        return 0.0;
    }

    /**
     * 检查缓存是否已满。
     */
    public boolean isFull() {
        return size() >= maxSize;
    }

    /**
     * 获取剩余容量。
     */
    public int getRemainingCapacity() {
        return maxSize - size();
    }

    /**
     * 批量放入。
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 获取或计算值。
     */
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        V value = get(key);
        if (value == null) {
            value = mappingFunction.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    /**
     * 创建线程安全的 LRU 缓存。
     */
    public static <K, V> LRUCache<K, V> synchronizedCache(int maxSize) {
        return new LRUCache<K, V>(maxSize) {
            @Override
            public synchronized V get(Object key) {
                return super.get(key);
            }

            @Override
            public synchronized V put(K key, V value) {
                return super.put(key, value);
            }

            @Override
            public synchronized boolean containsKey(Object key) {
                return super.containsKey(key);
            }

            @Override
            public synchronized int size() {
                return super.size();
            }
        };
    }
}
