package org.windy.hologram.feature;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDamageEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth;
import org.windy.hologram.api.DHAPI;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 伤害显示功能。
 * <p>监控玩家血量变化，在受伤位置生成临时悬浮字显示伤害。
 */
public class DamageDisplayFeature extends PacketListenerAbstract {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;

    // 配置
    private boolean enabled = true;
    private int duration = 40; // 悬浮字存活时间（tick）
    private String appearance = "§c-{damage}❤";
    private String criticalAppearance = "§4§l暴击！ §c-{damage}❤";

    // 追踪玩家血量
    private final Map<UUID, Float> playerHealth = new ConcurrentHashMap<>();

    // 追踪伤害事件（用于关联血量变化和伤害来源）
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastDamageSource = new ConcurrentHashMap<>();

    public DamageDisplayFeature(HologramManager hologramManager, PlayerTracker playerTracker) {
        super(PacketListenerPriority.LOW);
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setAppearance(String appearance) { this.appearance = appearance; }
    public void setCriticalAppearance(String criticalAppearance) { this.criticalAppearance = criticalAppearance; }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled) return;

        if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
            handleUpdateHealth(event);
        } else if (event.getPacketType() == PacketType.Play.Server.DAMAGE_EVENT) {
            handleDamageEvent(event);
        }
    }

    /**
     * 处理血量更新包。
     * <p>检测血量减少并生成伤害显示。
     */
    private void handleUpdateHealth(PacketSendEvent event) {
        try {
            WrapperPlayServerUpdateHealth packet = new WrapperPlayServerUpdateHealth(event);
            UUID playerId = event.getUser().getUUID();
            if (playerId == null) return;

            float newHealth = packet.getHealth();
            Float oldHealth = playerHealth.get(playerId);

            // 更新血量记录
            playerHealth.put(playerId, newHealth);

            // 检测血量减少
            if (oldHealth != null && newHealth < oldHealth) {
                float damage = oldHealth - newHealth;

                // 检查是否是暴击（伤害超过一定阈值）
                boolean critical = damage >= 10.0f;

                // 生成伤害显示
                spawnDamageHologram(playerId, damage, critical);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理伤害事件包。
     * <p>记录伤害来源信息。
     */
    private void handleDamageEvent(PacketSendEvent event) {
        try {
            WrapperPlayServerDamageEvent packet = new WrapperPlayServerDamageEvent(event);
            int entityId = packet.getEntityId();

            // 记录伤害来源
            lastDamageSource.put(event.getUser().getUUID(), packet.getSourceCauseId());
            lastDamageTime.put(event.getUser().getUUID(), System.currentTimeMillis());
        } catch (Exception ignored) {
        }
    }

    /**
     * 生成伤害悬浮字。
     */
    private void spawnDamageHologram(UUID playerId, float damage, boolean critical) {
        // 格式化伤害文本
        String damageText;
        if (damage == (int) damage) {
            damageText = String.valueOf((int) damage);
        } else {
            damageText = String.format("%.1f", damage);
        }

        String text = critical ? criticalAppearance : appearance;
        text = text.replace("{damage}", damageText);

        // 从 PlayerTracker 获取玩家位置
        PlayerState state = playerTracker.get(playerId);
        if (state == null) return;

        double x = state.getX();
        double y = state.getY() + 2.0; // 在玩家头顶上方
        double z = state.getZ();
        String dimension = state.getDimension();
        String server = state.getServer();

        // 生成唯一名称
        String holoName = "damage_" + playerId.toString().substring(0, 8) + "_" + System.currentTimeMillis();

        // 创建临时悬浮字
        Hologram hologram = DHAPI.createHologram(holoName, x, y, z, dimension, server);
        DHAPI.addLine(hologram, text);

        // 设置悬浮字属性
        hologram.setAlwaysFacePlayer(true);
        hologram.addFlag("always_visible");

        // 显示给玩家
        DHAPI.showHologram(hologram, playerId);

        // 定时删除
        scheduleRemove(holoName, duration);
    }

    /**
     * 定时删除悬浮字。
     */
    private void scheduleRemove(String holoName, int delayTicks) {
        // 使用异步线程延迟删除
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L); // 1 tick = 50ms
                DHAPI.deleteHologram(holoName);
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    /**
     * 清理玩家数据。
     */
    public void onPlayerDisconnect(UUID playerId) {
        playerHealth.remove(playerId);
        lastDamageTime.remove(playerId);
        lastDamageSource.remove(playerId);
    }
}
