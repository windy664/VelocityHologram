package org.windy.hologram;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.windy.hologram.action.ActionContext;
import org.windy.hologram.action.ClickHandler;
import org.windy.hologram.command.HologramCommand;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.placeholder.PlaceholderManager;
import org.windy.hologram.tracker.HologramPacketListener;
import org.windy.hologram.tracker.PlayerTracker;

import java.nio.file.Path;
import java.time.Duration;

/**
 * VelocityHologram - 纯代理端悬浮字系统。
 * <p>基于 packetevents 实现，无需子服插件配合。
 *
 * <p>特性：
 * <ul>
 *   <li>Text Display 实体（1.19.4+）</li>
 *   <li>点击动作（命令/URL/RCON）</li>
 *   <li>文本动画（循环/随机/打字机）</li>
 *   <li>占位符（在线人数/玩家名等）</li>
 *   <li>空间分区优化</li>
 *   <li>YAML 持久化</li>
 * </ul>
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
    private final Path dataDir;
    private PlayerTracker playerTracker;
    private HologramManager hologramManager;
    private HologramLoader hologramLoader;
    private ClickHandler clickHandler;
    private PlaceholderManager placeholderManager;

    @Inject
    public VelocityHologramPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        // 初始化动作上下文
        ActionContext.init(proxy);

        // 初始化核心组件
        playerTracker = new PlayerTracker();
        clickHandler = new ClickHandler();
        placeholderManager = new PlaceholderManager(proxy);
        hologramManager = new HologramManager(playerTracker, clickHandler, placeholderManager);
        hologramLoader = new HologramLoader(dataDir);

        // 注册 packetevents 监听器
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(new HologramPacketListener(playerTracker, hologramManager));
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager()
                .registerListener(clickHandler);

        // 注册命令
        HologramCommand command = new HologramCommand(hologramManager, playerTracker, hologramLoader);
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("holo").build(),
                command
        );

        // 启动异步可见性更新（每 500ms）
        proxy.getScheduler().buildTask(this, () -> {
            try {
                hologramManager.tickVisibility();
            } catch (Exception e) {
                logger.error("悬浮字可见性更新异常", e);
            }
        }).repeat(Duration.ofMillis(500)).schedule();

        // 启动动画更新（每 100ms = 2 tick）
        proxy.getScheduler().buildTask(this, () -> {
            try {
                hologramManager.tickAnimation();
            } catch (Exception e) {
                logger.error("悬浮字动画更新异常", e);
            }
        }).repeat(Duration.ofMillis(100)).schedule();

        // 加载配置中的悬浮字
        hologramLoader.loadAll(hologramManager);

        logger.info("[VelocityHologram] 已启用（packetevents 包级实体模拟）");
        logger.info("[VelocityHologram] 已加载 " + hologramManager.getAllHolograms().size() + " 个悬浮字");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // 保存所有悬浮字配置
        for (Hologram hologram : hologramManager.getAllHolograms()) {
            hologramLoader.save(hologram);
            hologram.destroy();
        }
        logger.info("[VelocityHologram] 已关闭");
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        // 注册玩家到追踪器
        playerTracker.register(player.getUniqueId(), player.getUsername());
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        // 更新玩家服务器
        String server = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        var state = playerTracker.get(player.getUniqueId());
        if (state != null) {
            state.setServer(server);
        }

        // 延迟评估可见性
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
}
