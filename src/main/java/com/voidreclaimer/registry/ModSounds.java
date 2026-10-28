package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Our own sound events. Until real audio exists, {@code sounds.json} (datagen) points them at quiet vanilla sounds,
 * so swapping in custom audio later needs no code changes.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, VoidReclaimer.MOD_ID);

    public static final Supplier<SoundEvent> THRUSTER = register("thruster");
    public static final Supplier<SoundEvent> DAMPENER = register("dampener");
    public static final Supplier<SoundEvent> SENSOR_PULSE = register("sensor_pulse");
    public static final Supplier<SoundEvent> SENSOR_ECHO = register("sensor_echo");
    public static final Supplier<SoundEvent> SENSOR_HUM = register("sensor_hum");

    private ModSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(VoidReclaimer.id(name)));
    }
}
