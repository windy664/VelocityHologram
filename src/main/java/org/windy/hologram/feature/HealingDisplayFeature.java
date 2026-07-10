package org.windy.hologram.feature;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
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
 * 治疗显示功能。
 * <p>监控玩家血量变化，在治疗位置生成临时悬浮字显示治疗量。
 */
public class HealingDisplayFeature extends PacketListenerAbstract {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;

    // 配置
    private boolean enabled = true;
    private int duration = 40; // 悬浮字存活时间（tick）
    private String appearance = "§a+{healing}❤";

    // 追踪玩家血量
    private final Map<UUID, Float> playerHealth = new ConcurrentHashMap<>();

    public HealingDisplayFeature(HologramManager hologramManager, PlayerTracker playerTracker) {
        super(PacketListenerPriority.LOW);
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setAppearance(String appearance) { this.appearance = appearance; }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!enabled) return;

        if (event.getPacketType() == PacketType.Play.Server.UPDATE_HEALTH) {
            handleUpdateHealth(event);
        }
    }

    /**
     * 处理血量更新包。
     * <p>检测血量增加并生成治疗显示。
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

            // 检测血量增加
            if (oldHealth != null && newHealth > oldHealth) {
                float healing = newHealth - oldHealth;

                // 生成治疗显示
                spawnHealingHologram(playerId, healing);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 生成治疗悬浮字。
     */
    private void spawnHealingHologram(UUID playerId, float healing) {
        // 格式化治疗文本
        String healingText;
        if (healing == (int) healing) {
            healingText = String.valueOf((int) healing);
        } else {
            healingText = String.format("%.1f", healing);
        }

        String text = appearance.replace("{healing}", healingText);

        // 从 PlayerTracker 获取玩家位置
        PlayerState state = playerTracker.get(playerId);
        if (state == null) return;

        double x = state.getX();
        double y = state.getY() + 2.0; // 在玩家头顶上方
        double z = state.getZ();
        String dimension = state.getDimension();
        String server = state.getServer();

        // 生成唯一名称
        String holoName = "heal_" + playerId.toString().substring(0, 8) + "_" + System.currentTimeMillis();

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
    }
}
