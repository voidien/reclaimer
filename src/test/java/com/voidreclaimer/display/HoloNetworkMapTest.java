package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HoloNetworkMapTest {
    private static final HoloNetworkMap.Colours COLOURS = new HoloNetworkMap.Colours(1, 2, 3, 4);
    private static final int RADIUS = 6400;

    private static int at(byte[] image, double east, double south) {
        return image[HoloMapImage.pixel(south, RADIUS) * HoloMapImage.SIZE + HoloMapImage.pixel(east, RADIUS)];
    }

    @Test
    void aMapStaysCloseWithNothingFarOff() {
        assertEquals(HoloNetworkMap.SMALLEST_RADIUS, HoloNetworkMap.radius(List.of(), 10.0, 10.0));
        assertEquals(HoloNetworkMap.SMALLEST_RADIUS, HoloNetworkMap.radius(List.of(new HoloNetworkMap.Node(20.0, 10.0, true)), 10.0, 10.0));
    }

    @Test
    void aMapZoomsOutToItsFurthestRelayFromWhereItIs() {
        assertEquals(1100, HoloNetworkMap.radius(List.of(new HoloNetworkMap.Node(1100.0, 0.0, true), new HoloNetworkMap.Node(0.0, -900.0, false)), 100.0, 0.0));
        assertEquals(HoloNetworkMap.LARGEST_RADIUS, HoloNetworkMap.radius(List.of(new HoloNetworkMap.Node(30_000_000.0, 0.0, true)), -30_000_000.0, 0.0));
    }

    @Test
    void relaysAndTheProjectorAreMarked() {
        byte[] image = HoloNetworkMap.draw(List.of(new HoloNetworkMap.Node(4000.0, 0.0, true), new HoloNetworkMap.Node(0.0, 4000.0, false)), 0.0, 0.0, RADIUS, 0.0, COLOURS);
        assertEquals(1, at(image, 0.0, 0.0));
        assertEquals(2, at(image, 4000.0, 0.0));
        assertEquals(3, at(image, 0.0, 4000.0));
        assertEquals(0, at(image, -4000.0, -4000.0));
    }

    @Test
    void aMapIsCentredOnTheProjector() {
        byte[] image = HoloNetworkMap.draw(List.of(new HoloNetworkMap.Node(10_000.0, 500.0, true)), 6000.0, 500.0, RADIUS, 0.0, COLOURS);
        assertEquals(2, at(image, 4000.0, 0.0));
    }

    @Test
    void onlyLitRelaysReachAnything() {
        byte[] image = HoloNetworkMap.draw(List.of(new HoloNetworkMap.Node(4000.0, 0.0, true), new HoloNetworkMap.Node(-4000.0, 0.0, false)), 0.0, 0.0, RADIUS, 1000.0, COLOURS);
        assertEquals(4, at(image, 4000.0, 800.0));
        assertEquals(0, at(image, 4000.0, 1600.0), "past its range");
        assertEquals(0, at(image, -4000.0, 800.0), "a dark relay reaches nothing");
    }

    @Test
    void aLitRelayIsDrawnOverADarkOneInTheSamePlace() {
        var dark = new HoloNetworkMap.Node(4000.0, 0.0, false);
        var lit = new HoloNetworkMap.Node(4000.0, 0.0, true);
        assertEquals(2, at(HoloNetworkMap.draw(List.of(lit, dark), 0.0, 0.0, RADIUS, 0.0, COLOURS), 4000.0, 0.0));
        assertEquals(2, at(HoloNetworkMap.draw(List.of(dark, lit), 0.0, 0.0, RADIUS, 0.0, COLOURS), 4000.0, 0.0));
    }

    @Test
    void aRelayOffTheMapIsLeftOutWithoutBreakingTheEdge() {
        byte[] image = HoloNetworkMap.draw(List.of(new HoloNetworkMap.Node(RADIUS * 2.0, 0.0, true), new HoloNetworkMap.Node(RADIUS - 1.0, RADIUS - 1.0, false)), 0.0, 0.0, RADIUS, 0.0, COLOURS);
        assertEquals(3, at(image, RADIUS - 1.0, RADIUS - 1.0));
        assertTrue(image.length == HoloMapImage.SIZE * HoloMapImage.SIZE);
    }
}
