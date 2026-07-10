package org.windy.hologram.action;

import java.util.UUID;

/**
 * 点击传送到坐标。
 * <p>通过 RCON 执行 /tp 命令。
 */
public class TeleportAction implements Action {

    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;

    public TeleportAction(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
    }

    public TeleportAction(String world, double x, double y, double z) {
        this(world, x, y, z, 0, 0);
    }

    @Override
    public void execute(UUID playerId) {
        if (ActionContext.getProxy() == null) return;
        var player = ActionContext.getProxy().getPlayer(playerId).orElse(null);
        if (player == null) return;

        String playerName = player.getUsername();
        String dim = world != null ? world : "minecraft:overworld";
        String cmd = "execute in " + dim + " run tp " + playerName
                + " " + x + " " + y + " " + z + " " + yaw + " " + pitch;

        // 优先 RCON
        if (ActionContext.rconAvailable()) {
            ActionContext.executeRcon(cmd, null);
        } else {
            ActionContext.executeConsole(cmd);
        }
    }

    @Override
    public ActionType getType() { return ActionType.TELEPORT; }

    @Override
    public String serialize() {
        return "teleport:" + (world != null ? world : "") + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch;
    }
}
