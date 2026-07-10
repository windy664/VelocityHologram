package org.windy.hologram.action;

import org.windy.hologram.hologram.Hologram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 动作链。
 * <p>多个动作按顺序执行，遇到 PERMISSION 门控失败则中断。
 *
 * <p>语法：用 {@code ;} 分隔多个动作：
 * <pre>
 * perm:holo.use;connect:lobby
 * sound:entity.experience_orb.pickup;nextpage
 * </pre>
 */
public class ActionChain implements Action {

    private final List<Action> actions;

    public ActionChain(List<Action> actions) {
        this.actions = actions;
    }

    @Override
    public void execute(UUID playerId) {
        for (Action action : actions) {
            if (action instanceof PermissionAction) {
                PermissionAction perm = (PermissionAction) action;
                perm.execute(playerId);
                if (!perm.isAllowed()) {
                    // 权限不足，中断后续动作
                    return;
                }
            } else {
                action.execute(playerId);
            }
        }
    }

    @Override
    public ActionType getType() {
        return actions.isEmpty() ? ActionType.COMMAND : actions.get(0).getType();
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(actions.get(i).serialize());
        }
        return sb.toString();
    }

    public List<Action> getActions() { return actions; }

    /**
     * 解析动作链字符串。
     * <p>支持 {@code ;} 分隔的多个动作。
     *
     * @param input    动作字符串
     * @param hologram 所属悬浮字（page 动作需要）
     * @return 动作链，空输入返回 null
     */
    public static ActionChain parse(String input, Hologram hologram) {
        if (input == null || input.isEmpty()) return null;

        String[] parts = input.split(";");
        List<Action> actions = new ArrayList<>();

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            Action action = parseSingle(part, hologram);
            if (action != null) actions.add(action);
        }

        return actions.isEmpty() ? null : new ActionChain(actions);
    }

    /**
     * 解析单个动作。
     */
    static Action parseSingle(String input, Hologram hologram) {
        if (input == null || input.isEmpty()) return null;

        // 处理无参数的动作
        String lower = input.toLowerCase().trim();
        if (lower.equals("nextpage") && hologram != null) return new NextPageAction(hologram);
        if (lower.equals("prevpage") && hologram != null) return new PrevPageAction(hologram);

        String[] parts = input.split(":", 2);
        if (parts.length < 2) return null;

        String type = parts[0].toLowerCase().trim();
        String data = parts[1];

        switch (type) {
            case "command":
            case "cmd":
                return new CommandAction(data);
            case "console":
            case "server":
                return new ConsoleCommandAction(data);
            case "rcon":
                return RconAction.deserialize(data);
            case "url":
            case "link":
                return new UrlAction(data);
            case "message":
            case "msg":
                return new MessageAction(data);
            case "suggest":
                return new SuggestCommandAction(data);
            case "connect":
                return new ConnectAction(data);
            case "sound":
                return parseSound(data);
            case "teleport":
                return parseTeleport(data);
            case "perm":
            case "permission":
                return new PermissionAction(data);
            case "page":
                if (hologram != null) {
                    try { return new PageAction(hologram, Integer.parseInt(data.trim())); }
                    catch (NumberFormatException e) { return null; }
                }
                return null;
            default:
                return null;
        }
    }

    private static SoundAction parseSound(String data) {
        String[] p = data.split(":");
        String name = p[0];
        float vol = p.length > 1 ? parseFloat(p[1], 1.0f) : 1.0f;
        float pitch = p.length > 2 ? parseFloat(p[2], 1.0f) : 1.0f;
        return new SoundAction(name, vol, pitch);
    }

    private static TeleportAction parseTeleport(String data) {
        String[] p = data.split(":");
        String world = p.length > 0 ? p[0] : null;
        double x = p.length > 1 ? parseDouble(p[1], 0) : 0;
        double y = p.length > 2 ? parseDouble(p[2], 0) : 0;
        double z = p.length > 3 ? parseDouble(p[3], 0) : 0;
        float yaw = p.length > 4 ? parseFloat(p[4], 0) : 0;
        float pitch = p.length > 5 ? parseFloat(p[5], 0) : 0;
        return new TeleportAction(world, x, y, z, yaw, pitch);
    }

    private static float parseFloat(String s, float def) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return def; }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
