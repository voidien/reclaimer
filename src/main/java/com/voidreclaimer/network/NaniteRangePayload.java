package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Modules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: set the build distance of the active Nanite Swarm module (sneak + scroll). */
public record NaniteRangePayload(int range) implements CustomPacketPayload {
    public static final Type<NaniteRangePayload> TYPE = new Type<>(VoidReclaimer.id("nanite_range"));
    public static final StreamCodec<ByteBuf, NaniteRangePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(NaniteRangePayload::new, NaniteRangePayload::range);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(NaniteRangePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Modules.setNaniteRange(player, payload.range());
        }
    }
}
