package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: how far a pilot moved the mouse since the last one. See {@link com.voidreclaimer.ship.PilotMouse}. */
public record PilotMousePayload(float right, float up) implements CustomPacketPayload {
    public static final Type<PilotMousePayload> TYPE = new Type<>(VoidReclaimer.id("pilot_mouse"));
    public static final StreamCodec<ByteBuf, PilotMousePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PilotMousePayload::right,
            ByteBufCodecs.FLOAT, PilotMousePayload::up,
            PilotMousePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(PilotMousePayload payload, IPayloadContext context) {
        context.player().setData(ModAttachments.PILOT_MOUSE, context.player().getData(ModAttachments.PILOT_MOUSE).plus(payload.right(), payload.up()));
    }
}
