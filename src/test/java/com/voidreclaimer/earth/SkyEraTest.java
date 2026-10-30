package com.voidreclaimer.earth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class SkyEraTest {
    @Test
    void codecRoundTripsEveryEra() {
        for (SkyEra era : SkyEra.values()) {
            var json = SkyEra.CODEC.encodeStart(JsonOps.INSTANCE, era).getOrThrow();
            assertEquals(era, SkyEra.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }

    @Test
    void streamCodecRoundTripsEveryEra() {
        for (SkyEra era : SkyEra.values()) {
            var buffer = Unpooled.buffer();
            SkyEra.STREAM_CODEC.encode(buffer, era);
            assertEquals(era, SkyEra.STREAM_CODEC.decode(buffer));
        }
    }

    @Test
    void erasLookDifferentAndClearUpInOrder() {
        SkyLook ruined = SkyLook.of(SkyEra.RUINED);
        SkyLook healing = SkyLook.of(SkyEra.HEALING);
        SkyLook restored = SkyLook.of(SkyEra.RESTORED);
        assertNotEquals(ruined, healing);
        assertNotEquals(healing, restored);
        assertTrue(ruined.fogFar() < healing.fogFar() && healing.fogFar() < restored.fogFar(), "smog should thin out");
        assertTrue(ruined.sunAlpha() < restored.sunAlpha(), "the sun should clear up");
        assertFalse(ruined.clouds());
        assertTrue(healing.clouds() && restored.clouds());
    }

    @Test
    void mixBlendsColors() {
        assertEquals(0x000000, SkyLook.mix(0x000000, 0xFFFFFF, 0f));
        assertEquals(0xFFFFFF, SkyLook.mix(0x000000, 0xFFFFFF, 1f));
        assertEquals(0x808080, SkyLook.mix(0x000000, 0xFFFFFF, 0.5f));
    }
}
