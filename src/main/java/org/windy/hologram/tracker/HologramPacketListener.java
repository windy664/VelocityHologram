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
        super(PacketListenerPriority.LOW); // 低优先级，不影响其他插件
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
            handleMovement(event, name);
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

        // 追踪维度
        // TODO: 找到正确的 packetevents 包类型常量
        // if (type == PacketType.Play.Server.JOIN_GAME || type == PacketType.Play.Server.RESPAWN) {
        //     handleDimensionChange(event, name);
        // }
    }

    private boolean isMovement(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    /**
     * 从移动包中提取坐标。
     */
    private void handleMovement(PacketReceiveEvent event, String name) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            double x = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
            double y = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);
            double z = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readDouble(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            // 通过 UUID 获取玩家状态（需要从 name 映射到 UUID）
            // TODO: 维护 name -> UUID 映射
            UUID playerId = findPlayerId(name);
            if (playerId != null) {
                PlayerState state = playerTracker.getOrCreate(playerId);
                state.setPosition(x, y, z);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 从 Login/Respawn 包中提取维度。
     * <p>这些包的格式比较复杂，包含 NBT 数据。
     * TODO: 正确解析维度字段
     */
    private void handleDimensionChange(PacketSendEvent event, String name) {
        // 简化实现：暂时跳过维度解析
        // 实际需要解析 Login/Respawn 包的 Dimension Type 字段
    }

    /**
     * 通过玩家名查找 UUID。
     * TODO: 实现 name -> UUID 映射
     */
    private UUID findPlayerId(String name) {
        // Velocity: proxy.getPlayer(name).map(Player::getUniqueId)
        // 需要持有 proxy 引用
        return null;
    }
}
