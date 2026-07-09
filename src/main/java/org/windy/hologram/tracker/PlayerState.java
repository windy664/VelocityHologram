package org.windy.hologram.tracker;

/**
 * 玩家状态（坐标 + 维度 + 当前子服）。
 * <p>由 packetevents 包拦截器实时更新。
 */
public class PlayerState {

    private volatile String name = "";
    private volatile double x, y, z;
    private volatile String dimension = "minecraft:overworld";
    private volatile String server = "";

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getDimension() { return dimension; }
    public String getServer() { return server; }
}
