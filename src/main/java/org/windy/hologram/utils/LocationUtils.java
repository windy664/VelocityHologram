package org.windy.hologram.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 位置工具类。
 * <p>位置序列化、反序列化、格式化等。
 */
public final class LocationUtils {

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "([^,]+),([^,]+),([^,]+)(?:,([^,]+))?(?:,([^,]+))?"
    );

    private LocationUtils() {}

    /**
     * 格式化坐标。
     * <p>例如: (100.5, 64.0, -200.3) → "100.5, 64.0, -200.3"
     */
    public static String format(double x, double y, double z) {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }

    /**
     * 格式化坐标（带世界）。
     */
    public static String format(double x, double y, double z, String world) {
        return String.format("%.1f, %.1f, %.1f (%s)", x, y, z, world);
    }

    /**
     * 解析坐标字符串。
     * <p>支持格式: "x,y,z" 或 "x,y,z,world" 或 "x,y,z,world,server"
     *
     * @return double[] {x, y, z}，解析失败返回 null
     */
    public static double[] parseCoordinates(String input) {
        if (input == null || input.isEmpty()) return null;

        Matcher matcher = LOCATION_PATTERN.matcher(input.trim());
        if (!matcher.matches()) return null;

        try {
            double x = Double.parseDouble(matcher.group(1).trim());
            double y = Double.parseDouble(matcher.group(2).trim());
            double z = Double.parseDouble(matcher.group(3).trim());
            return new double[]{x, y, z};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析位置字符串（包含世界）。
     *
     * @return String[] {x, y, z, world}，解析失败返回 null
     */
    public static String[] parseLocation(String input) {
        if (input == null || input.isEmpty()) return null;

        Matcher matcher = LOCATION_PATTERN.matcher(input.trim());
        if (!matcher.matches()) return null;

        try {
            String x = matcher.group(1).trim();
            String y = matcher.group(2).trim();
            String z = matcher.group(3).trim();
            String world = matcher.group(4) != null ? matcher.group(4).trim() : "world";
            String server = matcher.group(5) != null ? matcher.group(5).trim() : "";
            return new String[]{x, y, z, world, server};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算两点之间的距离。
     */
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 计算两点之间的距离平方（避免开方，用于比较）。
     */
    public static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 居中坐标到方块中心。
     */
    public static double center(double value) {
        return Math.floor(value) + 0.5;
    }

    /**
     * 居中坐标到区块中心。
     */
    public static double chunkCenter(double value) {
        return Math.floor(value / 16) * 16 + 8.5;
    }

    /**
     * 格式化朝向。
     */
    public static String formatFacing(float yaw, float pitch) {
        return String.format("yaw=%.1f, pitch=%.1f", yaw, pitch);
    }

    /**
     * 获取朝向名称。
     */
    public static String getFacingDirection(float yaw) {
        // 标准化角度到 0-360
        float normalized = ((yaw % 360) + 360) % 360;

        if (normalized >= 315 || normalized < 45) return "南";
        if (normalized >= 45 && normalized < 135) return "西";
        if (normalized >= 135 && normalized < 225) return "北";
        return "东";
    }

    /**
     * 检查坐标是否在范围内。
     */
    public static boolean isInRange(double x1, double y1, double z1, double x2, double y2, double z2, double range) {
        return distanceSquared(x1, y1, z1, x2, y2, z2) <= range * range;
    }

    /**
     * 格式化区块坐标。
     */
    public static String formatChunk(int chunkX, int chunkZ) {
        return String.format("[%d, %d]", chunkX, chunkZ);
    }

    /**
     * 获取区块坐标。
     */
    public static int toChunkCoord(double blockCoord) {
        return (int) Math.floor(blockCoord / 16);
    }
}
