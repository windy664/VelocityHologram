package org.windy.hologram.action;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;
import java.util.UUID;

/**
 * 点击切换子服。
 */
public class ConnectAction implements Action {

    private final String server;

    public ConnectAction(String server) {
        this.server = server;
    }

    @Override
    public void execute(UUID playerId) {
        if (ActionContext.getProxy() == null) return;
        Player player = ActionContext.getProxy().getPlayer(playerId).orElse(null);
        if (player == null) return;
        Optional<RegisteredServer> target = ActionContext.getProxy().getServer(server);
        target.ifPresent(s -> player.createConnectionRequest(s).connectWithIndication());
    }

    @Override
    public ActionType getType() { return ActionType.CONNECT; }

    @Override
    public String serialize() { return "connect:" + server; }
}
