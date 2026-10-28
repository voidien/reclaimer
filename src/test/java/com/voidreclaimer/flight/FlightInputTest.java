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
}
