package org.windy.hologram.action;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点击处理器。
 * <p>拦截 INTERACT_ENTITY 包，检测玩家点击悬浮字实体并执行对应动作。
 * <p>支持 4 种点击类型：LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT。
 */
public class ClickHandler extends PacketListenerAbstract {

    // entityId → 动作链（4 种点击类型）
    private final Map<Integer, ActionChain> leftClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> rightClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> shiftLeftClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> shiftRightClickActions = new ConcurrentHashMap<>();

    // 玩家潜行状态追踪
    private final Map<UUID, Boolean> sneaking = new ConcurrentHashMap<>();

    // Hologram 引用（用于 page 动作）
    private HologramManager hologramManager;

    public ClickHandler() {
        super(PacketListenerPriority.LOWEST);
    }

    public void setHologramManager(HologramManager manager) {
        this.hologramManager = manager;
    }

    /**
     * 注册实体的点击动作链。
     */
    public void registerClickAction(int entityId, ActionChain left, ActionChain right,
                                     ActionChain shiftLeft, ActionChain shiftRight) {
        if (left != null) leftClickActions.put(entityId, left);
        else leftClickActions.remove(entityId);
        if (right != null) rightClickActions.put(entityId, right);
        else rightClickActions.remove(entityId);
        if (shiftLeft != null) shiftLeftClickActions.put(entityId, shiftLeft);
        else shiftLeftClickActions.remove(entityId);
        if (shiftRight != null) shiftRightClickActions.put(entityId, shiftRight);
        else shiftRightClickActions.remove(entityId);
    }

    /**
     * 注册单个点击动作（兼容旧 API）。
     */
    public void registerClickAction(int entityId, Action left, Action right) {
        if (left != null) leftClickActions.put(entityId, new ActionChain(java.util.Collections.singletonList(left)));
        else leftClickActions.remove(entityId);
        if (right != null) rightClickActions.put(entityId, new ActionChain(java.util.Collections.singletonList(right)));
        else rightClickActions.remove(entityId);
    }

    /**
     * 注销实体的所有点击动作。
     */
    public void unregisterClickAction(int entityId) {
        leftClickActions.remove(entityId);
        rightClickActions.remove(entityId);
        shiftLeftClickActions.remove(entityId);
        shiftRightClickActions.remove(entityId);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteractEntity(event);
        }
        // 追踪潜行状态
        else if (type == PacketType.Play.Client.ENTITY_ACTION) {
            handleEntityAction(event);
        }
    }

    /**
     * 处理 INTERACT_ENTITY 包。
     * <p>包格式：EntityID(VarInt) + Type(VarInt) + 可选字段
     * <p>Type: 0=interact(右键), 1=attack(左键), 2=interact_at
     */
    private void handleInteractEntity(PacketReceiveEvent event) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            int entityId = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf);
            int type = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            User user = event.getUser();
            UUID playerId = user.getUUID();
            if (playerId == null) return;

            boolean isSneaking = sneaking.getOrDefault(playerId, false);

            ActionChain action = null;
            if (type == 0) {
                // 右键
                action = isSneaking ? shiftRightClickActions.get(entityId) : rightClickActions.get(entityId);
            } else if (type == 1) {
                // 左键
                action = isSneaking ? shiftLeftClickActions.get(entityId) : leftClickActions.get(entityId);
            }

            if (action != null) {
                action.execute(playerId);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理 ENTITY_ACTION 包，追踪潜行状态。
     * <p>ActionType: 1=START_SNEAKING, 2=STOP_SNEAKING
     */
    private void handleEntityAction(PacketReceiveEvent event) {
        try {
            Object buf = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf); // entityId
            int actionType = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buf);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buf);

            User user = event.getUser();
            UUID playerId = user.getUUID();
            if (playerId == null) return;

            if (actionType == 1) {
                sneaking.put(playerId, true);
            } else if (actionType == 2) {
                sneaking.put(playerId, false);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 玩家断开时清理潜行状态。
     */
    public void onPlayerDisconnect(UUID playerId) {
        sneaking.remove(playerId);
    }
}
