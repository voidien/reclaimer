package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.SensorPulse;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a sensor echo arrived. Direction is blurred; strength is all the player learns about distance. */
public record SensorEchoPayload(Vec3 direction, SensorPulse.Strength strength) implements CustomPacketPayload {
    public static final Type<SensorEchoPayload> TYPE = new Type<>(VoidReclaimer.id("sensor_echo"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.FLOAT, v -> (float) v.x,
            ByteBufCodecs.FLOAT, v -> (float) v.y,
            ByteBufCodecs.FLOAT, v -> (float) v.z,
            (x, y, z) -> new Vec3(x, y, z));
    private static final StreamCodec<ByteBuf, SensorPulse.Strength> STRENGTH = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Enum::ordinal, SensorPulse.Strength.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Enum::ordinal);
    public static final StreamCodec<ByteBuf, SensorEchoPayload> STREAM_CODEC = StreamCodec.composite(
            VEC3, SensorEchoPayload::direction,
            STRENGTH, SensorEchoPayload::strength,
            SensorEchoPayload::new);

    /**
     * Client-side receiver, installed by the client entrypoint. Keeps this common class free of client-only
     * references, so the dedicated server can load it.
     */
    public static volatile Consumer<SensorEchoPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SensorEchoPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
