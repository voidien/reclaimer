package com.voidreclaimer.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.api.body.ModuleKind;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class BodyActionPayloadTest {
    private static BodyActionPayload roundTrip(BodyActionPayload payload) {
        ByteBuf buf = Unpooled.buffer();
        BodyActionPayload.STREAM_CODEC.encode(buf, payload);
        BodyActionPayload decoded = BodyActionPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes(), "bytes left over");
        return decoded;
    }

    @Test
    void useToolCarriesTheSneakFlag() {
        assertTrue(roundTrip(BodyActionPayload.useTool(true)).sneak());
        assertFalse(roundTrip(BodyActionPayload.useTool(false)).sneak());
        assertEquals(BodyActionPayload.Action.USE_TOOL, roundTrip(BodyActionPayload.useTool(true)).action());
    }

    @Test
    void otherActionsRoundTrip() {
        BodyActionPayload payload = BodyActionPayload.installSection("hull", 7);
        assertEquals(payload, roundTrip(payload));
        BodyActionPayload select = BodyActionPayload.selectTool(2);
        assertEquals(select, roundTrip(select));
        assertEquals(ModuleKind.TOOL, roundTrip(select).kind());
    }
}
