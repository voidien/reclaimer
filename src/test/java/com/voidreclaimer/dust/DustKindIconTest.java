package com.voidreclaimer.dust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.dust.DustKind;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DustKindIconTest {
    @Test
    void theIconIsOptionalAndDefaultsToNone() {
        DustKind kind = DustKind.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"color\":16711935}")).getOrThrow();
        assertEquals(Optional.empty(), kind.icon());
    }

    @Test
    void anIconIdDefaultsToOurNamespaceAndRoundTrips() {
        DustKind kind = DustKind.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"color\":1,\"icon\":\"hud/dust_plasma\"}")).getOrThrow();
        assertEquals(Optional.of(VoidReclaimer.id("hud/dust_plasma")), kind.icon());
        DustKind again = DustKind.CODEC.parse(JsonOps.INSTANCE, DustKind.CODEC.encodeStart(JsonOps.INSTANCE, kind).getOrThrow()).getOrThrow();
        assertEquals(kind, again);
    }

    @Test
    void everyBuiltInKindHasItsSprite() throws Exception {
        for (String kind : new String[] {"metallic", "silicate", "ice", "carbon"}) {
            byte[] png;
            try (var in = DustKindIconTest.class.getResourceAsStream("/assets/void_reclaimer/textures/gui/sprites/hud/dust_" + kind + ".png")) {
                assertTrue(in != null, "missing sprite for " + kind);
                png = in.readAllBytes();
            }
            int w = java.nio.ByteBuffer.wrap(png, 16, 4).getInt();
            int h = java.nio.ByteBuffer.wrap(png, 20, 4).getInt();
            assertEquals(9, w, kind + " width");
            assertEquals(9, h, kind + " height");
        }
    }
}
