package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EarthWindTest {
    private static final double EPS = 1.0E-9;

    @Test
    void theWindIsHorizontalAndNeverFasterThanItsTopSpeed() {
        double top = 0.0;
        for (int i = 0; i < 2000; i++) {
            Vec3 wind = EarthWind.field(i * 37.0, i * -53.0, i * 17L, 1.5);
            assertEquals(0.0, wind.y, EPS);
            top = Math.max(top, wind.length());
            assertTrue(wind.length() <= 1.5 + EPS, "faster than the top speed: " + wind);
        }
        assertTrue(top > 1.0, "the wind never gets near its top speed: " + top);
    }

    @Test
    void noTopSpeedMeansNoWind() {
        assertEquals(Vec3.ZERO, EarthWind.field(100, 200, 300L, 0.0));
        assertEquals(Vec3.ZERO, EarthWind.field(100, 200, 300L, -1.0));
    }

    @Test
    void theWindShiftsWithPlaceAndTime() {
        Vec3 here = EarthWind.field(0, 0, 0L, 1.0);
        assertNotEquals(here, EarthWind.field(EarthWind.CELL * 1.5, 0, 0L, 1.0), "the same wind a cell and a half away");
        assertNotEquals(here, EarthWind.field(0, 0, (long) (EarthWind.PERIOD / 4), 1.0), "the same wind a quarter period later");
        // The field is a plain function of place and time, so the client and the server agree.
        assertEquals(here, EarthWind.field(0, 0, 0L, 1.0));
    }

    @Test
    void theWindScalesWithItsTopSpeed() {
        Vec3 slow = EarthWind.field(321, -45, 999L, 1.0);
        Vec3 fast = EarthWind.field(321, -45, 999L, 3.0);
        assertEquals(slow.x * 3, fast.x, EPS);
        assertEquals(slow.z * 3, fast.z, EPS);
    }
}
