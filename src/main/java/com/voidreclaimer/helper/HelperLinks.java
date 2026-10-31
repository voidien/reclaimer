package com.voidreclaimer.helper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a helper drone works on: either a source inventory (the ferry) or a mine point near asteroid rock (mining), and
 * the destination inventory it brings things to. Set on the drone's item, carried by the drone.
 */
public record HelperLinks(Optional<BlockPos> source, Optional<BlockPos> destination, Optional<BlockPos> mine) {
    public static final HelperLinks EMPTY = new HelperLinks(Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<HelperLinks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("source").forGetter(HelperLinks::source),
            BlockPos.CODEC.optionalFieldOf("destination").forGetter(HelperLinks::destination),
            BlockPos.CODEC.optionalFieldOf("mine").forGetter(HelperLinks::mine)
    ).apply(instance, HelperLinks::new));
    public static final StreamCodec<ByteBuf, HelperLinks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HelperLinks::source,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HelperLinks::destination,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HelperLinks::mine,
            HelperLinks::new);

    /** Ferry links only. */
    public HelperLinks(Optional<BlockPos> source, Optional<BlockPos> destination) {
        this(source, destination, Optional.empty());
    }

    /** A source makes a ferry: it replaces any mine point. */
    public HelperLinks withSource(BlockPos pos) {
        return new HelperLinks(Optional.of(pos.immutable()), destination, Optional.empty());
    }

    public HelperLinks withDestination(BlockPos pos) {
        return new HelperLinks(source, Optional.of(pos.immutable()), mine);
    }

    /** A mine point makes a miner: it replaces any source. */
    public HelperLinks withMine(BlockPos pos) {
        return new HelperLinks(Optional.empty(), destination, Optional.of(pos.immutable()));
    }

    /** Whether the drone mines (a mine point) rather than ferries (a source). */
    public boolean mining() {
        return mine.isPresent();
    }

    /** A job and a destination: the drone can work. */
    public boolean complete() {
        return (source.isPresent() || mine.isPresent()) && destination.isPresent();
    }
}
