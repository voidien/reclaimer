package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModSounds;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

/** Placeholder audio: each of our sound events plays a vanilla sound event until custom audio exists. */
public final class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, VoidReclaimer.MOD_ID, helper);
    }

    @Override
    public void registerSounds() {
        add(ModSounds.THRUSTER, definition()
                .subtitle("subtitles.void_reclaimer.thruster")
                .with(vanilla("item.elytra.flying").pitch(1.6).volume(0.5)));
        add(ModSounds.DAMPENER, definition()
                .subtitle("subtitles.void_reclaimer.dampener")
                .with(vanilla("block.beacon.ambient").pitch(0.6)));
        add(ModSounds.SENSOR_PULSE, definition()
                .subtitle("subtitles.void_reclaimer.sensor_pulse")
                .with(vanilla("block.beacon.power_select").pitch(1.4).volume(0.6)));
        add(ModSounds.SENSOR_ECHO, definition()
                .subtitle("subtitles.void_reclaimer.sensor_echo")
                .with(vanilla("block.amethyst_block.chime").volume(1.5)));
        add(ModSounds.SENSOR_HUM, definition()
                .subtitle("subtitles.void_reclaimer.sensor_hum")
                .with(vanilla("block.note_block.bit")));
        add(ModSounds.NANITE_BUILD, definition()
                .subtitle("subtitles.void_reclaimer.nanite_build")
                .with(vanilla("block.amethyst_cluster.place").pitch(1.5).volume(0.7)));
        add(ModSounds.ACID_DRIZZLE, definition()
                .subtitle("subtitles.void_reclaimer.acid_drizzle")
                .with(vanilla("weather.rain").pitch(1.3).volume(0.5)));
        add(ModSounds.MASS_DRIVER_LAUNCH, definition()
                .subtitle("subtitles.void_reclaimer.mass_driver_launch")
                .with(vanilla("entity.generic.explode").pitch(0.7).volume(0.6)));
    }

    private static SoundDefinition.Sound vanilla(String event) {
        return sound("minecraft:" + event, SoundDefinition.SoundType.EVENT);
    }
}
