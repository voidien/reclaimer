package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.PartKind;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: a Body tab or module wheel action. The server validates everything ({@link Bodies}).
 *
 * @param kind          slot kind (install, remove)
 * @param index         slot index (install, remove) or tool index (select)
 * @param inventorySlot source inventory slot (install)
 * @param cosmetics     new looks (cosmetics)
 */
public record BodyActionPayload(Action action, PartKind kind, int index, int inventorySlot, Cosmetics cosmetics)
        implements CustomPacketPayload {
    public static final Type<BodyActionPayload> TYPE = new Type<>(VoidReclaimer.id("body_action"));

    public enum Action {
        INSTALL, REMOVE, SELECT_TOOL, COSMETICS, USE_TOOL;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    public static final StreamCodec<ByteBuf, BodyActionPayload> STREAM_CODEC = StreamCodec.composite(
            Action.STREAM_CODEC, BodyActionPayload::action,
            PartKind.STREAM_CODEC, BodyActionPayload::kind,
            ByteBufCodecs.VAR_INT, BodyActionPayload::index,
            ByteBufCodecs.VAR_INT, BodyActionPayload::inventorySlot,
            Cosmetics.STREAM_CODEC, BodyActionPayload::cosmetics,
            BodyActionPayload::new);

    public static BodyActionPayload install(PartKind kind, int index, int inventorySlot) {
        return new BodyActionPayload(Action.INSTALL, kind, index, inventorySlot, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload remove(PartKind kind, int index) {
        return new BodyActionPayload(Action.REMOVE, kind, index, 0, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload selectTool(int index) {
        return new BodyActionPayload(Action.SELECT_TOOL, PartKind.TOOL, index, 0, Cosmetics.DEFAULT);
    }

    public static BodyActionPayload cosmetics(Cosmetics cosmetics) {
        return new BodyActionPayload(Action.COSMETICS, PartKind.COSMETIC, 0, 0, cosmetics);
    }

    public static BodyActionPayload useTool() {
        return new BodyActionPayload(Action.USE_TOOL, PartKind.TOOL, 0, 0, Cosmetics.DEFAULT);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BodyActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case INSTALL -> Bodies.install(player, payload.kind(), payload.index(), payload.inventorySlot());
            case REMOVE -> Bodies.remove(player, payload.kind(), payload.index());
            case SELECT_TOOL -> Bodies.selectTool(player, payload.index());
            case COSMETICS -> Bodies.setCosmetics(player, payload.cosmetics());
            case USE_TOOL -> com.voidreclaimer.body.Modules.use(player);
        }
    }
}
