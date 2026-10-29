package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a Mass Driver fired. The deep space sky draws the payload as a blue streak towards the star. */
public record LaunchStreakPayload(BlockPos from, boolean truss) implements CustomPacketPayload {
    public static final Type<LaunchStreakPayload> TYPE = new Type<>(VoidReclaimer.id("launch_streak"));

    public static final StreamCodec<ByteBuf, LaunchStreakPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LaunchStreakPayload::from,
            ByteBufCodecs.BOOL, LaunchStreakPayload::truss,
            LaunchStreakPayload::new);

    /** Client-side receiver, installed by the client entrypoint; keeps this class free of client references. */
    public static volatile Consumer<LaunchStreakPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(LaunchStreakPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
