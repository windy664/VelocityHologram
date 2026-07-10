package org.windy.hologram.action;

import com.velocitypowered.api.proxy.Player;

import java.util.UUID;

/**
 * 权限门控动作。
 * <p>检查玩家是否有指定权限，无权限则中断后续动作执行。
 * <p>这个动作本身不执行任何操作，只作为门控条件。
 * <p>在 ActionChain 中，如果 PermissionAction.execute() 返回 false，后续动作不执行。
 */
public class PermissionAction implements Action {

    private final String permission;
    private volatile boolean lastResult = true;

    public PermissionAction(String permission) {
        this.permission = permission;
    }

    @Override
    public void execute(UUID playerId) {
        if (ActionContext.getProxy() == null) {
            lastResult = true;
            return;
        }
        Player player = ActionContext.getProxy().getPlayer(playerId).orElse(null);
        lastResult = player == null || player.hasPermission(permission);
    }

    /**
     * 获取上次检查结果。
     */
    public boolean isAllowed() { return lastResult; }

    public String getPermission() { return permission; }

    @Override
    public ActionType getType() { return ActionType.PERMISSION; }

    @Override
    public String serialize() { return "perm:" + permission; }
}
