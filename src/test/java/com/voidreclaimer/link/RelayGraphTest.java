package com.voidreclaimer.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.link.RelayGraph.Bridge;
import com.voidreclaimer.link.RelayGraph.Node;
import com.voidreclaimer.link.RelayGraph.Place;
import com.voidreclaimer.link.RelayGraph.Reading;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class RelayGraphTest {
    private static final double EXACT = 1e-9;
    private static final double BAND = 0.25;
    private static final ResourceKey<Level> SPACE = dimension("space");
    private static final ResourceKey<Level> EARTH = dimension("earth");
    private static final Node SEAT = space(0, 100);

    private static ResourceKey<Level> dimension(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("void_reclaimer", name));
    }

    private static Place in(ResourceKey<Level> dimension, double x) {
        return new Place(dimension, new Vec3(x, 0, 0));
    }

    private static Node space(double x, double range) {
        return new Node(in(SPACE, x), range);
    }

    private static Node earth(double x, double range) {
        return new Node(in(EARTH, x), range);
    }

    private static RelayGraph graph(Node... relays) {
        return new RelayGraph(List.of(relays), List.of());
    }

    @Test
    void theSeatAloneReachesItsRange() {
        Reading reading = graph().signal(SEAT, in(SPACE, 50), BAND);
        assertTrue(reading.reachable());
        assertEquals(1, reading.strength(), EXACT);
        assertEquals(0, reading.hops());
    }

    @Test
    void pastTheSeatsRangeTheSignalIsLost() {
        assertEquals(Reading.NONE, graph().signal(SEAT, in(SPACE, 101), BAND));
    }

    @Test
    void theVeryEdgeIsReachedAtNoStrength() {
        Reading reading = graph().signal(SEAT, in(SPACE, 100), BAND);
        assertTrue(reading.reachable());
        assertEquals(0, reading.strength(), EXACT);
    }

    @Test
    void strengthFadesAcrossTheStaticBand() {
        assertEquals(0.5, graph().signal(SEAT, in(SPACE, 87.5), BAND).strength(), EXACT);
    }

    @Test
    void theBandIsAnInput() {
        assertEquals(1, graph().signal(SEAT, in(SPACE, 87.5), 0.1).strength(), EXACT);
    }

    @Test
    void aChainCarriesTheSignalPastTheSeat() {
        Reading reading = graph(space(70, 100), space(140, 100)).signal(SEAT, in(SPACE, 210), BAND);
        assertTrue(reading.reachable());
        assertEquals(2, reading.hops());
        assertEquals(1, reading.strength(), EXACT);
    }

    @Test
    void aBrokenChainReachesNothingPastTheGap() {
        assertFalse(graph(space(70, 100), space(300, 100)).signal(SEAT, in(SPACE, 350), BAND).reachable());
    }

    @Test
    void aRelayOutsideEveryRangeIsNoHelpThoughItCoversTheTarget() {
        assertFalse(graph(space(200, 500)).signal(SEAT, in(SPACE, 300), BAND).reachable());
    }

    @Test
    void aHopIsOneWay() {
        // The seat is inside the relay's range, but the relay is outside the seat's: the signal starts at the seat.
        assertFalse(graph(space(150, 500)).signal(SEAT, in(SPACE, 150), BAND).reachable());
    }

    @Test
    void aChainIsAsStrongAsItsWeakestHop() {
        // Seat to relay in the band (strength 0.5), then a clean hop to the target.
        Reading reading = graph(space(87.5, 100)).signal(SEAT, in(SPACE, 137.5), BAND);
        assertEquals(0.5, reading.strength(), EXACT);
        assertEquals(1, reading.hops());
    }

    @Test
    void theStrongestChainWinsEvenWhenItIsLonger() {
        // Straight through one weak relay, or round through two clean ones.
        Node weak = new Node(new Place(SPACE, new Vec3(0, 95, 0)), 300);
        Node cleanA = new Node(new Place(SPACE, new Vec3(50, 0, 0)), 100);
        Node cleanB = new Node(new Place(SPACE, new Vec3(100, 50, 0)), 100);
        Place target = new Place(SPACE, new Vec3(100, 120, 0));
        Reading reading = graph(weak, cleanA, cleanB).signal(SEAT, target, BAND);
        assertEquals(1, reading.strength(), EXACT);
        assertEquals(2, reading.hops());
    }

    @Test
    void aWeakHopEarlyInAChainWeakensEverythingAfterIt() {
        // Weak from the seat to the first relay, then two clean hops.
        Reading reading = graph(space(87.5, 100), space(150, 100)).signal(SEAT, in(SPACE, 210), BAND);
        assertEquals(0.5, reading.strength(), EXACT);
        assertEquals(2, reading.hops());
    }

    @Test
    void aRelayCleansUpWhatTheSeatOnlyReachesThroughStatic() {
        Reading reading = graph(space(50, 100)).signal(SEAT, in(SPACE, 87.5), BAND);
        assertEquals(1, reading.strength(), EXACT);
        assertEquals(1, reading.hops());
    }

    @Test
    void aFarNodesOwnRangeSetsItsReach() {
        assertFalse(graph(space(90, 10)).signal(SEAT, in(SPACE, 150), BAND).reachable());
        assertTrue(graph(space(50, 200)).signal(SEAT, in(SPACE, 240), BAND).reachable());
    }

    @Test
    void aNodeInAnotherDimensionPassesNothing() {
        assertFalse(graph(earth(50, 1000)).signal(SEAT, in(SPACE, 150), BAND).reachable());
    }

    @Test
    void anotherDimensionIsOutOfReachWithoutABridge() {
        assertFalse(graph().signal(SEAT, in(EARTH, 10), BAND).reachable());
    }

    @Test
    void aBridgeCarriesTheSignalAcrossDimensions() {
        RelayGraph graph = new RelayGraph(List.of(), List.of(new Bridge(space(50, 10), earth(5000, 100))));
        Reading reading = graph.signal(SEAT, in(EARTH, 5050), BAND);
        assertTrue(reading.reachable());
        assertEquals(1, reading.strength(), EXACT);
        assertEquals(2, reading.hops());
    }

    @Test
    void aBridgeWorksBothWays() {
        RelayGraph graph = new RelayGraph(List.of(), List.of(new Bridge(earth(5000, 100), space(50, 10))));
        assertTrue(graph.signal(SEAT, in(EARTH, 5050), BAND).reachable());
        Node seatOnEarth = earth(5000, 100);
        assertTrue(graph.signal(seatOnEarth, in(SPACE, 55), BAND).reachable());
    }

    @Test
    void aBridgeOutOfRangeBridgesNothing() {
        RelayGraph graph = new RelayGraph(List.of(), List.of(new Bridge(space(500, 10), earth(5000, 100))));
        assertFalse(graph.signal(SEAT, in(EARTH, 5050), BAND).reachable());
    }

    @Test
    void aBridgeLosesNothingButKeepsTheChainsWeakness() {
        RelayGraph graph = new RelayGraph(List.of(), List.of(new Bridge(space(87.5, 10), earth(5000, 100))));
        assertEquals(0.5, graph.signal(SEAT, in(EARTH, 5010), BAND).strength(), EXACT);
    }

    @Test
    void relaysAfterABridgeExtendTheFarNetwork() {
        RelayGraph graph = new RelayGraph(List.of(earth(5090, 100)), List.of(new Bridge(space(50, 10), earth(5000, 100))));
        Reading reading = graph.signal(SEAT, in(EARTH, 5180), BAND);
        assertTrue(reading.reachable());
        assertEquals(3, reading.hops());
    }

    @Test
    void twoBridgesKeepTheirOwnEnds() {
        ResourceKey<Level> third = dimension("third");
        RelayGraph graph = new RelayGraph(List.of(), List.of(
                new Bridge(space(5000, 10), earth(0, 10)),
                new Bridge(space(50, 10), new Node(in(third, 0), 10))));
        assertTrue(graph.signal(SEAT, in(third, 5), BAND).reachable());
        assertFalse(graph.signal(SEAT, in(EARTH, 5), BAND).reachable());
    }

    @Test
    void aLongChainIsFollowedToItsEnd() {
        List<Node> relays = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            relays.add(space(i * 70.0, 100));
        }
        Reading reading = new RelayGraph(relays, List.of()).signal(SEAT, in(SPACE, 500 * 70.0 + 50), BAND);
        assertEquals(500, reading.hops());
        assertEquals(1, reading.strength(), EXACT);
    }
}
