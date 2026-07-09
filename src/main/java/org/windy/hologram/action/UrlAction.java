package org.windy.hologram.action;

import java.util.UUID;

/**
 * 打开 URL。
 */
public class UrlAction implements Action {

    private final String url;

    public UrlAction(String url) {
        this.url = url;
    }

    @Override
    public void execute(UUID playerId) {
        ActionContext.openUrl(playerId, url);
    }

    @Override
    public ActionType getType() { return ActionType.URL; }

    @Override
    public String serialize() { return "url:" + url; }

    public static UrlAction deserialize(String data) {
        return new UrlAction(data);
    }
}
