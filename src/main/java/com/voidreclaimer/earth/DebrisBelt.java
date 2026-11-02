package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.weather.DebrisRock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The band of wreckage around Earth, between {@code earth.beltFloor} and the top of the sky. Portal arrivals come out above it and have to
 * cross it. It thins with every sector restored, and a healed planet has none. It is always there, so it isn't weather.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class DebrisBelt {
    private static final int PERIOD = 20;

    private DebrisBelt() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % PERIOD == 0) {
            second(player);
        }
    }

    /** One second of the belt on this player, if they are in it. */
    public static void second(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !RuinedEarth.is(level) || player.getY() < Config.EARTH_BELT_FLOOR.get()
                || player.isCreative() || player.isSpectator()) {
            return;
        }
        float rocks = EarthAtmosphere.beltRocks(SectorMap.of(level).fraction(), Config.EARTH_BELT_ROCKS.get());
        if (player.getRandom().nextFloat() < rocks) {
            DebrisRock.aimAt(player);
        }
    }
}
