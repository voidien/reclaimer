package com.voidreclaimer.dyson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class DysonTiersTest {
    private static final long LATTICE = 10;
    private static final long SHELL = 100;

    private static int tier(long satellites, long trusses) {
        return DysonTiers.tier(satellites, trusses, LATTICE, SHELL);
    }

    @Test
    void satelliteTiersFollowTheDesignRanges() {
        assertEquals(0, tier(0, 0));
        assertEquals(1, tier(1, 0));
        assertEquals(1, tier(50, 0));
        assertEquals(2, tier(51, 0));
        assertEquals(2, tier(100_000, 0));
    }

    @Test
    void trussTiersTakeOverFromTheSwarm() {
        assertEquals(2, tier(500, 9));
        assertEquals(3, tier(500, 10));
        assertEquals(3, tier(0, 99));
        assertEquals(4, tier(0, 100));
        assertEquals(4, tier(500, 5000));
    }

    @Test
    void thresholdsAreClampedToBeSane() {
        // A shell total below the lattice start can never skip tier 3's start.
        assertEquals(4, DysonTiers.tier(0, 5, 5, 1));
        assertEquals(0, DysonTiers.tier(0, 0, 0, 0));
        assertEquals(4, DysonTiers.tier(0, 1, 0, 0));
    }

    @Test
    void densityAndProgressStayInRange() {
        assertEquals(0f, DysonTiers.swarmDensity(0));
        assertEquals(0.5f, DysonTiers.swarmDensity(DysonTiers.SWARM_FULL / 2));
        assertEquals(1f, DysonTiers.swarmDensity(1_000_000));
        assertEquals(0.25f, DysonTiers.shellProgress(25, 100));
        assertEquals(1f, DysonTiers.shellProgress(500, 100));
        assertEquals(0f, DysonTiers.shellProgress(5, 0));
    }

    @Test
    void sunlightOnlyFalls() {
        float last = 1f;
        for (int t = 0; t <= 4; t++) {
            float light = DysonTiers.sunlight(t, 0f);
            assertTrue(light <= last, "tier " + t);
            last = light;
        }
        assertEquals(0.5f, DysonTiers.sunlight(3, 0f), 1.0E-6f);
        assertTrue(DysonTiers.sunlight(3, 0.9f) < DysonTiers.sunlight(3, 0.1f));
        assertEquals(0f, DysonTiers.sunlight(4, 1f));
    }

    @Test
    void dataCodecsRoundTripAndClamp() {
        DysonSphereData data = new DysonSphereData(77, 12, 10, 100);
        assertEquals(data, DysonSphereData.CODEC.parse(JsonOps.INSTANCE, DysonSphereData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow());
        ByteBuf buf = Unpooled.buffer();
        DysonSphereData.STREAM_CODEC.encode(buf, data);
        assertEquals(data, DysonSphereData.STREAM_CODEC.decode(buf));

        DysonSphereData clamped = new DysonSphereData(-5, -1, 0, -3);
        assertEquals(0, clamped.satellites());
        assertEquals(0, clamped.trusses());
        assertEquals(1, clamped.latticeStart());
        assertEquals(1, clamped.shellTotal());
        assertEquals(3, data.tier());
        assertEquals(2, data.withTrusses(9).tier());
    }
}
