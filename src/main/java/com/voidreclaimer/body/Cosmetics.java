package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * How the body looks: a Create-style casing material, a free accent tint (lights, trim, pipes) and a thruster trail.
 * Chosen in the Body tab; rendered on the model in Phase 5b (trails are drawn already).
 */
public record Cosmetics(Casing casing, int accent, Trail trail) {
    public static final int DEFAULT_ACCENT = 0x7FD4FF;
    public static final Cosmetics DEFAULT = new Cosmetics(Casing.ANDESITE, DEFAULT_ACCENT, Trail.STANDARD);

    public static final Codec<Cosmetics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Casing.CODEC.optionalFieldOf("casing", Casing.ANDESITE).forGetter(Cosmetics::casing),
            Codec.INT.optionalFieldOf("accent", DEFAULT_ACCENT).forGetter(Cosmetics::accent),
            Trail.CODEC.optionalFieldOf("trail", Trail.STANDARD).forGetter(Cosmetics::trail)
    ).apply(instance, Cosmetics::new));

    public static final StreamCodec<ByteBuf, Cosmetics> STREAM_CODEC = StreamCodec.composite(
            Casing.STREAM_CODEC, Cosmetics::casing,
            ByteBufCodecs.INT, Cosmetics::accent,
            Trail.STREAM_CODEC, Cosmetics::trail,
            Cosmetics::new);

    public Cosmetics withCasing(Casing value) {
        return new Cosmetics(value, accent, trail);
    }

    public Cosmetics withAccent(int value) {
        return new Cosmetics(casing, value & 0xFFFFFF, trail);
    }

    public Cosmetics withTrail(Trail value) {
        return new Cosmetics(casing, accent, value);
    }

    /** Casing materials, after Create's casings. Later phases unlock more. */
    public enum Casing implements StringRepresentable {
        ANDESITE("andesite"), BRASS("brass"), COPPER("copper");

        public static final Codec<Casing> CODEC = StringRepresentable.fromEnum(Casing::values);
        private static final IntFunction<Casing> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Casing> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        private final String name;

        Casing(String name) {
            this.name = name;
        }

        public Casing next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Component displayName() {
            return Component.translatable("body.void_reclaimer.casing." + name);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Thruster exhaust particles, tinted per style. */
    public enum Trail implements StringRepresentable {
        NONE("none", 0), STANDARD("standard", 0xDDE6EE), EMBER("ember", 0xFF8A3D), PLASMA("plasma", 0x7FD4FF), ACCENT("accent", -1);

        public static final Codec<Trail> CODEC = StringRepresentable.fromEnum(Trail::values);
        private static final IntFunction<Trail> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<ByteBuf, Trail> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

        private final String name;
        private final int color;

        Trail(String name, int color) {
            this.name = name;
            this.color = color;
        }

        /** Particle color; {@link #ACCENT} uses the body's accent tint. */
        public int color(int accent) {
            return this == ACCENT ? accent : color;
        }

        public Trail next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Component displayName() {
            return Component.translatable("body.void_reclaimer.trail." + name);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
