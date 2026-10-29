package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/**
 * A body module (the {@code void_reclaimer:module} data component, a default component on module items). Modules go
 * into the mounts sections offer. Only the spec matching {@link #kind} is used.
 *
 * @param kind    which mounts it goes in
 * @param tier    the body's core must be at least this tier (a core's own tier is what it allows)
 * @param mass    adds to body mass
 * @param minSize smallest size class that can carry it, if any
 */
public record ModuleStats(ModuleKind kind, int tier, float mass, Optional<ResourceKey<SizeClass>> minSize,
                          Optional<Core> core, Optional<Thruster> thruster, Optional<Storage> storage) {
    public static final Codec<ModuleStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModuleKind.CODEC.fieldOf("kind").forGetter(ModuleStats::kind),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(ModuleStats::tier),
            Codec.FLOAT.optionalFieldOf("mass", 0f).forGetter(ModuleStats::mass),
            ModCodecs.key(VoidReclaimerRegistries.SIZE_CLASS).optionalFieldOf("min_size").forGetter(ModuleStats::minSize),
            Core.CODEC.optionalFieldOf("core").forGetter(ModuleStats::core),
            Thruster.CODEC.optionalFieldOf("thruster").forGetter(ModuleStats::thruster),
            Storage.CODEC.optionalFieldOf("storage").forGetter(ModuleStats::storage)
    ).apply(instance, ModuleStats::new));

    public static final StreamCodec<ByteBuf, ModuleStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static ModuleStats simple(ModuleKind kind, int tier, float mass) {
        return new ModuleStats(kind, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ModuleStats core(int tier, float mass, Core core) {
        return new ModuleStats(ModuleKind.CORE, tier, mass, Optional.empty(), Optional.of(core), Optional.empty(), Optional.empty());
    }

    public static ModuleStats thruster(int tier, float mass, Optional<ResourceKey<SizeClass>> minSize, Thruster thruster) {
        return new ModuleStats(ModuleKind.THRUSTER, tier, mass, minSize, Optional.empty(), Optional.of(thruster), Optional.empty());
    }

    public static ModuleStats storage(int tier, float mass, Storage storage) {
        return new ModuleStats(ModuleKind.STORAGE, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(storage));
    }

    /**
     * @param energyCapacity energy capacity
     * @param solarRate      sunlight charge multiplier
     */
    public record Core(float energyCapacity, float solarRate) {
        public static final Codec<Core> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("energy_capacity").forGetter(Core::energyCapacity),
                Codec.FLOAT.optionalFieldOf("solar_rate", 1f).forGetter(Core::solarRate)
        ).apply(instance, Core::new));
    }

    /**
     * @param thrust   force (blocks/tick² per unit mass); thrusters add up
     * @param brake    inertial dampener force; adds up
     * @param topSpeed top speed in blocks per second; the best thruster sets it
     */
    public record Thruster(float thrust, float brake, float topSpeed) {
        public static final Codec<Thruster> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("thrust").forGetter(Thruster::thrust),
                Codec.FLOAT.optionalFieldOf("brake", 0f).forGetter(Thruster::brake),
                Codec.FLOAT.fieldOf("top_speed").forGetter(Thruster::topSpeed)
        ).apply(instance, Thruster::new));
    }

    /**
     * @param rows         extra main-inventory rows
     * @param dustCapacity extra storage per dust kind
     */
    public record Storage(int rows, float dustCapacity) {
        public static final Codec<Storage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("rows", 0).forGetter(Storage::rows),
                Codec.FLOAT.optionalFieldOf("dust_capacity", 0f).forGetter(Storage::dustCapacity)
        ).apply(instance, Storage::new));
    }
}
