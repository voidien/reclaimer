package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EarthAtmosphereTest {
    @Test
    void everyRestoredSectorThinsTheDrizzle() {
        SectorMap map = SectorMap.NONE;
        float last = EarthAtmosphere.drizzle(map, 0, 0);
        assertEquals(1f, last);
        for (int sector = 0; sector < Sectors.COUNT; sector++) {
            if (sector == Sectors.at(0, 0)) {
                continue;
            }
            map = map.with(sector);
            float now = EarthAtmosphere.drizzle(map, 0, 0);
            assertTrue(now < last, "sector " + sector);
            last = now;
        }
    }

    @Test
    void restoredGroundIsDryAtOnce() {
        SectorMap one = SectorMap.NONE.with(Sectors.at(0, 0));
        assertEquals(0f, EarthAtmosphere.drizzle(one, 0, 0));
        assertEquals(15 / 16f, EarthAtmosphere.drizzle(one, 300, 300));
    }

    @Test
    void offThePlanetItFallsLikeAnywhereUnrestored() {
        assertEquals(1f, EarthAtmosphere.drizzle(SectorMap.NONE, 900, 900));
    }

    @Test
    void wearIsTheRatePerMinuteOverSixty() {
        assertEquals(0.0001f, EarthAtmosphere.wearPerSecond(1f, 0.006), 1e-7f);
        assertEquals(0.00005f, EarthAtmosphere.wearPerSecond(0.5f, 0.006), 1e-7f);
        assertEquals(0f, EarthAtmosphere.wearPerSecond(0f, 0.006));
    }

    @Test
    void anyDrizzleDrawsADropAndNoneDrawsNone() {
        assertEquals(0, EarthAtmosphere.drops(0f, 14));
        assertEquals(1, EarthAtmosphere.drops(0.01f, 14));
        assertEquals(14, EarthAtmosphere.drops(1f, 14));
        assertEquals(0, EarthAtmosphere.drops(0f, 0));
    }
}
