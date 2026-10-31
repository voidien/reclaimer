package com.voidreclaimer.helper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Where a helper drone takes items from and brings them to: set on the drone's item, carried by the drone. */
public record HelperLinks(Optional<BlockPos> source, Optional<BlockPos> destination) {
    public static final HelperLinks EMPTY = new HelperLinks(Optional.empty(), Optional.empty());
    public static final Codec<HelperLinks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("source").forGetter(HelperLinks::source),
            BlockPos.CODEC.optionalFieldOf("destination").forGetter(HelperLinks::destination)
    ).apply(instance, HelperLinks::new));
    public static final StreamCodec<ByteBuf, HelperLinks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HelperLinks::source,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), HelperLinks::destination,
            HelperLinks::new);

    public HelperLinks withSource(BlockPos pos) {
        return new HelperLinks(Optional.of(pos.immutable()), destination);
    }

    public HelperLinks withDestination(BlockPos pos) {
        return new HelperLinks(source, Optional.of(pos.immutable()));
    }

    /** Both ends set: the drone can ferry. */
    public boolean complete() {
        return source.isPresent() && destination.isPresent();
    }
}
