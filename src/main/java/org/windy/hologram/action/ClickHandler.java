package org.windy.hologram.action;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.hologram.HologramLine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点击处理器。
 * <p>拦截 INTERACT_ENTITY 包，检测玩家点击悬浮字实体并执行对应动作。
 */
public class ClickHandler extends PacketListenerAbstract {

    // entityId → Action 映射
    private final Map<Integer, Action> leftClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, Action> rightClickActions = new ConcurrentHashMap<>();

    public ClickHandler() {
        super(PacketListenerPriority.LOWEST);
    }

    /**
     * 注册实体的点击动作。
     */
    public void registerClickAction(int entityId, Action leftClick, Action rightClick) {
        if (leftClick != null) leftClickActions.put(entityId, leftClick);
        if (rightClick != null) rightClickActions.put(entityId, rightClick);
    }

    /**
     * 注销实体的点击动作。
     */
    public void unregisterClickAction(int entityId) {
        leftClickActions.remove(entityId);
        rightClickActions.remove(entityId);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        // 拦截 INTERACT_ENTITY 包
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(event);
        }
    }

    /**
     * 处理 INTERACT_ENTITY 包。
     * <p>包格式（VarInt 为主）：
     * - Entity ID (VarInt)
     * - Type (VarInt): 0=interact, 1=attack, 2=interact_at
     * - 可选字段取决于 Type
     */
    private void handleInteractEntity(PacketReceiveEvent event) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            // 读取 Entity ID
            int entityId = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf);

            // 读取交互类型
            int type = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            // 根据交互类型查找动作
            User user = event.getUser();
            UUID playerId = user.getUUID();

            if (type == 0) {
                // 交互（右键）
                Action action = rightClickActions.get(entityId);
                if (action != null) {
                    action.execute(playerId);
                }
            } else if (type == 1) {
                // 攻击（左键）
                Action action = leftClickActions.get(entityId);
                if (action != null) {
                    action.execute(playerId);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
