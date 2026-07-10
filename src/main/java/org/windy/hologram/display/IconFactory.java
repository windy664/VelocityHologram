package org.windy.hologram.display;

/**
 * ICON 行类型工厂。
 * <p>使用 Item Display 实体显示物品图标。
 * <p>语法：#ICON:物品ID 或 #ICON:玩家名
 */
public class IconFactory implements DisplayEntityFactory {

    private final ItemDisplayFactory itemFactory = new ItemDisplayFactory();

    @Override
    public void spawn(Object player, int entityId, double x, double y, double z, DisplayConfig config) {
        // ICON 使用 Item Display，但需要特殊处理物品
        DisplayConfig iconConfig = resolveIconConfig(config);
        itemFactory.spawn(player, entityId, x, y, z, iconConfig);
    }

    @Override
    public void despawn(Object player, int entityId) {
        itemFactory.despawn(player, entityId);
    }

    @Override
    public void updateMetadata(Object player, int entityId, DisplayConfig config) {
        DisplayConfig iconConfig = resolveIconConfig(config);
        itemFactory.updateMetadata(player, entityId, iconConfig);
    }

    /**
     * 解析 ICON 配置。
     * <p>将 #ICON: 前缀转换为 Item Display 配置。
     */
    private DisplayConfig resolveIconConfig(DisplayConfig config) {
        String content = config.blockId();
        if (content == null || content.isEmpty()) {
            content = "minecraft:stone";
        }

        // 移除 #ICON: 前缀（如果有）
        if (content.toUpperCase().startsWith("#ICON:")) {
            content = content.substring(6);
        }

        // 构建 Item Display 配置
        return DisplayConfig.builder(DisplayEntityType.ITEM_DISPLAY)
                .itemId(content)
                .billboard(config.billboard())
                .scale(config.scaleX(), config.scaleY(), config.scaleZ())
                .build();
    }
}
