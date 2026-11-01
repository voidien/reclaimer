package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Wear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    /**
     * Nothing solid over the top of the body. A heightmap, so a roof counts the moment it is placed, and then a look straight up,
     * because a ship's blocks are in its plot and no heightmap sees them: the owner's rule is that a hull is a roof.
     */
    public static boolean underOpenSky(ServerPlayer player) {
        Level level = player.level();
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ()) > Mth.ceil(player.getEyeY())) {
            return false;
        }
        Vec3 eyes = player.getEyePosition();
        Vec3 top = new Vec3(eyes.x, level.getMaxBuildHeight(), eyes.z);
        return level.clip(new ClipContext(eyes, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}
