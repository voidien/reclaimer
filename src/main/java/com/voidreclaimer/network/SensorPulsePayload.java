package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.SensorPulse;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the player pressed the sensor pulse key. The server checks sensor, cooldown and energy. */
public record SensorPulsePayload() implements CustomPacketPayload {
    public static final SensorPulsePayload INSTANCE = new SensorPulsePayload();
    public static final Type<SensorPulsePayload> TYPE = new Type<>(VoidReclaimer.id("sensor_pulse"));
    public static final StreamCodec<ByteBuf, SensorPulsePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SensorPulsePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SensorPulse.pulse(player);
        }
    }
}
