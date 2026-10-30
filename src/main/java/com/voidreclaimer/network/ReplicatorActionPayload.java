package com.voidreclaimer.network;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.replicator.Replicator;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a Replicator screen action. The server validates everything (unlock, queue size, dust). */
public record ReplicatorActionPayload(Action action, Optional<ResourceLocation> recipe, int index, int to) implements CustomPacketPayload {
    public static final Type<ReplicatorActionPayload> TYPE = new Type<>(VoidReclaimer.id("replicator_action"));

    public enum Action {
        QUEUE, CANCEL, COLLECT, MOVE;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
    }

    public static final StreamCodec<ByteBuf, ReplicatorActionPayload> STREAM_CODEC = StreamCodec.composite(
            Action.STREAM_CODEC, ReplicatorActionPayload::action,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), ReplicatorActionPayload::recipe,
            ByteBufCodecs.VAR_INT, ReplicatorActionPayload::index,
            ByteBufCodecs.VAR_INT, ReplicatorActionPayload::to,
            ReplicatorActionPayload::new);

    public static ReplicatorActionPayload queue(ResourceLocation recipe) {
        return new ReplicatorActionPayload(Action.QUEUE, Optional.of(recipe), 0, 0);
    }

    public static ReplicatorActionPayload cancel(int index) {
        return new ReplicatorActionPayload(Action.CANCEL, Optional.empty(), index, 0);
    }

    /** Moves queued job {@code from} to position {@code to}; the job being worked on (index 0) never moves. */
    public static ReplicatorActionPayload move(int from, int to) {
        return new ReplicatorActionPayload(Action.MOVE, Optional.empty(), from, to);
    }

    public static ReplicatorActionPayload collect() {
        return new ReplicatorActionPayload(Action.COLLECT, Optional.empty(), 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(ReplicatorActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case QUEUE -> payload.recipe().ifPresent(id -> Replicator.queue(player, id));
            case CANCEL -> Replicator.cancel(player, payload.index());
            case COLLECT -> Replicator.collect(player);
            case MOVE -> Replicator.move(player, payload.index(), payload.to());
        }
    }
}
