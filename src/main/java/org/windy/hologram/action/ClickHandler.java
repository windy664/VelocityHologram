package org.windy.hologram.action;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import org.windy.hologram.hologram.HologramManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点击处理器。
 * <p>拦截 INTERACT_ENTITY 包，检测玩家点击悬浮字实体并执行对应动作。
 * <p>支持 4 种点击类型：LEFT、RIGHT、SHIFT_LEFT、SHIFT_RIGHT。
 */
public class ClickHandler extends PacketListenerAbstract {

    private final Map<Integer, ActionChain> leftClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> rightClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> shiftLeftClickActions = new ConcurrentHashMap<>();
    private final Map<Integer, ActionChain> shiftRightClickActions = new ConcurrentHashMap<>();

    private final Map<UUID, Boolean> sneaking = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();

    private long clickCooldownMs = 1000;
    private HologramManager hologramManager;

    public ClickHandler() {
        super(PacketListenerPriority.LOWEST);
    }

    /**
     * 设置点击冷却时间（秒）。
     */
    public void setClickCooldown(double seconds) {
        this.clickCooldownMs = Math.max(0, (long) (seconds * 1000));
    }

    public void setHologramManager(HologramManager manager) {
        this.hologramManager = manager;
    }

    /**
     * 注册实体的点击动作链。
     */
    public void registerClickAction(int entityId, ActionChain left, ActionChain right,
                                    ActionChain shiftLeft, ActionChain shiftRight) {
        putOrRemove(leftClickActions, entityId, left);
        putOrRemove(rightClickActions, entityId, right);
        putOrRemove(shiftLeftClickActions, entityId, shiftLeft);
        putOrRemove(shiftRightClickActions, entityId, shiftRight);
    }

    /**
     * 注册单个点击动作（兼容旧 API）。
     */
    public void registerClickAction(int entityId, Action left, Action right) {
        registerClickAction(
                entityId,
                toChain(left),
                toChain(right),
                null,
                null
        );
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
        } else if (type == PacketType.Play.Client.ENTITY_ACTION) {
            handleEntityAction(event);
        }
    }

    private void handleInteractEntity(PacketReceiveEvent event) {
        try {
            Object buffer = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buffer);

            int entityId = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buffer);
            int interactionType = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buffer);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buffer);

            User user = event.getUser();
            UUID playerId = user.getUUID();
            if (playerId == null) return;

            String clickType = resolveClickType(
                    interactionType,
                    sneaking.getOrDefault(playerId, false)
            );
            if (clickType == null) return;

            ActionChain registeredAction = getRegisteredAction(entityId, clickType);
            boolean hologramEntity = registeredAction != null || isHologramEntity(entityId);
            if (!hologramEntity) return;

            if (isCoolingDown(playerId)) return;
            lastClickTime.put(playerId, System.currentTimeMillis());

            if (registeredAction != null) {
                registeredAction.execute(playerId);
                return;
            }

            if (hologramManager != null) {
                hologramManager.onClick(playerId, entityId, clickType);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleEntityAction(PacketReceiveEvent event) {
        try {
            Object buffer = event.getByteBuf();
            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.markReaderIndex(buffer);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buffer);
            int actionType = com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.readVarInt(buffer);

            com.github.retrooper.packetevents.netty.buffer.ByteBufHelper.resetReaderIndex(buffer);

            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            if (actionType == 1) {
                sneaking.put(playerId, true);
            } else if (actionType == 2) {
                sneaking.put(playerId, false);
            }
        } catch (Exception ignored) {
        }
    }

    private String resolveClickType(int interactionType, boolean isSneaking) {
        if (interactionType == 0 || interactionType == 2) {
            return isSneaking ? "shift-right" : "right";
        }
        if (interactionType == 1) {
            return isSneaking ? "shift-left" : "left";
        }
        return null;
    }

    private ActionChain getRegisteredAction(int entityId, String clickType) {
        switch (clickType) {
            case "left":
                return leftClickActions.get(entityId);
            case "right":
                return rightClickActions.get(entityId);
            case "shift-left":
                return shiftLeftClickActions.get(entityId);
            case "shift-right":
                return shiftRightClickActions.get(entityId);
            default:
                return null;
        }
    }

    private boolean isHologramEntity(int entityId) {
        if (hologramManager == null) return false;
        return hologramManager.getAllHolograms().stream()
                .anyMatch(hologram -> hologram.findPageByEntityId(entityId) >= 0);
    }

    private boolean isCoolingDown(UUID playerId) {
        if (clickCooldownMs <= 0) return false;

        Long lastClick = lastClickTime.get(playerId);
        return lastClick != null
                && System.currentTimeMillis() - lastClick < clickCooldownMs;
    }

    private ActionChain toChain(Action action) {
        return action == null
                ? null
                : new ActionChain(java.util.Collections.singletonList(action));
    }

    private void putOrRemove(Map<Integer, ActionChain> map, int entityId, ActionChain action) {
        if (action == null) {
            map.remove(entityId);
        } else {
            map.put(entityId, action);
        }
    }

    /**
     * 玩家断开时清理状态。
     */
    public void onPlayerDisconnect(UUID playerId) {
        sneaking.remove(playerId);
        lastClickTime.remove(playerId);
    }
}
