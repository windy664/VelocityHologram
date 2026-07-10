package org.windy.hologram.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界事件监听器。
 * <p>通过 packetevents 监听维度切换事件，实现世界加载/卸载处理。
 *
 * <p>功能：
 * <ul>
 *   <li>当玩家切换到新维度时，显示该维度的悬浮字</li>
 *   <li>当所有玩家离开维度时，隐藏该维度的悬浮字</li>
 *   <li>支持 WORLD_UNLOAD DisableCause</li>
 * </ul>
 */
public class WorldListener extends PacketListenerAbstract {

    private final HologramManager hologramManager;

    // 维度 → 当前在该维度的玩家集合
    private final Map<String, Set<UUID>> dimensionPlayers = new ConcurrentHashMap<>();

    // 玩家 → 当前所在维度
    private final Map<UUID, String> playerDimensions = new ConcurrentHashMap<>();

    public WorldListener(HologramManager hologramManager) {
        super(PacketListenerPriority.LOW);
        this.hologramManager = hologramManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            handleJoinGame(event);
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            handleRespawn(event);
        }
    }

    /**
     * 处理 JoinGame 包。
     * <p>玩家首次进入服务器或重新连接时触发。
     */
    private void handleJoinGame(PacketSendEvent event) {
        try {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            String dimension = normalizeDimension(packet.getWorldName());

            // 更新玩家维度
            String oldDimension = playerDimensions.get(playerId);
            if (oldDimension != null) {
                removePlayerFromDimension(playerId, oldDimension);
            }

            playerDimensions.put(playerId, dimension);
            addPlayerToDimension(playerId, dimension);

            // 显示该维度的悬浮字
            onDimensionLoad(dimension);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理 Respawn 包。
     * <p>玩家切换维度时触发。
     */
    private void handleRespawn(PacketSendEvent event) {
        try {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            String dimension = normalizeDimension(packet.getWorldName().orElse(null));

            // 更新玩家维度
            String oldDimension = playerDimensions.get(playerId);
            if (oldDimension != null && !oldDimension.equals(dimension)) {
                removePlayerFromDimension(playerId, oldDimension);

                // 检查旧维度是否还有玩家
                if (isDimensionEmpty(oldDimension)) {
                    onDimensionUnload(oldDimension);
                }
            }

            playerDimensions.put(playerId, dimension);
            addPlayerToDimension(playerId, dimension);

            // 显示新维度的悬浮字
            onDimensionLoad(dimension);
        } catch (Exception ignored) {
        }
    }

    /**
     * 维度加载事件。
     * <p>当第一个玩家进入维度时触发。
     */
    private void onDimensionLoad(String dimension) {
        // 重新显示该维度的悬浮字
        hologramManager.getAllHolograms().stream()
                .filter(Hologram::isEnabled)
                .filter(h -> h.getPosition().dimension().equals(dimension))
                .forEach(hologram -> {
                    // 对该维度的所有玩家更新可见性
                    Set<UUID> players = dimensionPlayers.get(dimension);
                    if (players != null) {
                        for (UUID playerId : players) {
                            hologram.showTo(playerId);
                        }
                    }
                });
    }

    /**
     * 维度卸载事件。
     * <p>当最后一个玩家离开维度时触发。
     */
    private void onDimensionUnload(String dimension) {
        // 隐藏该维度的悬浮字
        hologramManager.getAllHolograms().stream()
                .filter(h -> h.getPosition().dimension().equals(dimension))
                .forEach(hologram -> {
                    // 对所有观察者隐藏
                    for (UUID observer : hologram.getObservers()) {
                        hologram.hideFrom(observer);
                    }
                });
    }

    /**
     * 玩家退出时清理。
     */
    public void onPlayerDisconnect(UUID playerId) {
        String dimension = playerDimensions.remove(playerId);
        if (dimension != null) {
            removePlayerFromDimension(playerId, dimension);

            // 检查维度是否还有玩家
            if (isDimensionEmpty(dimension)) {
                onDimensionUnload(dimension);
            }
        }
    }

    /**
     * 检查维度是否还有玩家。
     */
    private boolean isDimensionEmpty(String dimension) {
        Set<UUID> players = dimensionPlayers.get(dimension);
        return players == null || players.isEmpty();
    }

    /**
     * 添加玩家到维度。
     */
    private void addPlayerToDimension(UUID playerId, String dimension) {
        dimensionPlayers.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet()).add(playerId);
    }

    /**
     * 从维度移除玩家。
     */
    private void removePlayerFromDimension(UUID playerId, String dimension) {
        Set<UUID> players = dimensionPlayers.get(dimension);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                dimensionPlayers.remove(dimension);
            }
        }
    }

    /**
     * 获取玩家当前维度。
     */
    public String getPlayerDimension(UUID playerId) {
        return playerDimensions.get(playerId);
    }

    /**
     * 获取维度内的玩家数。
     */
    public int getDimensionPlayerCount(String dimension) {
        Set<UUID> players = dimensionPlayers.get(dimension);
        return players != null ? players.size() : 0;
    }

    /**
     * 标准化维度名。
     */
    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return "minecraft:overworld";
        }

        // 已经是命名空间格式
        if (dimension.contains(":")) {
            return dimension.toLowerCase();
        }

        // 短名映射
        switch (dimension.toLowerCase()) {
            case "overworld":
            case "the_overworld":
                return "minecraft:overworld";
            case "the_nether":
            case "nether":
                return "minecraft:the_nether";
            case "the_end":
            case "end":
                return "minecraft:the_end";
            default:
                return "minecraft:" + dimension.toLowerCase();
        }
    }
}
