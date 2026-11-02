package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyCrash;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a body flew into something, and this is the speed that went into it, in blocks a tick. */
public record BodyCrashPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<BodyCrashPayload> TYPE = new Type<>(VoidReclaimer.id("body_crash"));
    public static final StreamCodec<ByteBuf, BodyCrashPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, BodyCrashPayload::x,
            ByteBufCodecs.DOUBLE, BodyCrashPayload::y,
            ByteBufCodecs.DOUBLE, BodyCrashPayload::z,
            BodyCrashPayload::new);

    public static BodyCrashPayload of(Vec3 lost) {
        return new BodyCrashPayload(lost.x, lost.y, lost.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the server thread. */
    static void handle(BodyCrashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BodyCrash.heard(player, new Vec3(payload.x(), payload.y(), payload.z()));
        }
    }
}
