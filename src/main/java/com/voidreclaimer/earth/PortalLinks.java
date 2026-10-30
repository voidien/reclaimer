package com.voidreclaimer.earth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What ties the portals together, saved on the {@code ruined_earth} level: the return gate built on the planet, and
 * where the last portal that sent someone here stands, so the way back ends where the trip began.
 *
 * @param gate the return gate on ruined Earth, once it has been built
 * @param exit where the way back leaves the origin portal, once someone has used one
 */
public record PortalLinks(Optional<Gate> gate, Optional<Exit> exit) {
    public static final PortalLinks NONE = new PortalLinks(Optional.empty(), Optional.empty());

    /** The return gate's interior: bottom-left block and size (its width runs along X). */
    public record Gate(BlockPos bottomLeft, int width, int height) {
        public static final Codec<Gate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("bottom_left").forGetter(Gate::bottomLeft),
                Codec.INT.fieldOf("width").forGetter(Gate::width),
                Codec.INT.fieldOf("height").forGetter(Gate::height)
        ).apply(instance, Gate::new));

        public PortalShape.Rect rect() {
            return new PortalShape.Rect(bottomLeft, Direction.Axis.X, width, height);
        }
    }

    /** The middle of an origin portal's floor, and the side of it that the way back comes out on. */
    public record Exit(ResourceKey<Level> dimension, Vec3 base, Direction side) {
        public static final Codec<Exit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Exit::dimension),
                Vec3.CODEC.fieldOf("base").forGetter(Exit::base),
                Direction.CODEC.fieldOf("side").forGetter(Exit::side)
        ).apply(instance, Exit::new));
    }

    public static final Codec<PortalLinks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Gate.CODEC.optionalFieldOf("gate").forGetter(PortalLinks::gate),
            Exit.CODEC.optionalFieldOf("exit").forGetter(PortalLinks::exit)
    ).apply(instance, PortalLinks::new));

    public PortalLinks withGate(Gate value) {
        return new PortalLinks(Optional.of(value), exit);
    }

    public PortalLinks withExit(Exit value) {
        return new PortalLinks(gate, Optional.of(value));
    }
}
