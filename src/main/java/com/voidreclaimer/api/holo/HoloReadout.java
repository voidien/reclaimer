package com.voidreclaimer.api.holo;

import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * One kind of built-in readout a Holo Projector can show, registered in {@code void_reclaimer:holo_readout}. The sampler
 * reads the world into a plain snapshot on the server; the formatter turns the snapshot into lines and touches nothing
 * else, so it can be tested alone. Lines past the projector's caps are cut off.
 *
 * @param <S> the snapshot
 */
public record HoloReadout<S>(Sampler<S> sampler, Function<S, List<String>> formatter) {
    /** Reads the world around a projector. */
    @FunctionalInterface
    public interface Sampler<S> {
        /** The snapshot for the projector at {@code pos}, or null when there is nothing to read. */
        @Nullable
        S sample(Level level, BlockPos pos);
    }

    /** The lines to show now: none when there is nothing to read. */
    public List<String> lines(Level level, BlockPos pos) {
        S snapshot = sampler.sample(level, pos);
        return snapshot == null ? List.of() : formatter.apply(snapshot);
    }
}
