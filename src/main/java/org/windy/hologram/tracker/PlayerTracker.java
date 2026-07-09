package org.windy.hologram.tracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家状态追踪器。
 * <p>通过 packetevents 拦截移动/维度包，实时维护所有在线玩家的坐标和维度。
 */
public class PlayerTracker {

    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    /**
     * 获取或创建玩家状态。
     */
    public PlayerState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(playerId, k -> new PlayerState());
    }

    /**
     * 获取玩家状态（可能为 null）。
     */
    public PlayerState get(UUID playerId) {
        return states.get(playerId);
    }

    /**
     * 获取所有玩家状态。
     */
    public Map<UUID, PlayerState> getAllStates() {
        return states;
    }

    /**
     * 玩家断开连接时移除。
     */
    public void remove(UUID playerId) {
        states.remove(playerId);
    }
}
