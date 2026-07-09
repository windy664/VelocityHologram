package org.windy.hologram.api;

/**
 * 悬浮字单行文本 API 接口。
 */
public interface IHologramLine {

    int getIndex();
    String getText();
    void setText(String text);
    int getEntityId();
}
