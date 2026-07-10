package org.windy.hologram.config;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多语言消息管理。
 * <p>从 lang.yml 加载消息，支持占位符替换。
 */
public class Lang {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static Map<String, String> messages = new LinkedHashMap<>();

    static {
        // 悬浮字操作
        DEFAULTS.put("holo_created", "§a已创建悬浮字 '%name%' 在你的位置");
        DEFAULTS.put("holo_deleted", "§a已删除悬浮字 '%name%'");
        DEFAULTS.put("holo_not_found", "§c悬浮字 '%name%' 不存在");
        DEFAULTS.put("holo_already_exists", "§c悬浮字 '%name%' 已存在");
        DEFAULTS.put("holo_saved", "§a已保存悬浮字 '%name%' 到配置文件");
        DEFAULTS.put("holo_saved_all", "§a已保存所有悬浮字到配置文件");
        DEFAULTS.put("holo_reloaded", "§a配置已重新加载，共 %count% 个悬浮字");

        // 行操作
        DEFAULTS.put("line_added", "§a已添加%type%行到悬浮字 '%name%'");
        DEFAULTS.put("line_set", "§a已更新悬浮字 '%name%' 第 %index% 行");
        DEFAULTS.put("line_removed", "§a已删除悬浮字 '%name%' 第 %index% 行");
        DEFAULTS.put("line_inserted", "§a已在位置 %index% 插入行");
        DEFAULTS.put("lines_swapped", "§a已交换第 %a% 行和第 %b% 行");

        // 页面操作
        DEFAULTS.put("page_added", "§a已添加第 %count% 页到悬浮字 '%name%'");
        DEFAULTS.put("page_removed", "§a已删除悬浮字 '%name%' 第 %index% 页");
        DEFAULTS.put("page_switched", "§a编辑页已切换到第 %index% 页");
        DEFAULTS.put("page_cannot_remove", "§c无法删除（至少保留 1 页，或页码无效）");

        // Flag
        DEFAULTS.put("flag_added", "§a已添加 flag '%flag%' 到悬浮字 '%name%'");
        DEFAULTS.put("flag_removed", "§a已移除 flag '%flag%' 从悬浮字 '%name%'");

        // 权限
        DEFAULTS.put("permission_set", "§a已设置悬浮字 '%name%' 的权限为: %perm%");
        DEFAULTS.put("permission_cleared", "§a已清除悬浮字 '%name%' 的权限限制");
        DEFAULTS.put("no_permission", "§c你没有权限使用此命令 (%perm%)");
        DEFAULTS.put("player_only", "§c只有玩家可以使用此命令");

        // 传送
        DEFAULTS.put("tp_success", "§a已传送到悬浮字 '%name%' (%server%)");
        DEFAULTS.put("tp_rcon_unavailable", "§cRCON 未配置，无法自动传送");

        // 通用
        DEFAULTS.put("usage", "§c用法: %usage%");
        DEFAULTS.put("number_required", "§c%field% 必须是数字");
        DEFAULTS.put("invalid_index", "§c%field% 无效");
    }

    private Lang() {}

    /**
     * 加载语言文件。
     */
    public static void load(Path dataDir) {
        // 先加载默认值
        messages = new LinkedHashMap<>(DEFAULTS);

        File file = dataDir.resolve("lang.yml").toFile();
        if (!file.exists()) {
            createDefault(file);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loaded = yaml.load(fis);
            if (loaded != null) {
                for (Map.Entry<String, Object> entry : loaded.entrySet()) {
                    messages.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        } catch (Exception e) {
            System.err.println("[VelocityHologram] 加载 lang.yml 失败: " + e.getMessage());
        }
    }

    /**
     * 获取消息。
     *
     * @param key 消息键
     * @return 消息文本
     */
    public static String get(String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * 获取消息并替换占位符。
     *
     * @param key    消息键
     * @param params 占位符参数（键值对，如 "%name%", "test"）
     * @return 替换后的消息
     */
    public static String get(String key, String... params) {
        String msg = get(key);
        for (int i = 0; i + 1 < params.length; i += 2) {
            msg = msg.replace(params[i], params[i + 1]);
        }
        return msg;
    }

    private static void createDefault(File file) {
        try {
            List<String> lines = new java.util.ArrayList<>();
            lines.add("# VelocityHologram 语言文件");
            lines.add("# 修改此文件可自定义所有提示消息");
            lines.add("");
            for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
                lines.add(entry.getKey() + ": \"" + entry.getValue() + "\"");
            }
            Files.write(file.toPath(), lines);
        } catch (IOException ignored) {
        }
    }
}
