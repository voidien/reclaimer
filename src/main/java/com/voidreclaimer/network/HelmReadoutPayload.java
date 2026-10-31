package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.ship.HelmBlockEntity;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to the pilot, every tick in the chair: what the Helm sees of its ship, for the pilot HUD. */
public record HelmReadoutPayload(HelmBlockEntity.Readout readout) implements CustomPacketPayload {
    public static final Type<HelmReadoutPayload> TYPE = new Type<>(VoidReclaimer.id("helm_readout"));
    public static final StreamCodec<ByteBuf, HelmReadoutPayload> STREAM_CODEC =
            HelmBlockEntity.Readout.STREAM_CODEC.map(HelmReadoutPayload::new, HelmReadoutPayload::readout);

    /**
     * Client-side receiver, installed by the client entrypoint. Keeps this common class free of client-only
     * references, so the dedicated server can load it.
     */
    public static volatile Consumer<HelmReadoutPayload> clientHandler = payload -> {};

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HelmReadoutPayload payload, IPayloadContext context) {
        clientHandler.accept(payload);
    }
}
