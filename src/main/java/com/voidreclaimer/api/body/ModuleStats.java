package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * A body module (the {@code void_reclaimer:module} data component, a default component on module items). Modules go
 * into the mounts sections offer. A kind has the spec it expects; a utility module does what the specs it carries say.
 *
 * @param kind    which mounts it goes in
 * @param tier    the body's core must be at least this tier (a core's own tier is what it allows)
 * @param mass    adds to body mass
 * @param minSize smallest size class that can carry it, if any
 */
public record ModuleStats(ModuleKind kind, int tier, float mass, Optional<ResourceKey<SizeClass>> minSize,
                          Optional<Core> core, Optional<Thruster> thruster, Optional<Storage> storage,
                          Optional<Collector> collector, Optional<Plating> plating) {
    public static final Codec<ModuleStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModuleKind.CODEC.fieldOf("kind").forGetter(ModuleStats::kind),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(ModuleStats::tier),
            Codec.FLOAT.optionalFieldOf("mass", 0f).forGetter(ModuleStats::mass),
            ModCodecs.key(VoidReclaimerRegistries.SIZE_CLASS).optionalFieldOf("min_size").forGetter(ModuleStats::minSize),
            Core.CODEC.optionalFieldOf("core").forGetter(ModuleStats::core),
            Thruster.CODEC.optionalFieldOf("thruster").forGetter(ModuleStats::thruster),
            Storage.CODEC.optionalFieldOf("storage").forGetter(ModuleStats::storage),
            Collector.CODEC.optionalFieldOf("collector").forGetter(ModuleStats::collector),
            Plating.CODEC.optionalFieldOf("plating").forGetter(ModuleStats::plating)
    ).apply(instance, ModuleStats::new));

    public static final StreamCodec<ByteBuf, ModuleStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static ModuleStats simple(ModuleKind kind, int tier, float mass) {
        return new ModuleStats(kind, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ModuleStats core(int tier, float mass, Core core) {
        return new ModuleStats(ModuleKind.CORE, tier, mass, Optional.empty(), Optional.of(core), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ModuleStats thruster(int tier, float mass, Optional<ResourceKey<SizeClass>> minSize, Thruster thruster) {
        return new ModuleStats(ModuleKind.THRUSTER, tier, mass, minSize, Optional.empty(), Optional.of(thruster), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ModuleStats storage(int tier, float mass, Storage storage) {
        return new ModuleStats(ModuleKind.STORAGE, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(storage), Optional.empty(), Optional.empty());
    }

    /** A utility module that adds dust or inventory storage, like a storage pod but in a utility mount. */
    public static ModuleStats utility(int tier, float mass, Storage storage) {
        return new ModuleStats(ModuleKind.UTILITY, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(storage), Optional.empty(), Optional.empty());
    }

    /** A utility module that armours the body. */
    public static ModuleStats utility(int tier, float mass, Plating plating) {
        return new ModuleStats(ModuleKind.UTILITY, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(plating));
    }

    public static ModuleStats collector(int tier, float mass, Collector collector) {
        return new ModuleStats(ModuleKind.TOOL, tier, mass, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(collector), Optional.empty());
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

    /**
     * A raw-item collector: while the module is installed and the body is powered, dust gathered in a cloud also
     * produces raw items of the cloud's kind.
     *
     * @param yield           items produced per unit of dust gathered (the Replicator makes 0.5 per dust)
     * @param energyPerSecond energy used per second spent inside a cloud
     * @param outputs         item produced for each dust kind id; a kind with no entry produces nothing
     */
    public record Collector(float yield, float energyPerSecond, Map<ResourceLocation, ResourceLocation> outputs) {
        public static final Codec<Collector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("yield", 1f).forGetter(Collector::yield),
                Codec.FLOAT.optionalFieldOf("energy_per_second", 1f).forGetter(Collector::energyPerSecond),
                Codec.unboundedMap(ModCodecs.ID, ModCodecs.ID).fieldOf("outputs").forGetter(Collector::outputs)
        ).apply(instance, Collector::new));

        public Collector {
            outputs = Map.copyOf(outputs);
        }
    }

    /**
     * @param health adds to the body's health, like a section's, and fades with the wear of the section holding it
     */
    public record Plating(float health) {
        public static final Codec<Plating> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("health").forGetter(Plating::health)
        ).apply(instance, Plating::new));
    }
}
