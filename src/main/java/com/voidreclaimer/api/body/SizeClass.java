package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * A body size class (Drone, Mech, Ship). Entries live in the synced datapack registry {@code void_reclaimer:size_class};
 * the name defaults to {@code body.<ns>.size.<path>}.
 *
 * @param scale value of the vanilla {@code generic.scale} attribute for bodies of this class
 * @param rank  ordering for "at least this size" checks (modules with a minimum size); built-ins 0, 1, 2
 * @param speedFactor multiplies the top speed of the body's best thruster, so a big body covers ground to match its size
 * @param maxSpeed    the most any body of this class can reach, in blocks a second, however good its thrusters
 */
public record SizeClass(double scale, int rank, Optional<Component> name, double speedFactor, double maxSpeed, double storageFactor, boolean staysInOrbit) {
    public static final double DEFAULT_MAX_SPEED = 40.0;


    public static final Codec<SizeClass> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0.0625, 16.0).fieldOf("scale").forGetter(SizeClass::scale),
            Codec.INT.fieldOf("rank").forGetter(SizeClass::rank),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(SizeClass::name),
            Codec.doubleRange(0.01, 100.0).optionalFieldOf("speed_factor", 1.0).forGetter(SizeClass::speedFactor),
            Codec.doubleRange(0.01, 150.0).optionalFieldOf("max_speed", DEFAULT_MAX_SPEED).forGetter(SizeClass::maxSpeed),
            Codec.doubleRange(0.01, 100.0).optionalFieldOf("storage_factor", 1.0).forGetter(SizeClass::storageFactor),
            Codec.BOOL.optionalFieldOf("stays_in_orbit", false).forGetter(SizeClass::staysInOrbit)
    ).apply(instance, SizeClass::new));

    public SizeClass(double scale, int rank) {
        this(scale, rank, 1.0);
    }

    public SizeClass(double scale, int rank, double speedFactor) {
        this(scale, rank, Optional.empty(), speedFactor, DEFAULT_MAX_SPEED, 1.0, false);
    }

    public SizeClass(double scale, int rank, double speedFactor, double storageFactor) {
        this(scale, rank, Optional.empty(), speedFactor, DEFAULT_MAX_SPEED, storageFactor, false);
    }

    public SizeClass(double scale, int rank, Optional<Component> name, double speedFactor, double maxSpeed) {
        this(scale, rank, name, speedFactor, maxSpeed, 1.0, false);
    }

    /** The same size, too big for a portal: bodies of this size stay in orbit and send a Shuttle down. */
    public SizeClass inOrbit() {
        return new SizeClass(scale, rank, name, speedFactor, maxSpeed, storageFactor, true);
    }

    public Component displayName(ResourceLocation id) {
        return name.orElseGet(() -> Component.translatable("body." + id.getNamespace() + ".size." + id.getPath()));
    }
}
