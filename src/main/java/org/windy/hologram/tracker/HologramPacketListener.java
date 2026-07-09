package org.windy.hologram.tracker;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
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
     * 拦截客户端→服务端包。
     * <p>追踪玩家坐标。
     */
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        String name = user.getName();
        if (name == null) return;

        PacketTypeCommon type = event.getPacketType();

        // 追踪坐标
        if (isMovement(type)) {
            handleMovement(event, user, name);
        }
    }

    /**
     * 拦截服务端→客户端包。
     * <p>追踪维度变化。
     */
    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        String name = user.getName();
        if (name == null) return;

        PacketTypeCommon type = event.getPacketType();

        // 追踪维度（Join Game 包）
        if (type == PacketType.Play.Server.JOIN_GAME) {
            handleJoinGame(event, user, name);
        }
        // 追踪维度（Respawn 包）
        else if (type == PacketType.Play.Server.RESPAWN) {
            handleRespawn(event, user, name);
        }
    }

    private boolean isMovement(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    /**
     * 从移动包中提取坐标。
     */
    private void handleMovement(PacketReceiveEvent event, User user, String name) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            double x = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
            double y = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
            double z = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            // 获取或创建玩家状态
            UUID playerId = user.getUUID();
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setName(name);
                state.setPosition(x, y, z);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 从 Join Game 包中提取维度。
     * <p>Join Game 包格式复杂，包含 NBT 数据。
     * 简化实现：暂时使用默认维度。
     */
    private void handleJoinGame(PacketSendEvent event, User user, String name) {
        // TODO: 解析 Join Game 包的 Dimension Type 字段
        // 暂时设置为默认维度
        UUID playerId = user.getUUID();
        if (playerId != null) {
            PlayerState state = playerTracker.getOrCreate(playerId);
            state.setName(name);
            state.setDimension("minecraft:overworld");
        }
    }

    /**
     * 从 Respawn 包中提取维度。
     * <p>Respawn 包格式复杂，包含 NBT 数据。
     * 简化实现：暂时使用默认维度。
     */
    private void handleRespawn(PacketSendEvent event, User user, String name) {
        // TODO: 解析 Respawn 包的 Dimension Type 字段
        // 暂时设置为默认维度
        UUID playerId = user.getUUID();
        if (playerId != null) {
            PlayerState state = playerTracker.getOrCreate(playerId);
            state.setName(name);
        }
    }
}
