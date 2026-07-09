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
    private final Map<String, UUID> nameToUuid = new ConcurrentHashMap<>();

    /**
     * 注册玩家（进服时调用）。
     */
    public void register(UUID playerId, String name) {
        nameToUuid.put(name.toLowerCase(), playerId);
        states.computeIfAbsent(playerId, k -> new PlayerState());
    }

    /**
     * 获取或创建玩家状态。
     */
    public PlayerState getOrCreate(UUID playerId) {
        return states.computeIfAbsent(playerId, k -> new PlayerState());
    }

    /**
     * 通过名字获取 UUID。
     */
    public UUID findUuid(String name) {
        return nameToUuid.get(name.toLowerCase());
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
        PlayerState state = states.remove(playerId);
        if (state != null) {
            nameToUuid.remove(state.getName().toLowerCase());
        }
    }
}
