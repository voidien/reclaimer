package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.tether.Tether;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the recall key was pressed or released. Sent on change only. */
public record TetherRecallPayload(boolean held) implements CustomPacketPayload {
    public static final Type<TetherRecallPayload> TYPE = new Type<>(VoidReclaimer.id("tether_recall"));
    public static final StreamCodec<ByteBuf, TetherRecallPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(TetherRecallPayload::new, TetherRecallPayload::held);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(TetherRecallPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Tether.setRecallHeld(player, payload.held());
        }
    }
}
