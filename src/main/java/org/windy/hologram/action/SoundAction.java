package org.windy.hologram.action;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.sound.Sound;

import java.util.UUID;

/**
 * 点击播放音效。
 */
public class SoundAction implements Action {

    private final String soundName;
    private final float volume;
    private final float pitch;

    public SoundAction(String soundName, float volume, float pitch) {
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void execute(UUID playerId) {
        if (ActionContext.getProxy() == null) return;
        Player player = ActionContext.getProxy().getPlayer(playerId).orElse(null);
        if (player == null) return;
        Sound sound = Sound.sound()
                .type(net.kyori.adventure.key.Key.key(soundName.toLowerCase()))
                .source(Sound.Source.MASTER)
                .volume(volume)
                .pitch(pitch)
                .build();
        player.playSound(sound);
    }

    @Override
    public ActionType getType() { return ActionType.SOUND; }

    @Override
    public String serialize() { return "sound:" + soundName + ":" + volume + ":" + pitch; }
}
