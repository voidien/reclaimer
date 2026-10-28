package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCores;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the death screen's respawn choice, a Backup Core or empty for the free deep space spawn. */
public record RespawnChoicePayload(Optional<GlobalPos> core) implements CustomPacketPayload {
    public static final Type<RespawnChoicePayload> TYPE = new Type<>(VoidReclaimer.id("respawn_choice"));
    public static final StreamCodec<ByteBuf, RespawnChoicePayload> STREAM_CODEC =
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional).map(RespawnChoicePayload::new, RespawnChoicePayload::core);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RespawnChoicePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BackupCores.select(player, payload.core());
        }
    }
}
