package com.voidreclaimer.client.sound;

import com.voidreclaimer.ClientConfig;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** A looping sound that fades in while its condition holds and fades out (then stops) when it doesn't. */
public final class ThrusterSoundInstance extends AbstractTickableSoundInstance {
    private static final float FADE_PER_TICK = 0.08f;

    private final LocalPlayer player;
    private final BooleanSupplier active;
    private final float maxVolume;

    public ThrusterSoundInstance(LocalPlayer player, SoundEvent sound, BooleanSupplier active, float maxVolume, float pitch) {
        super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.active = active;
        this.maxVolume = maxVolume;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.pitch = pitch;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (player.isRemoved()) {
            stop();
            return;
        }
        float target = active.getAsBoolean() ? maxVolume * ClientConfig.THRUSTER_VOLUME.get().floatValue() : 0f;
        if (volume < target) {
            volume = Math.min(target, volume + FADE_PER_TICK);
        } else {
            volume = Math.max(target, volume - FADE_PER_TICK);
        }
        if (volume <= 0f) {
            stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
