package com.voidreclaimer.tether;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-player tether state, synced to the owner (the client runs the leash and recall physics).
 *
 * @param anchor          the last anchor clipped to: the leash point while clipped, and the recall target
 * @param clipped         leashed to the anchor right now
 * @param recalling       the recall winch is pulling right now (set by the server)
 * @param teleportReadyAt game time when the long-range recall teleport can be used again
 */
public record TetherData(Optional<GlobalPos> anchor, boolean clipped, boolean recalling, long teleportReadyAt) {
    public static final TetherData NONE = new TetherData(Optional.empty(), false, false, 0L);

    public static final Codec<TetherData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.lenientOptionalFieldOf("anchor").forGetter(TetherData::anchor),
            Codec.BOOL.optionalFieldOf("clipped", false).forGetter(TetherData::clipped),
            // Not saved: a recall never survives a reload.
            Codec.LONG.optionalFieldOf("teleport_ready_at", 0L).forGetter(TetherData::teleportReadyAt)
    ).apply(instance, (anchor, clipped, readyAt) -> new TetherData(anchor, clipped, false, readyAt)));

    public static final StreamCodec<ByteBuf, TetherData> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), TetherData::anchor,
            ByteBufCodecs.BOOL, TetherData::clipped,
            ByteBufCodecs.BOOL, TetherData::recalling,
            ByteBufCodecs.VAR_LONG, TetherData::teleportReadyAt,
            TetherData::new);

    public TetherData withClip(GlobalPos pos) {
        return new TetherData(Optional.of(pos), true, recalling, teleportReadyAt);
    }

    public TetherData withClipped(boolean value) {
        return new TetherData(anchor, value, recalling, teleportReadyAt);
    }

    public TetherData withRecalling(boolean value) {
        return new TetherData(anchor, clipped, value, teleportReadyAt);
    }

    public TetherData withTeleportReadyAt(long time) {
        return new TetherData(anchor, clipped, recalling, time);
    }

    public boolean isAnchoredAt(GlobalPos pos) {
        return anchor.isPresent() && anchor.get().equals(pos);
    }
}
