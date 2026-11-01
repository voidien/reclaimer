package com.voidreclaimer.display;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.holo.HoloReadout;
import java.util.function.Supplier;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The built-in readout kinds: each one's sampler (reads the world on the server) with its {@link Readouts} formatter. */
public final class ReadoutKinds {
    public static final DeferredRegister<HoloReadout<?>> KINDS = DeferredRegister.create(VoidReclaimerRegistries.HOLO_READOUT, VoidReclaimer.MOD_ID);

    public static final Supplier<HoloReadout<Readouts.Clock>> CLOCK = KINDS.register("clock",
            () -> new HoloReadout<>((level, pos) -> new Readouts.Clock(level.getGameTime()), Readouts::clock));

    private ReadoutKinds() {}
}
