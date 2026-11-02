package com.voidreclaimer.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * One small box of a big body, along its model. It belongs to a {@link Socket}, so it goes when the section does, and it turns with
 * the body. Both vectors are in blocks at scale 1, multiplied by the size class's scale, like {@link Chassis.Cockpit}.
 *
 * @param at   the box's middle from the body's feet: right, up, forward
 * @param size width (right), height (up), length (forward), each above 0
 */
public record HitBox(Vec3 at, Vec3 size) {
    private static final Codec<Vec3> SIZE = Vec3.CODEC.validate(size -> size.x > 0.0 && size.y > 0.0 && size.z > 0.0
            ? DataResult.success(size) : DataResult.error(() -> "a hit box needs a size above 0 each way: " + size));

    public static final Codec<HitBox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("at").forGetter(HitBox::at),
            SIZE.fieldOf("size").forGetter(HitBox::size)
    ).apply(instance, HitBox::new));
}
