package org.windy.hologram.hologram;

import org.windy.hologram.action.Action;
import org.windy.hologram.action.ActionFactory;
import org.windy.hologram.animation.GradientParser;
import org.windy.hologram.animation.TextAnimation;
import org.windy.hologram.api.IHologramLine;
import org.windy.hologram.display.DisplayConfig;
import org.windy.hologram.display.DisplayEntityFactory;
import org.windy.hologram.display.DisplayEntityType;
import org.windy.hologram.display.DisplayFactoryRegistry;
import org.windy.hologram.placeholder.PlaceholderManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 悬浮字单行实现。
 * <p>每行对应一个 Display 实体（Text/Item/Block），拥有独立的实体 ID。
 * <p>支持动作（点击执行命令）和动画（文本循环）。
 */
public class HologramLine implements IHologramLine {

    // 实体 ID 从高位递减，避开子服真实实体
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 10000);

    private final int entityId;
    private int index;

    // 显示配置（统一持有，替代原来的 text 字段）
    private volatile DisplayConfig displayConfig;

    // 原始文本（含占位符和动画语法，用于 Text Display）
    private volatile String rawText;

    // 行偏移
    private double offsetY;
    private double offsetX;
    private double offsetZ;

    // 行高（0 = 使用默认行间距）
    private double lineHeight = 0;

    // 行级权限
    private String permission;

    // 行级 Flag
    private final Set<String> flags = ConcurrentHashMap.newKeySet();

    // 动作
    private Action leftClickAction;
    private Action rightClickAction;
    private Action shiftLeftClickAction;
    private Action shiftRightClickAction;

    // 动画
    private TextAnimation animation;

    public HologramLine(int index, String text) {
        this.entityId = ENTITY_ID_COUNTER.decrementAndGet();
        this.index = index;
        this.rawText = text;
        this.displayConfig = DisplayConfig.builder(DisplayEntityType.TEXT_DISPLAY).text(text).build();
    }

    /**
     * 创建指定类型的行。
     */
    public HologramLine(int index, DisplayConfig config) {
        this.entityId = ENTITY_ID_COUNTER.decrementAndGet();
        this.index = index;
        this.displayConfig = config;
        this.rawText = config.text();
    }

    @Override
    public int getIndex() { return index; }

    void setIndex(int index) { this.index = index; }

    @Override
    public String getText() {
        return displayConfig.text() != null ? displayConfig.text() : "";
    }

    @Override
    public void setText(String text) {
        this.rawText = text;
        this.displayConfig = DisplayConfig.builder(displayConfig.type())
                .text(text)
                .itemId(displayConfig.itemId())
                .blockId(displayConfig.blockId())
                .billboard(displayConfig.billboard())
                .scale(displayConfig.scaleX(), displayConfig.scaleY(), displayConfig.scaleZ())
                .backgroundColor(displayConfig.backgroundColor())
                .textOpacity(displayConfig.textOpacity())
                .styleFlags(displayConfig.styleFlags())
                .lineWidth(displayConfig.lineWidth())
                .build();
    }

    /**
     * 获取原始文本（含占位符和动画语法）。
     */
    public String getRawText() { return rawText; }

    /**
     * 获取实体类型。
     */
    public DisplayEntityType getEntityType() { return displayConfig.type(); }

    /**
     * 获取显示配置。
     */
    public DisplayConfig getDisplayConfig() { return displayConfig; }

    /**
     * 设置显示配置。
     */
    public void setDisplayConfig(DisplayConfig config) {
        this.displayConfig = config;
        if (config.text() != null) {
            this.rawText = config.text();
        }
    }

    @Override
    public int getEntityId() { return entityId; }

    // ===== 行间距 =====

    public double getOffsetY() { return offsetY; }
    public void setOffsetY(double offsetY) { this.offsetY = offsetY; }
    public double getOffsetX() { return offsetX; }
    public void setOffsetX(double offsetX) { this.offsetX = offsetX; }
    public double getOffsetZ() { return offsetZ; }
    public void setOffsetZ(double offsetZ) { this.offsetZ = offsetZ; }

    // ===== 行高 =====

    public double getLineHeight() { return lineHeight; }
    public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }

    // ===== 行级权限 =====

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    // ===== 行级 Flag =====

    public Set<String> getFlags() { return flags; }
    public void addFlag(String flag) { flags.add(flag.toLowerCase()); }
    public void removeFlag(String flag) { flags.remove(flag.toLowerCase()); }
    public boolean hasFlag(String flag) { return flags.contains(flag.toLowerCase()); }

    // ===== 动作 =====

    public Action getLeftClickAction() { return leftClickAction; }
    public void setLeftClickAction(Action action) { this.leftClickAction = action; }

    public Action getRightClickAction() { return rightClickAction; }
    public void setRightClickAction(Action action) { this.rightClickAction = action; }

    public Action getShiftLeftClickAction() { return shiftLeftClickAction; }
    public void setShiftLeftClickAction(Action action) { this.shiftLeftClickAction = action; }

    public Action getShiftRightClickAction() { return shiftRightClickAction; }
    public void setShiftRightClickAction(Action action) { this.shiftRightClickAction = action; }

    // ===== 动画 =====

    public TextAnimation getAnimation() { return animation; }
    public void setAnimation(TextAnimation animation) { this.animation = animation; }

    /**
     * 推进动画。
     *
     * @return true 如果帧变化了
     */
    public boolean tickAnimation() {
        if (animation == null) return false;
        return animation.tick();
    }

    /**
     * 获取当前动画帧文本。
     */
    public String getAnimationText() {
        if (animation == null) return rawText;
        return animation.getCurrentFrame();
    }

    /**
     * 计算该行在世界中的 Y 坐标。
     *
     * @param baseY        悬浮字基准 Y
     * @param lineSpacing  默认行间距
     */
    public double getWorldY(double baseY, double lineSpacing) {
        double spacing = (lineHeight > 0) ? lineHeight : (offsetY > 0) ? offsetY : lineSpacing;
        return baseY - (index * spacing);
    }

    // ===== 启用/禁用 =====

    private boolean enabled = true;

    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
    public boolean isEnabled() { return enabled; }

    // ===== 类型 =====

    /**
     * 获取行类型。
     */
    public DisplayEntityType getType() { return displayConfig.type(); }

    /**
     * 获取实体ID数组（单行只有一个实体）。
     */
    public int[] getEntityIds() { return new int[]{entityId}; }

    // ===== 权限检查 =====

    /**
     * 检查玩家是否有权限查看此行。
     */
    public boolean hasPermission(java.util.UUID playerId, HologramManager.PermissionChecker checker) {
        if (permission == null || permission.isEmpty()) return true;
        if (checker == null) return true;
        return checker.hasPermission(playerId, permission);
    }

    /**
     * 检查是否可以显示给玩家。
     */
    public boolean canShow(java.util.UUID playerId, HologramManager.PermissionChecker checker) {
        if (!enabled) return false;
        if (hasFlag("disable")) return false;
        if (!hasPermission(playerId, checker)) return false;
        return true;
    }

    // ===== 范围检查 =====

    /**
     * 检查玩家是否在显示范围内。
     */
    public boolean isInDisplayRange(double playerX, double playerY, double playerZ,
                                     double lineX, double lineY, double lineZ,
                                     double displayRange) {
        double y = getWorldY(lineY, 0.3);
        double dx = playerX - lineX;
        double dy = playerY - y;
        double dz = playerZ - lineZ;
        return dx * dx + dy * dy + dz * dz <= displayRange * displayRange;
    }

    /**
     * 检查玩家是否在更新范围内。
     */
    public boolean isInUpdateRange(double playerX, double playerY, double playerZ,
                                    double lineX, double lineY, double lineZ,
                                    double updateRange) {
        double y = getWorldY(lineY, 0.3);
        double dx = playerX - lineX;
        double dy = playerY - y;
        double dz = playerZ - lineZ;
        return dx * dx + dy * dy + dz * dz <= updateRange * updateRange;
    }

    // ===== 显示/隐藏/更新 =====

    /**
     * 显示给玩家。
     */
    public void show(java.util.UUID playerId, double x, double y, double z,
                     DisplayFactoryRegistry registry, PlaceholderManager placeholders,
                     java.util.function.Function<java.util.UUID, Object> playerResolver) {
        if (!enabled) return;
        DisplayConfig config = resolveDisplayConfig(playerId, placeholders);
        DisplayEntityFactory factory = registry.getOrNull(getEntityType());
        Object player = playerResolver.apply(playerId);
        if (factory != null && player != null) {
            factory.spawn(player, entityId, x, getWorldY(y, 0.3), z, config);
        }
    }

    /**
     * 隐藏给玩家。
     */
    public void hide(java.util.UUID playerId, DisplayFactoryRegistry registry,
                     java.util.function.Function<java.util.UUID, Object> playerResolver) {
        DisplayEntityFactory factory = registry.getOrNull(getEntityType());
        Object player = playerResolver.apply(playerId);
        if (factory != null && player != null) {
            factory.despawn(player, entityId);
        }
    }

    /**
     * 更新给玩家。
     */
    public void update(java.util.UUID playerId, double x, double y, double z,
                       DisplayFactoryRegistry registry, PlaceholderManager placeholders,
                       java.util.function.Function<java.util.UUID, Object> playerResolver) {
        if (!enabled) return;
        DisplayConfig config = resolveDisplayConfig(playerId, placeholders);
        DisplayEntityFactory factory = registry.getOrNull(getEntityType());
        Object player = playerResolver.apply(playerId);
        if (factory != null && player != null) {
            factory.updateMetadata(player, entityId, config);
        }
    }

    /**
     * 解析显示配置（占位符 + 渐变）。
     */
    private DisplayConfig resolveDisplayConfig(java.util.UUID playerId, PlaceholderManager placeholders) {
        DisplayConfig base = getDisplayConfig();
        if (base.type() != DisplayEntityType.TEXT_DISPLAY) return base;

        String text = getAnimationText();
        if (placeholders != null) {
            text = placeholders.replace(text, playerId);
        }
        if (GradientParser.hasGradient(text)) {
            text = GradientParser.applyGradient(text);
        }
        if (text.equals(base.text())) return base;

        return DisplayConfig.builder(base.type())
                .text(text)
                .billboard(base.billboard())
                .scale(base.scaleX(), base.scaleY(), base.scaleZ())
                .backgroundColor(base.backgroundColor())
                .textOpacity(base.textOpacity())
                .styleFlags(base.styleFlags())
                .lineWidth(base.lineWidth())
                .build();
    }

    // ===== 序列化 =====

    /**
     * 序列化为 Map。
     */
    public java.util.Map<String, Object> serializeToMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("type", displayConfig.type().toConfig());
        if (displayConfig.type() == DisplayEntityType.TEXT_DISPLAY) {
            map.put("text", rawText);
        } else if (displayConfig.type() == DisplayEntityType.ITEM_DISPLAY) {
            map.put("item", displayConfig.itemId());
        } else if (displayConfig.type() == DisplayEntityType.BLOCK_DISPLAY) {
            map.put("block", displayConfig.blockId());
        } else if (displayConfig.type() == DisplayEntityType.ENTITY) {
            map.put("entity", displayConfig.blockId());
        } else if (displayConfig.type() == DisplayEntityType.HEAD || displayConfig.type() == DisplayEntityType.SMALLHEAD) {
            map.put("head", displayConfig.blockId());
        } else if (displayConfig.type() == DisplayEntityType.ICON) {
            map.put("icon", displayConfig.blockId());
        }

        if (offsetX != 0) map.put("offset-x", offsetX);
        if (offsetY != 0) map.put("offset-y", offsetY);
        if (offsetZ != 0) map.put("offset-z", offsetZ);
        if (lineHeight > 0) map.put("line-height", lineHeight);
        if (permission != null) map.put("permission", permission);
        if (!flags.isEmpty()) map.put("flags", new java.util.ArrayList<>(flags));

        if (leftClickAction != null) map.put("left-click", ActionFactory.serialize(leftClickAction));
        if (rightClickAction != null) map.put("right-click", ActionFactory.serialize(rightClickAction));
        if (shiftLeftClickAction != null) map.put("shift-left-click", ActionFactory.serialize(shiftLeftClickAction));
        if (shiftRightClickAction != null) map.put("shift-right-click", ActionFactory.serialize(shiftRightClickAction));

        return map;
    }
}
