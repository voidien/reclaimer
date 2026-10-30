package com.voidreclaimer.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class FlightInputTest {
    @Test
    void axesFromKeys() {
        FlightInput input = FlightInput.of(true, false, true, false, false, true);
        assertEquals(1, input.forwardAxis());
        assertEquals(-1, input.strafeAxis());
        assertEquals(-1, input.verticalAxis());
        assertTrue(input.isThrusting());
    }

    @Test
    void noneIsNotThrusting() {
        assertFalse(FlightInput.NONE.isThrusting());
        assertEquals(FlightInput.NONE, FlightInput.of(false, false, false, false, false, false));
    }

    @Test
    void streamCodecRoundTripsEveryCombination() {
        for (int bits = 0; bits < 64; bits++) {
            FlightInput input = new FlightInput((byte) bits);
            ByteBuf buf = Unpooled.buffer();
            FlightInput.STREAM_CODEC.encode(buf, input);
            assertEquals(input, FlightInput.STREAM_CODEC.decode(buf));
        }
    }

    @Test
    void engagedIsAFlagThatIsNotAKey() {
        FlightInput keys = FlightInput.of(true, false, false, true, true, false);
        FlightInput engaged = keys.withEngaged(true);
        assertTrue(engaged.engaged());
        assertFalse(keys.engaged());
        assertEquals(keys.forwardAxis(), engaged.forwardAxis());
        assertEquals(keys.strafeAxis(), engaged.strafeAxis());
        assertEquals(keys.verticalAxis(), engaged.verticalAxis());
        assertFalse(FlightInput.NONE.withEngaged(true).isThrusting(), "engaging alone is not thrust");
        assertEquals(keys, engaged.withEngaged(false));
        assertEquals(FlightInput.NONE, FlightInput.NONE.withEngaged(true).withEngaged(false));
    }

    @Test
    void streamCodecKeepsTheEngagedFlag() {
        FlightInput input = FlightInput.of(false, true, false, false, false, true).withEngaged(true);
        ByteBuf buf = Unpooled.buffer();
        FlightInput.STREAM_CODEC.encode(buf, input);
        assertTrue(FlightInput.STREAM_CODEC.decode(buf).engaged());
    }

    @Test
    void awalkingBodyDropsBackToWalkingOnlyOnTheGroundWithNoUpThrustHeld() {
        assertTrue(FlightInput.shouldDisengage(true, false), "landed, no Space held");
        assertFalse(FlightInput.shouldDisengage(true, true), "still on the ground but Space is held: taking off");
        assertFalse(FlightInput.shouldDisengage(false, false), "in the air stays engaged");
        assertFalse(FlightInput.shouldDisengage(false, true));
    }

    @Test
    void flightSettingsRoundTripWithTheRcsLockAndDefaultItOnForOldSaves() {
        var settings = com.voidreclaimer.flight.FlightSettings.DEFAULT.withRcsLock(false).withDampener(false);
        ByteBuf buf = Unpooled.buffer();
        com.voidreclaimer.flight.FlightSettings.STREAM_CODEC.encode(buf, settings);
        assertEquals(settings, com.voidreclaimer.flight.FlightSettings.STREAM_CODEC.decode(buf));
        var json = com.voidreclaimer.flight.FlightSettings.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, settings).getOrThrow();
        assertEquals(settings, com.voidreclaimer.flight.FlightSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
        // A save from before the RCS lock existed has no such field: it loads as on.
        var old = com.google.gson.JsonParser.parseString("{\"dampener\": false}");
        assertTrue(com.voidreclaimer.flight.FlightSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, old).getOrThrow().rcsLock());
    }
}
