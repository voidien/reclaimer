package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.Links;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the Leave Shell key was pressed. */
public record UnlinkPayload() implements CustomPacketPayload {
    public static final Type<UnlinkPayload> TYPE = new Type<>(VoidReclaimer.id("unlink"));
    public static final StreamCodec<ByteBuf, UnlinkPayload> STREAM_CODEC = StreamCodec.unit(new UnlinkPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(UnlinkPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            apply(player);
        }
    }

    /** What the key does: a wake, asked for like any other. A player who is not in a shell gets nothing. */
    public static void apply(ServerPlayer player) {
        Links.requestWake(player, result -> {});
    }
}
