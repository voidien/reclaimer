package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.energy.EnergyData;
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

    private ModAttachments() {}
}
