package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightInput;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the thruster keys now held. Sent only when they change. */
public record ThrustInputPayload(FlightInput input) implements CustomPacketPayload {
    public static final Type<ThrustInputPayload> TYPE = new Type<>(VoidReclaimer.id("thrust_input"));
    public static final StreamCodec<ByteBuf, ThrustInputPayload> STREAM_CODEC =
            FlightInput.STREAM_CODEC.map(ThrustInputPayload::new, ThrustInputPayload::input);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(ThrustInputPayload payload, IPayloadContext context) {
        context.player().setData(ModAttachments.FLIGHT_INPUT, payload.input());
    }
}
