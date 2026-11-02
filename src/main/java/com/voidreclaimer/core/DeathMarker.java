package com.voidreclaimer.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Where the player last died, so the HUD can point back. A marker with a wreck stays until that wreck has been emptied, and follows it as
 * it drifts; one without clears on arrival.
 *
 * @param pos   death position, empty when there is nothing to recover
 * @param wreck the main body's wreck, if it left one
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public record DeathMarker(Optional<GlobalPos> pos, Optional<UUID> wreck) {
    public static final DeathMarker NONE = new DeathMarker(Optional.empty(), Optional.empty());
    /** Within this distance the marker clears. */
    public static final double ARRIVE_DISTANCE = 8.0;
    private static final int CHECK_TICKS = 20;

    public static final Codec<DeathMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.lenientOptionalFieldOf("pos").forGetter(DeathMarker::pos),
            UUIDUtil.CODEC.lenientOptionalFieldOf("wreck").forGetter(DeathMarker::wreck)
    ).apply(instance, DeathMarker::new));
    public static final StreamCodec<ByteBuf, DeathMarker> STREAM_CODEC = StreamCodec.composite(
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), DeathMarker::pos,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), DeathMarker::wreck,
            DeathMarker::new);

    /** The marker for {@code wreck}, at {@code pos}. */
    public static void point(ServerPlayer player, UUID wreck, GlobalPos pos) {
        store(player, new DeathMarker(Optional.of(pos), Optional.of(wreck)));
    }

    /** {@code wreck} has moved. Only a block's worth of change is worth telling the client. */
    public static void follow(ServerPlayer player, UUID wreck, GlobalPos pos) {
        DeathMarker marker = player.getData(ModAttachments.DEATH_MARKER);
        if (marker.wreck().filter(wreck::equals).isPresent() && !marker.pos().filter(pos::equals).isPresent()) {
            store(player, new DeathMarker(Optional.of(pos), marker.wreck()));
        }
    }

    /** {@code wreck} has gone, emptied or destroyed. */
    public static void clear(ServerPlayer player, UUID wreck) {
        if (player.getData(ModAttachments.DEATH_MARKER).wreck().filter(wreck::equals).isPresent()) {
            store(player, NONE);
        }
    }

    private static void store(ServerPlayer player, DeathMarker marker) {
        player.setData(ModAttachments.DEATH_MARKER, marker);
        player.syncData(ModAttachments.DEATH_MARKER);
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()
                && !player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            store(player, new DeathMarker(Optional.of(GlobalPos.of(player.level().dimension(), player.blockPosition())), Optional.empty()));
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0 || !player.isAlive()) {
            return;
        }
        DeathMarker marker = player.getData(ModAttachments.DEATH_MARKER);
        Optional<GlobalPos> pos = marker.pos();
        if (marker.wreck().isEmpty() && pos.isPresent() && pos.get().dimension() == player.level().dimension()
                && player.position().distanceTo(Vec3.atCenterOf(pos.get().pos())) <= ARRIVE_DISTANCE) {
            store(player, NONE);
        }
    }
}
