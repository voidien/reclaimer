package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.BackupCores;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the core screen renames or charges a Backup Core. The server checks ownership and distance. */
public record CoreActionPayload(BlockPos pos, boolean charge, String name) implements CustomPacketPayload {
    public static final Type<CoreActionPayload> TYPE = new Type<>(VoidReclaimer.id("core_action"));
    public static final StreamCodec<ByteBuf, CoreActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoreActionPayload::pos,
            ByteBufCodecs.BOOL, CoreActionPayload::charge,
            ByteBufCodecs.stringUtf8(BackupCoreBlockEntity.MAX_NAME_LENGTH * 4), CoreActionPayload::name,
            CoreActionPayload::new);

    public static CoreActionPayload charge(BlockPos pos) {
        return new CoreActionPayload(pos, true, "");
    }

    public static CoreActionPayload rename(BlockPos pos, String name) {
        return new CoreActionPayload(pos, false, name);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(CoreActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.charge()) {
            BackupCores.charge(player, payload.pos());
        } else {
            BackupCores.rename(player, payload.pos(), payload.name());
        }
    }
}
