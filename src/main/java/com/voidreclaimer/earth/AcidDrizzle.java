package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Wear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Acid Drizzle on ruined Earth: once a second a body under the open sky takes a little wear on every section. It goes
 * through {@link Wear}, so it is repairable, never breaks a section and spares creative bodies; it hurts nothing else. Off
 * with the config {@code earth.acidDrizzle}.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class AcidDrizzle {
    private static final int PERIOD = 20;

    private AcidDrizzle() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % PERIOD == 0) {
            second(player);
        }
    }

    /** One second of drizzle on this player, if any falls on them. */
    public static void second(ServerPlayer player) {
        if (!Config.EARTH_ACID_DRIZZLE.get() || !(player.level() instanceof ServerLevel level) || !RuinedEarth.is(level) || !underOpenSky(player)) {
            return;
        }
        float strength = EarthAtmosphere.drizzle(SectorMap.of(level), player.getBlockX(), player.getBlockZ());
        float wear = EarthAtmosphere.wearPerSecond(strength, Config.EARTH_ACID_WEAR.get());
        if (wear > 0f) {
            for (String socket : Bodies.data(player).sections().keySet()) {
                Wear.add(player, socket, wear);
            }
        }
    }

    /** Nothing solid over the top of the body. A heightmap, so a roof counts the moment it is placed. */
    public static boolean underOpenSky(ServerPlayer player) {
        return player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ()) <= Mth.ceil(player.getEyeY());
    }
}
