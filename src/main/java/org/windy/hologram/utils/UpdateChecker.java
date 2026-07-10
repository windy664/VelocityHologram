package org.windy.hologram.utils;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/**
 * 版本更新检查器。
 * <p>从 GitHub Releases 检查最新版本。
 */
public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/windy664/VelocityHologram/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/windy664/VelocityHologram/releases";

    private final String currentVersion;
    private final Logger logger;

    public UpdateChecker(String currentVersion, Logger logger) {
        this.currentVersion = currentVersion;
        this.logger = logger;
    }

    /**
     * 异步检查更新。
     *
     * @param callback 回调，参数为最新版本号（如果没有更新则为 null）
     */
    public void checkAsync(Consumer<String> callback) {
        new Thread(() -> {
            try {
                String latestVersion = checkForUpdate();
                if (latestVersion != null && isNewer(latestVersion, currentVersion)) {
                    if (callback != null) {
                        callback.accept(latestVersion);
                    }
                } else {
                    if (callback != null) {
                        callback.accept(null);
                    }
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.warn("[VelocityHologram] 检查更新失败: " + e.getMessage());
                }
                if (callback != null) {
                    callback.accept(null);
                }
            }
        }).start();
    }

    /**
     * 同步检查更新。
     *
     * @return 最新版本号，如果没有更新或检查失败则返回 null
     */
    public String checkForUpdate() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 简单解析 JSON 获取 tag_name
            String json = response.toString();
            String tagKey = "\"tag_name\":\"";
            int tagStart = json.indexOf(tagKey);
            if (tagStart == -1) return null;
            tagStart += tagKey.length();
            int tagEnd = json.indexOf("\"", tagStart);
            if (tagEnd == -1) return null;

            String tag = json.substring(tagStart, tagEnd);
            // 移除 v 前缀
            if (tag.startsWith("v")) {
                tag = tag.substring(1);
            }

            return tag;
        } catch (Exception e) {
            if (logger != null) {
                logger.warn("[VelocityHologram] 检查更新失败: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * 比较版本号。
     *
     * @return true 如果 v1 比 v2 新
     */
    public static boolean isNewer(String v1, String v2) {
        if (v1 == null || v2 == null) return false;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int p2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (p1 > p2) return true;
            if (p1 < p2) return false;
        }

        return false;
    }

    /**
     * 获取更新下载链接。
     */
    public static String getDownloadUrl() {
        return GITHUB_RELEASES_URL;
    }

    /**
     * 获取当前版本。
     */
    public String getCurrentVersion() {
        return currentVersion;
    }

    private static int parseVersionPart(String part) {
        try {
            // 移除非数字字符（如 -SNAPSHOT）
            String numeric = part.replaceAll("[^0-9]", "");
            return numeric.isEmpty() ? 0 : Integer.parseInt(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
