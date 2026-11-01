package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.ShellRecovery;
import com.voidreclaimer.link.UplinkSeatBlockEntity;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: bring back the old copy of a shell, confirmed at the seat at {@code seat}. */
public record RecoverShellPayload(BlockPos seat, UUID shell) implements CustomPacketPayload {
    public static final Type<RecoverShellPayload> TYPE = new Type<>(VoidReclaimer.id("recover_shell"));
    public static final StreamCodec<ByteBuf, RecoverShellPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RecoverShellPayload::seat,
            UUIDUtil.STREAM_CODEC, RecoverShellPayload::shell,
            RecoverShellPayload::new);
    private static final double USE_DISTANCE = 8.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RecoverShellPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            apply(player, payload.seat(), payload.shell());
        }
    }

    /** A client can send anything: the seat must be there and in reach, and {@link ShellRecovery} makes the rest of the checks. */
    public static void apply(ServerPlayer player, BlockPos seat, UUID shell) {
        if (player.distanceToSqr(seat.getCenter()) <= USE_DISTANCE * USE_DISTANCE && player.level().getBlockEntity(seat) instanceof UplinkSeatBlockEntity) {
            ShellRecovery.request(player, shell, result -> player.displayClientMessage(
                    Component.translatable("message.void_reclaimer.recover." + result.name().toLowerCase(Locale.ROOT)), false));
        }
    }
}
