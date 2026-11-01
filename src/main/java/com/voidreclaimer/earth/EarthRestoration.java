package com.voidreclaimer.earth;

import com.voidreclaimer.api.event.SectorRestoredEvent;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;

/** Server side of the planet's restoration: the one place that changes the {@link SectorMap}; the sky era follows it. */
public final class EarthRestoration {
    private EarthRestoration() {}

    /** The planet's map, from any dimension. */
    public static SectorMap map(MinecraftServer server) {
        ServerLevel earth = server.getLevel(RuinedEarth.DIMENSION);
        return earth == null ? SectorMap.NONE : SectorMap.of(earth);
    }

    /** Marks a sector restored and moves the sky era on. False when it already was, or is not a sector. */
    public static boolean restore(ServerLevel earth, int sector) {
        SectorMap before = SectorMap.of(earth);
        SectorMap after = before.with(sector);
        if (after.equals(before)) {
            return false;
        }
        store(earth, after);
        NeoForge.EVENT_BUS.post(new SectorRestoredEvent(earth, sector, after.count()));
        return true;
    }

    /** Replaces the whole map, for the debug command; nothing on the ground changes and no event fires. */
    public static void set(ServerLevel earth, SectorMap map) {
        store(earth, map);
    }

    private static void store(ServerLevel earth, SectorMap map) {
        earth.setData(ModAttachments.EARTH_SECTORS, map);
        earth.setData(ModAttachments.EARTH_SKY_ERA, SkyEra.forSectors(map.count()));
    }
}
