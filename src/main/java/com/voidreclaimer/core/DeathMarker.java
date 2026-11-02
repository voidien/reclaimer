package com.voidreclaimer.core;

import com.mojang.serialization.Codec;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
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
 * Where the player last died, so the HUD can point back to the wreck. Cleared on arrival.
 *
 * @param pos death position, empty when there is nothing to recover
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public record DeathMarker(Optional<GlobalPos> pos) {
    public static final DeathMarker NONE = new DeathMarker(Optional.empty());
    /** Within this distance the marker clears. */
    public static final double ARRIVE_DISTANCE = 8.0;
    private static final int CHECK_TICKS = 20;

    public static final Codec<DeathMarker> CODEC = GlobalPos.CODEC.lenientOptionalFieldOf("pos").codec()
            .xmap(DeathMarker::new, DeathMarker::pos);
    public static final StreamCodec<ByteBuf, DeathMarker> STREAM_CODEC =
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional).map(DeathMarker::new, DeathMarker::pos);

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()
                && !player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            player.setData(ModAttachments.DEATH_MARKER, new DeathMarker(Optional.of(GlobalPos.of(player.level().dimension(), player.blockPosition()))));
            player.syncData(ModAttachments.DEATH_MARKER);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_TICKS != 0 || !player.isAlive()) {
            return;
        }
        Optional<GlobalPos> pos = player.getData(ModAttachments.DEATH_MARKER).pos();
        if (pos.isPresent() && pos.get().dimension() == player.level().dimension()
                && player.position().distanceTo(Vec3.atCenterOf(pos.get().pos())) <= ARRIVE_DISTANCE) {
            player.setData(ModAttachments.DEATH_MARKER, NONE);
            player.syncData(ModAttachments.DEATH_MARKER);
        }
    }
}
