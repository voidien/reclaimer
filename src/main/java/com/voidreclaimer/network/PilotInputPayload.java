package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.ship.PilotInput;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the six pilot axes now held at the Helm. Sent only when they change. */
public record PilotInputPayload(PilotInput input) implements CustomPacketPayload {
    public static final Type<PilotInputPayload> TYPE = new Type<>(VoidReclaimer.id("pilot_input"));
    public static final StreamCodec<ByteBuf, PilotInputPayload> STREAM_CODEC =
            PilotInput.STREAM_CODEC.map(PilotInputPayload::new, PilotInputPayload::input);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(PilotInputPayload payload, IPayloadContext context) {
        context.player().setData(ModAttachments.PILOT_INPUT, payload.input());
    }
}
