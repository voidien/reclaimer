package com.voidreclaimer.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class StationListTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes()), C = UUID.nameUUIDFromBytes("c".getBytes());
    private static final StationList LIST = new StationList(List.of(
            new StationList.Entry("Alpha", A, new Vec3(0, 0, 0), true),
            new StationList.Entry("Beta", B, new Vec3(50, 0, 0), false),
            new StationList.Entry("Gamma", C, new Vec3(0, 0, 200), true)));

    @Test
    void theNearestStationWithinRangeIsFound() {
        assertEquals("Alpha", LIST.nearest(new Vec3(3, 0, 0), 8).orElseThrow().name());
        assertEquals("Beta", LIST.nearest(new Vec3(45, 0, 0), 8).orElseThrow().name());
        assertTrue(LIST.nearest(new Vec3(25, 0, 0), 8).isEmpty(), "nothing within 8 blocks of the middle");
    }

    @Test
    void markersSkipTheStationYouAreAtAndComeNearestFirst() {
        List<StationList.Entry> marked = LIST.farthestFirstNearest(new Vec3(2, 0, 0), 24, 3);
        assertEquals(List.of("Beta", "Gamma"), marked.stream().map(StationList.Entry::name).toList());
        assertEquals(1, LIST.farthestFirstNearest(new Vec3(2, 0, 0), 24, 1).size(), "the count caps the markers");
    }

    @Test
    void theListRoundTripsThroughItsStreamCodec() {
        ByteBuf buf = Unpooled.buffer();
        StationList.STREAM_CODEC.encode(buf, LIST);
        assertEquals(LIST, StationList.STREAM_CODEC.decode(buf));
    }
}
