package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyTurning;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: where a big body points while it lags its view, and a last word when it catches up. Sent only when that changes. */
public record BodyHeadingPayload(boolean lagging, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<BodyHeadingPayload> TYPE = new Type<>(VoidReclaimer.id("body_heading"));
    public static final StreamCodec<ByteBuf, BodyHeadingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BodyHeadingPayload::lagging,
            ByteBufCodecs.FLOAT, BodyHeadingPayload::yaw,
            ByteBufCodecs.FLOAT, BodyHeadingPayload::pitch,
            BodyHeadingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(BodyHeadingPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BodyTurning.heard(player, payload.lagging(), payload.yaw(), payload.pitch());
        }
    }
}
