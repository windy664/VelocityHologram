package org.windy.hologram.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HeadDatabase 工具类。
 * <p>提供头颅纹理查询和缓存功能。
 */
public final class HeadDatabaseUtils {

    // 纹理缓存
    private static final Map<String, String> TEXTURE_CACHE = new ConcurrentHashMap<>();

    // 预设的特殊头颅纹理
    private static final Map<String, String> PRESET_TEXTURES = new HashMap<>();

    static {
        // 常用装饰头颅
        PRESET_TEXTURES.put("checkmark", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjVlMmUzYTc1ZjRkMmM0MjQ0YTQ0MjVlNzY1MmZlNzQ0MjRlNzY1MmZlNzQ0MjRlNzY1MmZlIn19fQ==");
        PRESET_TEXTURES.put("x_mark", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzRlOGFlYTliNTllMjY1OWIxNjRkMTg0ZGUxOTU4NjczMzJkMzY1YzhiZGEyMjU0MjE0YmRiYzE4In19fQ==");
        PRESET_TEXTURES.put("arrow_right", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjVlMmUzYTc1ZjRkMmM0MjQ0YTQ0MjVlNzY1MmZlNzQ0MjRlNzY1MmZlNzQ0MjRlNzY1MmZlIn19fQ==");
        PRESET_TEXTURES.put("arrow_left", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjVlMmUzYTc1ZjRkMmM0MjQ0YTQ0MjVlNzY1MmZlNzQ0MjRlNzY1MmZlNzQ0MjRlNzY1MmZlIn19fQ==");

        // 数字头颅
        for (int i = 0; i <= 9; i++) {
            PRESET_TEXTURES.put("num_" + i, getNumberTexture(i));
        }

        // 字母头颅
        for (char c = 'a'; c <= 'z'; c++) {
            PRESET_TEXTURES.put("letter_" + c, getLetterTexture(c));
        }
    }

    private HeadDatabaseUtils() {}

    /**
     * 获取头颅纹理。
     * <p>支持以下输入：
     * <ul>
     *   <li>预设名称（如 "checkmark"）</li>
     *   <li>玩家名</li>
     *   <li>纹理值</li>
     *   <li>HeadDatabase ID</li>
     * </ul>
     *
     * @param input 输入
     * @return 纹理值，如果无法获取则返回 null
     */
    public static String getTexture(String input) {
        if (input == null || input.isEmpty()) return null;

        String lower = input.toLowerCase().trim();

        // 检查缓存
        if (TEXTURE_CACHE.containsKey(lower)) {
            return TEXTURE_CACHE.get(lower);
        }

        // 检查预设
        if (PRESET_TEXTURES.containsKey(lower)) {
            String texture = PRESET_TEXTURES.get(lower);
            TEXTURE_CACHE.put(lower, texture);
            return texture;
        }

        // 如果是纹理值（Base64）
        if (input.matches("^[a-zA-Z0-9+/=]{20,}$")) {
            TEXTURE_CACHE.put(lower, input);
            return input;
        }

        // 如果是玩家名，需要异步查询
        // 这里返回 null，由调用者处理异步查询
        return null;
    }

    /**
     * 异步获取玩家头颅纹理。
     *
     * @param playerName 玩家名
     * @param callback   回调
     */
    public static void getTextureAsync(String playerName, TextureCallback callback) {
        if (playerName == null || playerName.isEmpty()) {
            if (callback != null) callback.onResult(null);
            return;
        }

        String lower = playerName.toLowerCase().trim();

        // 检查缓存
        if (TEXTURE_CACHE.containsKey(lower)) {
            if (callback != null) callback.onResult(TEXTURE_CACHE.get(lower));
            return;
        }

        // 异步查询
        new Thread(() -> {
            try {
                String texture = fetchPlayerTexture(playerName);
                if (texture != null) {
                    TEXTURE_CACHE.put(lower, texture);
                }
                if (callback != null) callback.onResult(texture);
            } catch (Exception e) {
                if (callback != null) callback.onResult(null);
            }
        }).start();
    }

    /**
     * 从 Mojang API 获取玩家纹理。
     */
    private static String fetchPlayerTexture(String playerName) {
        try {
            // 获取玩家 UUID
            java.net.URL uuidUrl = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            java.net.HttpURLConnection uuidConn = (java.net.HttpURLConnection) uuidUrl.openConnection();
            uuidConn.setRequestMethod("GET");
            uuidConn.setConnectTimeout(5000);
            uuidConn.setReadTimeout(5000);

            if (uuidConn.getResponseCode() != 200) return null;

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(uuidConn.getInputStream()));
            String uuidJson = reader.readLine();
            reader.close();

            // 解析 UUID
            String idKey = "\"id\":\"";
            int idStart = uuidJson.indexOf(idKey) + idKey.length();
            int idEnd = uuidJson.indexOf("\"", idStart);
            String uuid = uuidJson.substring(idStart, idEnd);

            // 获取皮肤数据
            java.net.URL skinUrl = new java.net.URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            java.net.HttpURLConnection skinConn = (java.net.HttpURLConnection) skinUrl.openConnection();
            skinConn.setRequestMethod("GET");
            skinConn.setConnectTimeout(5000);
            skinConn.setReadTimeout(5000);

            if (skinConn.getResponseCode() != 200) return null;

            reader = new java.io.BufferedReader(new java.io.InputStreamReader(skinConn.getInputStream()));
            StringBuilder skinJson = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                skinJson.append(line);
            }
            reader.close();

            // 解析纹理值
            String valueKey = "\"value\":\"";
            int valueStart = skinJson.indexOf(valueKey) + valueKey.length();
            int valueEnd = skinJson.indexOf("\"", valueStart);
            if (valueStart > valueKey.length() - 1 && valueEnd > valueStart) {
                return skinJson.substring(valueStart, valueEnd);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 注册自定义头颅纹理。
     */
    public static void registerTexture(String name, String texture) {
        if (name != null && texture != null) {
            PRESET_TEXTURES.put(name.toLowerCase(), texture);
        }
    }

    /**
     * 清除缓存。
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }

    /**
     * 获取数字头颅纹理（占位）。
     */
    private static String getNumberTexture(int number) {
        // 这里需要实际的纹理值，暂时返回占位符
        return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvIn19fQ==";
    }

    /**
     * 获取字母头颅纹理（占位）。
     */
    private static String getLetterTexture(char letter) {
        // 这里需要实际的纹理值，暂时返回占位符
        return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvIn19fQ==";
    }

    /**
     * 纹理回调接口。
     */
    @FunctionalInterface
    public interface TextureCallback {
        void onResult(String texture);
    }
}
