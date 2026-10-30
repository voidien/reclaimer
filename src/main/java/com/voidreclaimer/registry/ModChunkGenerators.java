package com.voidreclaimer.registry;

import com.mojang.serialization.MapCodec;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.asteroid.AsteroidChunkGenerator;
import com.voidreclaimer.earth.RuinedEarthChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, VoidReclaimer.MOD_ID);

    static {
        CHUNK_GENERATORS.register("asteroids", () -> AsteroidChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("ruined_earth", () -> RuinedEarthChunkGenerator.CODEC);
    }

    private ModChunkGenerators() {}
}
