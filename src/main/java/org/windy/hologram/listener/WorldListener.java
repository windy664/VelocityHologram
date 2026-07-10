package org.windy.hologram.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import org.windy.hologram.hologram.HologramManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界事件监听器。
 * <p>通过 packetevents 监听维度切换事件，实现世界加载/卸载处理。
 */
public class WorldListener extends PacketListenerAbstract {

    private final HologramManager hologramManager;
    private final Map<String, Set<UUID>> dimensionPlayers = new ConcurrentHashMap<>();
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

    private void handleJoinGame(PacketSendEvent event) {
        try {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            changeDimension(playerId, normalizeDimension(packet.getWorldName()));
        } catch (Exception ignored) {
        }
    }

    private void handleRespawn(PacketSendEvent event) {
        try {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            changeDimension(
                    playerId,
                    normalizeDimension(packet.getWorldName().orElse(null))
            );
        } catch (Exception ignored) {
        }
    }

    private void changeDimension(UUID playerId, String newDimension) {
        String oldDimension = playerDimensions.put(playerId, newDimension);

        if (oldDimension != null && !oldDimension.equals(newDimension)) {
            removePlayerFromDimension(playerId, oldDimension);
            hologramManager.hideAll(playerId);
        }

        addPlayerToDimension(playerId, newDimension);

        /*
         * 不直接调用 Hologram.showTo()。
         * 可见性必须由 HologramManager 统一检查服务器、维度、距离、
         * 权限、启用状态以及每玩家 hide/show 状态。
         */
        hologramManager.updateVisibility(playerId);
    }

    /**
     * 玩家退出时清理。
     */
    public void onPlayerDisconnect(UUID playerId) {
        String dimension = playerDimensions.remove(playerId);
        if (dimension != null) {
            removePlayerFromDimension(playerId, dimension);
        }
    }

    private void addPlayerToDimension(UUID playerId, String dimension) {
        dimensionPlayers.computeIfAbsent(
                dimension,
                key -> ConcurrentHashMap.newKeySet()
        ).add(playerId);
    }

    private void removePlayerFromDimension(UUID playerId, String dimension) {
        Set<UUID> players = dimensionPlayers.get(dimension);
        if (players == null) return;

        players.remove(playerId);
        if (players.isEmpty()) {
            dimensionPlayers.remove(dimension, players);
        }
    }

    public String getPlayerDimension(UUID playerId) {
        return playerDimensions.get(playerId);
    }

    public int getDimensionPlayerCount(String dimension) {
        Set<UUID> players = dimensionPlayers.get(dimension);
        return players != null ? players.size() : 0;
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return "minecraft:overworld";
        }

        if (dimension.contains(":")) {
            return dimension.toLowerCase();
        }

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
