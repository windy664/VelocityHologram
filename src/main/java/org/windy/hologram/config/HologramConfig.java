package org.windy.hologram.config;

import java.util.List;

/**
 * 悬浮字配置。
 */
public class HologramConfig {

    private String name;
    private double x, y, z;
    private String dimension;
    private String server;
    private List<String> lines;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public List<String> getLines() { return lines; }
    public void setLines(List<String> lines) { this.lines = lines; }
}
