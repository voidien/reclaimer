package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EarthAtmosphereBeltTest {
    @Test
    void theBeltThinsAsThePlanetHeals() {
        assertEquals(0.5f, EarthAtmosphere.beltRocks(0f, 0.5), 1e-6f);
        assertEquals(0.25f, EarthAtmosphere.beltRocks(0.5f, 0.5), 1e-6f);
        assertEquals(0f, EarthAtmosphere.beltRocks(1f, 0.5), 1e-6f);
    }
}
