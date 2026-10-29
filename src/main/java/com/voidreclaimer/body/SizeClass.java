package com.voidreclaimer.body;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Body size class, set by the frame. Scale is applied through the vanilla {@code generic.scale} attribute. */
public enum SizeClass implements StringRepresentable {
    DRONE("drone", 0.4),
    MECH("mech", 1.2),
    SHIP("ship", 3.0);

    public static final Codec<SizeClass> CODEC = StringRepresentable.fromEnum(SizeClass::values);

    private final String name;
    private final double scale;

    SizeClass(String name, double scale) {
        this.name = name;
        this.scale = scale;
    }

    public double scale() {
        return scale;
    }

    public boolean atLeast(SizeClass other) {
        return ordinal() >= other.ordinal();
    }

    public Component displayName() {
        return Component.translatable("body.void_reclaimer.size." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
