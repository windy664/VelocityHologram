package org.windy.hologram.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.tracker.PlayerTracker;

import java.time.Duration;

/**
 * 玩家事件监听器。
 * <p>处理玩家加入、退出、切换服务器等事件。
 */
public class PlayerListener {

    private final HologramManager hologramManager;
    private final PlayerTracker playerTracker;
    private final ProxyServer proxy;
    private final Object plugin;

    public PlayerListener(HologramManager hologramManager, PlayerTracker playerTracker,
                          ProxyServer proxy, Object plugin) {
        this.hologramManager = hologramManager;
        this.playerTracker = playerTracker;
        this.proxy = proxy;
        this.plugin = plugin;
    }

    /**
     * 玩家加入服务器。
     * <p>注册玩家到 PlayerTracker，延迟更新可见性。
     */
    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        playerTracker.register(player.getUniqueId(), player.getUsername());

        // 延迟更新可见性（等待位置信息）
        proxy.getScheduler().buildTask(plugin, () -> {
            hologramManager.updateVisibility(player.getUniqueId());
        }).delay(Duration.ofSeconds(2)).schedule();
    }

    /**
     * 玩家退出服务器。
     * <p>清理玩家数据，隐藏所有悬浮字。
     */
    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();
        hologramManager.onQuit(player.getUniqueId());
        playerTracker.remove(player.getUniqueId());
    }

    /**
     * 玩家切换服务器。
     * <p>更新服务器信息，重新评估可见性。
     */
    @Subscribe
    public void onPlayerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        // 更新服务器信息
        var state = playerTracker.get(player.getUniqueId());
        if (state != null) {
            state.setServer(server);
        }

        // 延迟重新评估可见性
        proxy.getScheduler().buildTask(plugin, () -> {
            hologramManager.updateVisibility(player.getUniqueId());
        }).delay(Duration.ofSeconds(1)).schedule();
    }
}
