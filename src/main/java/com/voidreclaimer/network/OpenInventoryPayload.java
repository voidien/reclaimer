package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyInventoryMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the player pressed E. The server opens the body inventory (survival and adventure only). */
public record OpenInventoryPayload() implements CustomPacketPayload {
    public static final OpenInventoryPayload INSTANCE = new OpenInventoryPayload();
    public static final Type<OpenInventoryPayload> TYPE = new Type<>(VoidReclaimer.id("open_inventory"));
    public static final StreamCodec<ByteBuf, OpenInventoryPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(OpenInventoryPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BodyInventoryMenu.open(player);
        }
    }
}
