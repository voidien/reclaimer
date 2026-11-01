package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.UplinkSeatBlockEntity;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a shell was picked at the seat at {@code seat}. */
public record PickShellPayload(BlockPos seat, UUID shell) implements CustomPacketPayload {
    public static final Type<PickShellPayload> TYPE = new Type<>(VoidReclaimer.id("pick_shell"));
    public static final StreamCodec<ByteBuf, PickShellPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PickShellPayload::seat,
            UUIDUtil.STREAM_CODEC, PickShellPayload::shell,
            PickShellPayload::new);
    /** As far as a Backup Core's screen stays open. */
    private static final double USE_DISTANCE = 8.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(PickShellPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            apply(player, payload.seat(), payload.shell());
        }
    }

    /** A client can send anything: the seat must be there and in reach, and the link makes the rest of the checks. */
    public static void apply(ServerPlayer player, BlockPos seat, UUID shell) {
        if (player.distanceToSqr(seat.getCenter()) <= USE_DISTANCE * USE_DISTANCE && player.level().getBlockEntity(seat) instanceof UplinkSeatBlockEntity found) {
            found.connect(player, shell);
        }
    }
}
