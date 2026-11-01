package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.ShellRow;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: opens the picker at the seat at {@code seat}. */
public record ShellListPayload(BlockPos seat, List<ShellRow> rows) implements CustomPacketPayload {
    public static final Type<ShellListPayload> TYPE = new Type<>(VoidReclaimer.id("shell_list"));
    public static final StreamCodec<ByteBuf, ShellListPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShellListPayload::seat,
            ShellRow.STREAM_CODEC.apply(ByteBufCodecs.list()), ShellListPayload::rows,
            ShellListPayload::new);

    /** Client-side receiver, installed by the client entrypoint. Keeps this common class free of client-only references. */
    public static volatile Consumer<ShellListPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ShellListPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
