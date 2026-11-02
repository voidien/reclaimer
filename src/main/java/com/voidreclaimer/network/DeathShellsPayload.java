package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.ShellRow;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client, just before the death screen: the shells a player could wake in. */
public record DeathShellsPayload(List<ShellRow> rows) implements CustomPacketPayload {
    public static final Type<DeathShellsPayload> TYPE = new Type<>(VoidReclaimer.id("death_shells"));
    public static final StreamCodec<ByteBuf, DeathShellsPayload> STREAM_CODEC =
            ShellRow.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DeathShellsPayload::new, DeathShellsPayload::rows);

    /** Client-side receiver, installed by the client entrypoint. Keeps this common class free of client-only references. */
    public static volatile Consumer<DeathShellsPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(DeathShellsPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
