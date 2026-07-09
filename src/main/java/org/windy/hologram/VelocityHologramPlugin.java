package org.windy.hologram;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.HologramPacketListener;
import org.windy.hologram.tracker.PlayerTracker;

import java.time.Duration;

/**
 * VelocityHologram - 纯代理端悬浮字系统。
 * <p>基于 packetevents 实现，无需子服插件配合。
 */
@Plugin(
        id = "velocityhologram",
        name = "VelocityHologram",
        version = "1.0.0",
        authors = {"风吟"}
)
public class VelocityHologramPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private PlayerTracker playerTracker;
    private HologramManager hologramManager;

    @Inject
    public VelocityHologramPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        // 初始化核心组件
        playerTracker = new PlayerTracker();
        hologramManager = new HologramManager(playerTracker);

        // 注册 packetevents 监听器
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(new HologramPacketListener(playerTracker, hologramManager));

        // 启动异步可见性更新（每 500ms）
        proxy.getScheduler().buildTask(this, () -> {
            try {
                hologramManager.tickVisibility();
            } catch (Exception e) {
                logger.error("悬浮字可见性更新异常", e);
            }
        }).repeat(Duration.ofMillis(500)).schedule();

        // 创建示例悬浮字（代理大厅位置）
        createDemoHolograms();

        logger.info("[VelocityHologram] 已启用（packetevents 包级实体模拟）");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // 销毁所有悬浮字
        for (var hologram : hologramManager.getAllHolograms()) {
            hologram.destroy();
        }
        logger.info("[VelocityHologram] 已关闭");
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        // 延迟评估可见性（等待坐标数据）
        proxy.getScheduler().buildTask(this, () -> {
            hologramManager.tickVisibility();
        }).delay(Duration.ofSeconds(1)).schedule();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        playerTracker.remove(player.getUniqueId());
        hologramManager.onPlayerDisconnect(player.getUniqueId());
    }

    /**
     * 创建示例悬浮字。
     */
    private void createDemoHolograms() {
        // 在代理大厅创建一个欢迎悬浮字
        // TODO: 从配置文件加载
        var hologram = hologramManager.createHologram(
                "welcome",
                0.5, 100, 0.5,
                "minecraft:overworld",
                "lobby"
        );
        hologram.addLine("§b§l欢迎来到本服");
        hologram.addLine("§7VelocityHologram v1.0.0");
        hologram.addLine("§a在线玩家: §f0");

        logger.info("[VelocityHologram] 已创建示例悬浮字 'welcome'");
    }
}
