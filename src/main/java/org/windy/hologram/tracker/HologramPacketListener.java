package org.windy.hologram.tracker;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import org.windy.hologram.hologram.HologramManager;

import java.util.UUID;

/**
 * packetevents 包监听器。
 * <p>拦截客户端移动包追踪坐标，拦截服务端 Login/Respawn 包追踪维度。
 */
public class HologramPacketListener extends PacketListenerAbstract {

    private final PlayerTracker playerTracker;
    private final HologramManager hologramManager;

    public HologramPacketListener(PlayerTracker playerTracker, HologramManager hologramManager) {
        super(PacketListenerPriority.LOW);
        this.playerTracker = playerTracker;
        this.hologramManager = hologramManager;
    }

    /**
     * 拦截客户端→服务端包。追踪玩家坐标。
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        String name = user.getName();
        if (name == null) return;

        PacketTypeCommon type = event.getPacketType();

        if (isMovement(type)) {
            handleMovement(event, user, name);
        }
    }

    /**
     * 拦截服务端→客户端包。追踪维度变化。
     */
    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        String name = user.getName();
        if (name == null) return;

        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.JOIN_GAME) {
            handleJoinGame(event, user, name);
        } else if (type == PacketType.Play.Server.RESPAWN) {
            handleRespawn(event, user, name);
        }
    }

    private boolean isMovement(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION;
    }

    /**
     * 从移动包中提取坐标和朝向。
     */
    private void handleMovement(PacketReceiveEvent event, User user, String name) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            PacketTypeCommon type = event.getPacketType();
            double x, y, z;
            float yaw = 0, pitch = 0;

            if (type == PacketType.Play.Client.PLAYER_POSITION) {
                // 位置包: x, y, z, onGround
                x = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
                y = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
                z = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
            } else if (type == PacketType.Play.Client.PLAYER_ROTATION) {
                // 朝向包: yaw, pitch, onGround
                yaw = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readFloat(buf);
                pitch = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readFloat(buf);
                com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);
                // 朝向包不包含位置，只更新朝向
                UUID playerId = user.getUUID();
                if (playerId != null) {
                    PlayerState state = playerTracker.getOrCreate(playerId);
                    state.setName(name);
                    state.setRotation(yaw, pitch);
                }
                return;
            } else {
                // 位置+朝向包: x, y, z, yaw, pitch, onGround
                x = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
                y = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
                z = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
                yaw = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readFloat(buf);
                pitch = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readFloat(buf);
            }

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
                state.setPosition(x, y, z);
                if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    state.setRotation(yaw, pitch);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 从 Join Game 包中提取维度。
     * <p>使用 packetevents 的 WrapperPlayServerJoinGame 解析。
     */
    private void handleJoinGame(PacketSendEvent event, User user, String name) {
        try {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            String dimension = normalizeDimension(packet.getWorldName());

            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
                state.setDimension(dimension);
            }
        } catch (Exception e) {
            // 降级：使用默认维度
            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
                state.setDimension("minecraft:overworld");
            }
        }
    }

    /**
     * 从 Respawn 包中提取维度。
     * <p>使用 packetevents 的 WrapperPlayServerRespawn 解析。
     */
    private void handleRespawn(PacketSendEvent event, User user, String name) {
        try {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            String worldName = packet.getWorldName().orElse(null);
            String dimension = normalizeDimension(worldName);

            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
                state.setDimension(dimension);
            }
        } catch (Exception e) {
            // 降级：保持当前维度
            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
            }
        }
    }

    /**
     * 标准化维度名。
     * <p>MC 不同版本维度格式不同，统一为命名空间格式。
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
