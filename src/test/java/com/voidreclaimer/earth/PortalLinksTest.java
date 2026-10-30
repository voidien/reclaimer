package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class PortalLinksTest {
    private static PortalLinks roundTrip(PortalLinks links) {
        var json = PortalLinks.CODEC.encodeStart(JsonOps.INSTANCE, links).getOrThrow();
        return PortalLinks.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    @Test
    void emptyLinksRoundTrip() {
        assertEquals(PortalLinks.NONE, roundTrip(PortalLinks.NONE));
    }

    @Test
    void gateAndExitRoundTrip() {
        PortalLinks links = PortalLinks.NONE
                .withGate(new PortalLinks.Gate(new BlockPos(-6, 65, -10), 12, 6))
                .withExit(new PortalLinks.Exit(Level.OVERWORLD, new Vec3(1.5, 70, -3.25), Direction.EAST));
        assertEquals(links, roundTrip(links));
        assertTrue(roundTrip(links).gate().orElseThrow().rect().axis() == Direction.Axis.X);
    }
}
