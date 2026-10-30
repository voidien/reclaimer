package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Server side of the ruined Earth sky: reads and sets the era on the {@code ruined_earth} level. */
public final class EarthSky {
    private EarthSky() {}

    public static SkyEra get(MinecraftServer server) {
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        return earth == null ? SkyEra.RUINED : SkyEra.of(earth);
    }

    /** Sets the era; it is saved with the level and synced to every client in it. False when the dimension is missing. */
    public static boolean set(MinecraftServer server, SkyEra era) {
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        if (earth == null) {
            return false;
        }
        earth.setData(ModAttachments.EARTH_SKY_ERA, era);
        return true;
    }
}
