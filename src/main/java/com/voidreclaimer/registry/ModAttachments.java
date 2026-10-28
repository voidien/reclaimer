package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.flight.FlightSettings;
import com.voidreclaimer.flight.FlightTracker;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, VoidReclaimer.MOD_ID);

    /** Per-player energy. Persisted, kept on death, and synced only to the owning player. */
    public static final Supplier<AttachmentType<EnergyData>> ENERGY = ATTACHMENT_TYPES.register("energy",
            () -> AttachmentType.builder(EnergyData::createDefault)
                    .serialize(EnergyData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, EnergyData.STREAM_CODEC)
                    .build());

    /** Per-player flight toggles. Persisted, kept on death, synced to the owning player. */
    public static final Supplier<AttachmentType<FlightSettings>> FLIGHT_SETTINGS = ATTACHMENT_TYPES.register("flight_settings",
            () -> AttachmentType.builder(() -> FlightSettings.DEFAULT)
                    .serialize(FlightSettings.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, FlightSettings.STREAM_CODEC)
                    .build());

    /**
     * Thruster keys currently held. Transient: the client sets it locally every tick and sends changes to the server
     * with {@link com.voidreclaimer.network.ThrustInputPayload}.
     */
    public static final Supplier<AttachmentType<FlightInput>> FLIGHT_INPUT = ATTACHMENT_TYPES.register("flight_input",
            () -> AttachmentType.builder(() -> FlightInput.NONE).build());

    /** Server-only, transient: recent positions for the speed check. */
    public static final Supplier<AttachmentType<FlightTracker>> FLIGHT_TRACKER = ATTACHMENT_TYPES.register("flight_tracker",
            () -> AttachmentType.builder(FlightTracker::new).build());

    private ModAttachments() {}
}
