package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.ToxicAirEvent;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Wear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Ruined Earth's air: once a second it wears every section of a body, under a roof or not, thinner with every sector restored and gone over
 * restored ground. It never takes health, but a section worn right out stops working until it is repaired, so a long stay needs a filter or
 * a way home. Off with the config {@code earth.toxicAir}.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ToxicAir {
    private static final int PERIOD = 20;

    private ToxicAir() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % PERIOD == 0) {
            second(player);
        }
    }

    /** One second of the air on this player, if it reaches them. */
    public static void second(ServerPlayer player) {
        if (!Config.EARTH_TOXIC_AIR.get() || !(player.level() instanceof ServerLevel level) || !RuinedEarth.is(level)) {
            return;
        }
        float strength = EarthAtmosphere.drizzle(SectorMap.of(level), player.getBlockX(), player.getBlockZ());
        float wear = EarthAtmosphere.wearPerSecond(strength, Config.EARTH_TOXIC_WEAR.get());
        if (wear > 0f && !NeoForge.EVENT_BUS.post(new ToxicAirEvent(player)).isCanceled()) {
            for (String socket : Bodies.data(player).sections().keySet()) {
                Wear.add(player, socket, wear);
            }
        }
    }
}
