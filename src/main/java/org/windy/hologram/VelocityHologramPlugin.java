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
import org.windy.hologram.api.VelocityHologramAPI;
import org.windy.hologram.command.HologramCommand;
import org.windy.hologram.config.HologramLoader;
import org.windy.hologram.config.Lang;
import org.windy.hologram.config.PluginConfig;
import org.windy.hologram.display.*;
import org.windy.hologram.rcon.RconPool;
import org.windy.hologram.hologram.Hologram;
import org.windy.hologram.hologram.HologramManager;
import org.windy.hologram.placeholder.PlaceholderManager;
import org.windy.hologram.tracker.HologramPacketListener;
import org.windy.hologram.tracker.PlayerState;
import org.windy.hologram.tracker.PlayerTracker;

import java.nio.file.Path;
import java.time.Duration;

/**
 * VelocityHologram - 纯代理端悬浮字系统。
 * <p>基于 packetevents 实现，无需子服插件配合。
 *
 * <p>特性：
 * <ul>
 *   <li>Text / Item / Block Display 实体（1.19.4+）</li>
 *   <li>点击动作（命令/URL/RCON）</li>
 *   <li>文本动画（循环/随机/打字机/渐变）</li>
 *   <li>占位符（在线人数/玩家名等）</li>
 *   <li>视觉效果（Billboard/缩放/透明度/背景）</li>
 *   <li>权限控制</li>
 *   <li>空间分区优化</li>
 *   <li>YAML 持久化</li>
 * </ul>
 */
@Plugin(
        id = "velocityhologram",
        name = "VelocityHologram",
        version = "1.2.0",
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
    private DisplayFactoryRegistry displayRegistry;
    private PluginConfig pluginConfig;
    private RconPool rconPool;
    private org.windy.hologram.listener.WorldListener worldListener;
    private boolean peSelfInitialized = false;

    @Inject
    public VelocityHologramPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        // packetevents 智能初始化：已有则复用，没有则自己初始化
        var peAPI = com.github.retrooper.packetevents.PacketEvents.getAPI();
        if (peAPI == null) {
            var pluginContainer = proxy.getPluginManager()
                    .getPlugin("velocityhologram")
                    .orElse(null);
            if (pluginContainer != null) {
                peAPI = io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder
                        .build(proxy, pluginContainer, logger, dataDir);
                com.github.retrooper.packetevents.PacketEvents.setAPI(peAPI);
                peAPI.load();
                peAPI.init();
                peSelfInitialized = true;
                logger.info("[VelocityHologram] PacketEvents 已自行初始化");
            }
        } else {
            logger.info("[VelocityHologram] 复用已有的 PacketEvents 实例");
        }

        if (peAPI == null) {
            logger.error("[VelocityHologram] PacketEvents 初始化失败，插件无法启用");
            return;
        }

        // 初始化动作上下文
        ActionContext.init(proxy);

        // 加载主配置 + 语言 + 属性默认值
        pluginConfig = new PluginConfig(dataDir);
        pluginConfig.load();
        Lang.load(dataDir);

        // 加载 Display 属性默认值
        var attributeDefaults = new org.windy.hologram.config.AttributeDefaults(dataDir);
        attributeDefaults.load();
        org.windy.hologram.display.DisplayConfig.setAttributeDefaults(attributeDefaults);
        logger.info("[VelocityHologram] 已加载 Display 属性默认值");

        // 初始化 RCON 连接池
        var rconServers = pluginConfig.getRconServers();
        if (!rconServers.isEmpty()) {
            rconPool = new RconPool(rconServers);
            ActionContext.setRconPool(rconPool);
            logger.info("[VelocityHologram] RCON 已配置 " + rconServers.size() + " 个服务器");
        }

        // 初始化 Display 工厂注册表
        displayRegistry = new DisplayFactoryRegistry();
        displayRegistry.register(DisplayEntityType.TEXT_DISPLAY, new TextDisplayFactory());
        displayRegistry.register(DisplayEntityType.ITEM_DISPLAY, new ItemDisplayFactory());
        displayRegistry.register(DisplayEntityType.BLOCK_DISPLAY, new BlockDisplayFactory());
        displayRegistry.register(DisplayEntityType.ENTITY, new EntityFactory());
        displayRegistry.register(DisplayEntityType.HEAD, new HeadFactory(false));
        displayRegistry.register(DisplayEntityType.SMALLHEAD, new HeadFactory(true));
        displayRegistry.register(DisplayEntityType.ICON, new IconFactory());

        // 初始化核心组件
        playerTracker = new PlayerTracker();
        clickHandler = new ClickHandler();
        clickHandler.setClickCooldown(pluginConfig.getClickCooldown());
        placeholderManager = new PlaceholderManager(proxy);
        hologramManager = new HologramManager(playerTracker, clickHandler, placeholderManager, displayRegistry,
                uuid -> proxy.getPlayer(uuid).orElse(null));
        hologramManager.setLogger(logger);
        VelocityHologramAPI.setManager(hologramManager);

        // 设置权限检查器（使用 Velocity 的 Player.hasPermission）
        hologramManager.setPermissionChecker((playerId, permission) -> {
            Player player = proxy.getPlayer(playerId).orElse(null);
            return player == null || player.hasPermission(permission);
        });

        hologramLoader = new HologramLoader(dataDir, displayRegistry);

        // 注册 packetevents 监听器
        peAPI.getEventManager()
                .registerListener(new HologramPacketListener(playerTracker, hologramManager));
        peAPI.getEventManager()
                .registerListener(clickHandler);

        // 注册世界监听器（通过 packetevents 监听维度切换）
        org.windy.hologram.listener.WorldListener worldListener = new org.windy.hologram.listener.WorldListener(hologramManager);
        peAPI.getEventManager().registerListener(worldListener);
        this.worldListener = worldListener;

        // 初始化伤害/治疗显示功能
        if (pluginConfig.isDamageDisplayEnabled()) {
            var damageDisplay = new org.windy.hologram.feature.DamageDisplayFeature(hologramManager, playerTracker);
            damageDisplay.setDuration(pluginConfig.getDamageDisplayDuration());
            damageDisplay.setAppearance(pluginConfig.getDamageDisplayAppearance());
            damageDisplay.setCriticalAppearance(pluginConfig.getDamageDisplayCriticalAppearance());
            peAPI.getEventManager().registerListener(damageDisplay);
            logger.info("[VelocityHologram] 伤害显示已启用");
        }
        if (pluginConfig.isHealingDisplayEnabled()) {
            var healingDisplay = new org.windy.hologram.feature.HealingDisplayFeature(hologramManager, playerTracker);
            healingDisplay.setDuration(pluginConfig.getHealingDisplayDuration());
            healingDisplay.setAppearance(pluginConfig.getHealingDisplayAppearance());
            peAPI.getEventManager().registerListener(healingDisplay);
            logger.info("[VelocityHologram] 治疗显示已启用");
        }

        // 注册命令
        HologramCommand command = new HologramCommand(hologramManager, playerTracker, hologramLoader, clickHandler, proxy, dataDir);
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("holo").build(),
                command
        );

        // 注册玩家监听器
        proxy.getEventManager().register(this, new org.windy.hologram.listener.PlayerListener(hologramManager, playerTracker, proxy, this));

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

        // 补注册插件启动前就已在线的玩家
        for (Player online : proxy.getAllPlayers()) {
            playerTracker.register(online.getUniqueId(), online.getUsername());
            String server = online.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse("unknown");
            PlayerState st = playerTracker.get(online.getUniqueId());
            if (st != null) st.setServer(server);
        }

        logger.info("[VelocityHologram] 已启用 v1.2.0（多页/动作扩展/命令补全）");
        logger.info("[VelocityHologram] 已加载 " + hologramManager.getAllHolograms().size() + " 个悬浮字");
        logger.info("[VelocityHologram] 已注册 " + playerTracker.getAllStates().size() + " 个在线玩家");

        // 延迟触发一次可见性，让已在线玩家看到已有悬浮字
        proxy.getScheduler().buildTask(this, () -> {
            hologramManager.tickVisibility();
        }).delay(Duration.ofSeconds(2)).schedule();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (hologramManager != null && hologramLoader != null) {
            for (Hologram hologram : hologramManager.getAllHolograms()) {
                hologramLoader.save(hologram);
                hologram.destroy();
            }
        }
        if (rconPool != null) {
            rconPool.closeAll();
        }
        // 只有自己初始化的才 terminate，不破坏别人的实例
        if (peSelfInitialized) {
            com.github.retrooper.packetevents.PacketEvents.getAPI().terminate();
        }
        logger.info("[VelocityHologram] 已关闭");
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        if (playerTracker == null) return;
        Player player = event.getPlayer();
        playerTracker.register(player.getUniqueId(), player.getUsername());
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        if (playerTracker == null || hologramManager == null) return;
        Player player = event.getPlayer();
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
        if (playerTracker == null || hologramManager == null) return;
        Player player = event.getPlayer();
        playerTracker.remove(player.getUniqueId());
        hologramManager.onPlayerDisconnect(player.getUniqueId());
        if (worldListener != null) {
            worldListener.onPlayerDisconnect(player.getUniqueId());
        }
    }
}
