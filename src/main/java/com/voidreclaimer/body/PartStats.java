package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stats of one body part, stored as a data component on part items (a default component, so balance changes in code
 * reach existing items). Only the spec matching {@link #kind} is present.
 *
 * @param kind    which slot the part goes in
 * @param tier    the core must be at least this tier to install it
 * @param mass    adds to body mass (handling); over the frame's budget, handling and energy use get worse
 * @param minSize smallest frame size class that can carry it
 */
public record PartStats(PartKind kind, int tier, float mass, SizeClass minSize,
                        Optional<Frame> frame, Optional<Core> core, Optional<Thruster> thruster, Optional<Storage> storage) {
    public static final Codec<PartStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PartKind.CODEC.fieldOf("kind").forGetter(PartStats::kind),
            Codec.INT.optionalFieldOf("tier", 1).forGetter(PartStats::tier),
            Codec.FLOAT.optionalFieldOf("mass", 0f).forGetter(PartStats::mass),
            SizeClass.CODEC.optionalFieldOf("min_size", SizeClass.DRONE).forGetter(PartStats::minSize),
            Frame.CODEC.optionalFieldOf("frame").forGetter(PartStats::frame),
            Core.CODEC.optionalFieldOf("core").forGetter(PartStats::core),
            Thruster.CODEC.optionalFieldOf("thruster").forGetter(PartStats::thruster),
            Storage.CODEC.optionalFieldOf("storage").forGetter(PartStats::storage)
    ).apply(instance, PartStats::new));

    public static final StreamCodec<ByteBuf, PartStats> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public static PartStats simple(PartKind kind, int tier, float mass) {
        return new PartStats(kind, tier, mass, SizeClass.DRONE, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static PartStats frame(int tier, float mass, Frame frame) {
        return new PartStats(PartKind.FRAME, tier, mass, SizeClass.DRONE, Optional.of(frame), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static PartStats core(int tier, float mass, Core core) {
        return new PartStats(PartKind.CORE, tier, mass, SizeClass.DRONE, Optional.empty(), Optional.of(core), Optional.empty(), Optional.empty());
    }

    public static PartStats thruster(int tier, float mass, SizeClass minSize, Thruster thruster) {
        return new PartStats(PartKind.THRUSTER, tier, mass, minSize, Optional.empty(), Optional.empty(), Optional.of(thruster), Optional.empty());
    }

    public static PartStats storage(int tier, float mass, Storage storage) {
        return new PartStats(PartKind.STORAGE, tier, mass, SizeClass.DRONE, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(storage));
    }

    /**
     * @param size       size class (sets the body scale)
     * @param massBudget mass the frame carries without penalty
     * @param maxHealth  max health in half-hearts
     * @param reach      extra block and entity interaction range
     * @param dustIntake dust collection multiplier
     * @param baseRows   main-inventory rows (beyond the hotbar) available without storage parts
     * @param slots      slot count for each part kind (frame and core always have one)
     */
    public record Frame(SizeClass size, float massBudget, float maxHealth, float reach, float dustIntake, int baseRows,
                        Map<PartKind, Integer> slots) {
        public static final Codec<Frame> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SizeClass.CODEC.fieldOf("size").forGetter(Frame::size),
                Codec.FLOAT.optionalFieldOf("mass_budget", 1f).forGetter(Frame::massBudget),
                Codec.FLOAT.optionalFieldOf("max_health", 20f).forGetter(Frame::maxHealth),
                Codec.FLOAT.optionalFieldOf("reach", 0f).forGetter(Frame::reach),
                Codec.FLOAT.optionalFieldOf("dust_intake", 1f).forGetter(Frame::dustIntake),
                Codec.INT.optionalFieldOf("base_rows", 1).forGetter(Frame::baseRows),
                Codec.unboundedMap(PartKind.CODEC, Codec.INT).optionalFieldOf("slots", Map.of()).forGetter(Frame::slots)
        ).apply(instance, Frame::new));

        public int slots(PartKind kind) {
            return kind == PartKind.FRAME || kind == PartKind.CORE ? 1 : slots.getOrDefault(kind, 0);
        }
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
