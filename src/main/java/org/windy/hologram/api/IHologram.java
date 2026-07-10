package org.windy.hologram.api;

import java.util.List;
import java.util.UUID;

/**
 * 悬浮字公共 API 接口。
 */
public interface IHologram {

    UUID getId();
    String getName();
    HologramPos getPosition();
    List<IHologramLine> getLines();

    void addLine(String text);
    void setLine(int index, String text);
    void removeLine(int index);

    void showTo(UUID playerId);
    void hideFrom(UUID playerId);
    void update();
    void destroy();

    /**
     * 悬浮字位置。
     */
    record HologramPos(double x, double y, double z, String dimension, String server) {}
}
