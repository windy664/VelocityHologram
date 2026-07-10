package org.windy.hologram.utils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物品工具类。
 * <p>解析物品ID、玩家头颅纹理等。
 */
public final class ItemUtils {

    private static final Pattern PLAYER_HEAD_PATTERN = Pattern.compile("^(?:player_head|skull)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXTURE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{20,}$");

    // 常用物品别名映射
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        // 武器
        ALIASES.put("sword", "minecraft:iron_sword");
        ALIASES.put("diamond_sword", "minecraft:diamond_sword");
        ALIASES.put("netherite_sword", "minecraft:netherite_sword");

        // 工具
        ALIASES.put("pickaxe", "minecraft:iron_pickaxe");
        ALIASES.put("diamond_pickaxe", "minecraft:diamond_pickaxe");

        // 方块
        ALIASES.put("grass", "minecraft:grass_block");
        ALIASES.put("stone", "minecraft:stone");
        ALIASES.put("diamond_block", "minecraft:diamond_block");
        ALIASES.put("gold_block", "minecraft:gold_block");
        ALIASES.put("emerald_block", "minecraft:emerald_block");
        ALIASES.put("netherite_block", "minecraft:netherite_block");

        // 物品
        ALIASES.put("diamond", "minecraft:diamond");
        ALIASES.put("emerald", "minecraft:emerald");
        ALIASES.put("gold", "minecraft:gold_ingot");
        ALIASES.put("iron", "minecraft:iron_ingot");
        ALIASES.put("netherite", "minecraft:netherite_ingot");

        // 特殊物品
        ALIASES.put("totem", "minecraft:totem_of_undying");
        ALIASES.put("elytra", "minecraft:elytra");
        ALIASES.put("pearl", "minecraft:ender_pearl");
        ALIASES.put("blaze_rod", "minecraft:blaze_rod");
        ALIASES.put("experience_bottle", "minecraft:experience_bottle");
    }

    private ItemUtils() {}

    /**
     * 解析物品ID。
     * <p>支持别名和完整命名空间格式。
     *
     * @param input 输入的物品ID或别名
     * @return 标准化的物品ID
     */
    public static String parseItemId(String input) {
        if (input == null || input.isEmpty()) {
            return "minecraft:stone";
        }

        String lower = input.toLowerCase().trim();

        // 检查别名
        if (ALIASES.containsKey(lower)) {
            return ALIASES.get(lower);
        }

        // 已经是完整格式
        if (lower.contains(":")) {
            return lower;
        }

        // 默认添加 minecraft: 前缀
        return "minecraft:" + lower;
    }

    /**
     * 检查是否是玩家头颅。
     */
    public static boolean isPlayerHead(String itemId) {
        if (itemId == null) return false;
        return PLAYER_HEAD_PATTERN.matcher(itemId).find();
    }

    /**
     * 解析玩家头颅纹理。
     * <p>支持以下格式：
     * <ul>
     *   <li>玩家名 - 从 Mojang API 获取纹理</li>
     *   <li>纹理值 - 直接使用</li>
     *   <li>完整 URL - 提取纹理值</li>
     * </ul>
     *
     * @param input 玩家名或纹理值
     * @return 纹理值，如果无法解析则返回 null
     */
    public static String parseHeadTexture(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // 如果是纹理值（Base64 编码的 JSON）
        if (TEXTURE_PATTERN.matcher(input).matches()) {
            return input;
        }

        // 如果是完整 URL
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return encodeTextureUrl(input);
        }

        // 如果是玩家名，需要异步查询 Mojang API
        // 这里返回玩家名，由调用者处理异步查询
        return input;
    }

    /**
     * 将纹理 URL 编码为 Base64 纹理值。
     */
    public static String encodeTextureUrl(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    /**
     * 从 Base64 纹理值解码 URL。
     */
    public static String decodeTextureUrl(String textureValue) {
        if (textureValue == null) return null;
        try {
            String json = new String(Base64.getDecoder().decode(textureValue));
            // 简单解析 JSON 获取 URL
            int urlStart = json.indexOf("\"url\":\"") + 7;
            int urlEnd = json.indexOf("\"", urlStart);
            if (urlStart > 6 && urlEnd > urlStart) {
                return json.substring(urlStart, urlEnd);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 格式化物品显示名。
     * <p>例如: "minecraft:diamond_sword" → "Diamond Sword"
     */
    public static String formatItemName(String itemId) {
        if (itemId == null || itemId.isEmpty()) return "";

        // 移除命名空间
        String name = itemId.contains(":") ? itemId.split(":")[1] : itemId;

        // 下划线转空格，首字母大写
        String[] words = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }

        return sb.toString();
    }

    /**
     * 检查物品ID是否有效。
     */
    public static boolean isValidItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        // 简单检查格式
        return itemId.matches("^[a-z0-9_]+:[a-z0-9_/]+$") || ALIASES.containsKey(itemId.toLowerCase());
    }
}
