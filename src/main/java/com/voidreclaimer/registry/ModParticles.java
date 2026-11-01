package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Our own particle types; their providers are registered in {@code VoidReclaimerClient}. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, VoidReclaimer.MOD_ID);

    /** A falling drop of Acid Drizzle. */
    public static final Supplier<SimpleParticleType> ACID_DROP = PARTICLE_TYPES.register("acid_drop", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
