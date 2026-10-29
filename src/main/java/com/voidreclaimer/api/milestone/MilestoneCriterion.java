package com.voidreclaimer.api.milestone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.voidreclaimer.registry.ModRegistries;
import java.util.function.Function;

/**
 * When a {@link Milestone} is reached. In JSON a criterion is an object with a {@code type} naming an entry of the
 * code registry {@code void_reclaimer:milestone_criterion_type}, plus that type's fields. Addons register the
 * {@link MapCodec} of their own criterion records there.
 */
public interface MilestoneCriterion {
    Codec<MilestoneCriterion> CODEC = Codec.lazyInitialized(() ->
            ModRegistries.MILESTONE_CRITERION_TYPES.byNameCodec().dispatch(MilestoneCriterion::codec, Function.identity()));

    /** Whether the player state meets the criterion. Must not have side effects. */
    boolean isReached(MilestoneContext context);

    /** The codec registered for this criterion's type. */
    MapCodec<? extends MilestoneCriterion> codec();
}
