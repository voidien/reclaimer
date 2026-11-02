package com.voidreclaimer.earth;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.weather.SpaceWeatherTicker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ruined Earth's bad air, which falls as Acid Drizzle: one hazard, the kind {@code toxic_air} in the shared weather system, which never
 * stops. It wears a body's sections under the open sky, and a roof or a ship's hull keeps it off. It goes through {@link Wear}, so it can be
 * repaired, and it never takes health, but a section worn right out stops working until it is. Off with the config {@code earth.acidDrizzle}.
 */
public final class AcidDrizzle {
    public static final ResourceLocation WEATHER = VoidReclaimer.id("toxic_air");

    private AcidDrizzle() {}

    /** How much of it reaches this player: nothing indoors, and less with every sector restored. */
    public static float exposure(ServerPlayer player) {
        if (!Config.EARTH_ACID_DRIZZLE.get() || !(player.level() instanceof ServerLevel level) || !RuinedEarth.is(level) || !underOpenSky(player)) {
            return 0f;
        }
        return EarthAtmosphere.drizzle(SectorMap.of(level), player.getBlockX(), player.getBlockZ());
    }

    /** One second of it on this player, if any falls on them. */
    public static void second(ServerPlayer player) {
        SpaceWeatherTicker.alwaysOn(player, WEATHER);
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
