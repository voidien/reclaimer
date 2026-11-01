package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.ship.ThrustAllocator.Frame;
import com.voidreclaimer.ship.ThrustAllocator.Thruster;
import java.util.List;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ThrustAllocatorTest {
    private static final double EPS = 1e-9;
    /** A ship facing -Z (north), right is +X... no: right of a ship facing north is east, +X. Up is +Y. */
    private static final Frame FRAME = new Frame(new Vector3d(0, 0, -1), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 0));

    private static Thruster at(double x, double y, double z, double px, double py, double pz) {
        return new Thruster(new Vector3d(x, y, z), ThrustAllocator.unit(px, py, pz), 100);
    }

    private static PilotInput keys(String held) {
        return PilotInput.of(held.contains("W"), held.contains("S"), held.contains("D"), held.contains("A"), held.contains("space"),
                held.contains("shift"), held.contains("E"), held.contains("Q"), held.contains("up"), held.contains("down"),
                held.contains("left"), held.contains("right"));
    }

    @Test
    void nothingHeldOpensNothing() {
        List<Thruster> ship = List.of(at(0, 0, 2, 0, 0, -1), at(0, 0, -2, 0, 0, 1));
        assertArrayEquals(new double[] {0, 0}, ThrustAllocator.throttles(ship, FRAME, PilotInput.NONE));
    }

    @Test
    void forwardOpensTheRearThrusterOnly() {
        // One thruster at the stern pushing forward (-Z), one at the bow pushing back.
        List<Thruster> ship = List.of(at(0, 0, 2, 0, 0, -1), at(0, 0, -2, 0, 0, 1));
        assertArrayEquals(new double[] {1, 0}, ThrustAllocator.throttles(ship, FRAME, keys("W")), EPS);
        assertArrayEquals(new double[] {0, 1}, ThrustAllocator.throttles(ship, FRAME, keys("S")), EPS);
    }

    @Test
    void aThrusterAtAnAngleOpensInProportion() {
        // Pushing forward-and-right at 45 degrees: half-useful for forward, and W+D together use it fully.
        List<Thruster> ship = List.of(at(0, 0, 0, 1, 0, -1));
        assertEquals(Math.sqrt(0.5), ThrustAllocator.throttles(ship, FRAME, keys("W"))[0], EPS);
        assertEquals(1.0, ThrustAllocator.throttles(ship, FRAME, keys("W D"))[0], EPS);
        assertEquals(0.0, ThrustAllocator.throttles(ship, FRAME, keys("A"))[0], EPS, "it would push the wrong way");
    }

    @Test
    void yawUsesTheSideJetsAtTheEnds() {
        // Two RCS jets at the bow (z=-3): one pushes right, one pushes left. Yaw left = torque about +Y.
        // Lever (0,0,-3) x push (1,0,0) = (0*0-(-3)*0, (-3)*1-0*0, 0) = (0,-3,0): torque about -Y, that is yaw right.
        Thruster bowRight = at(0, 0, -3, 1, 0, 0);
        Thruster bowLeft = at(0, 0, -3, -1, 0, 0);
        List<Thruster> ship = List.of(bowRight, bowLeft);
        assertArrayEquals(new double[] {0, 1}, ThrustAllocator.throttles(ship, FRAME, keys("left")), EPS);
        assertArrayEquals(new double[] {1, 0}, ThrustAllocator.throttles(ship, FRAME, keys("right")), EPS);
    }

    @Test
    void pitchAndRollOpenTheJetsThatTurnTheShipThatWay() {
        // Pushing the bow up raises the nose, whatever the signs say. Pushing the right wing up rolls left.
        Thruster bowUp = at(0, 0, -3, 0, 1, 0);
        Thruster wingUp = at(3, 0, 0, 0, 1, 0);
        List<Thruster> ship = List.of(bowUp, wingUp);
        assertArrayEquals(new double[] {1, 0}, ThrustAllocator.throttles(ship, FRAME, keys("up")), EPS, "arrow up opens the jet under the bow");
        assertArrayEquals(new double[] {0, 0}, ThrustAllocator.throttles(ship, FRAME, keys("down")), EPS, "nothing here pushes the bow down");
        assertArrayEquals(new double[] {0, 1}, ThrustAllocator.throttles(ship, FRAME, keys("Q")), EPS, "Q opens the jet under the right wing");
        assertArrayEquals(new double[] {0, 0}, ThrustAllocator.throttles(ship, FRAME, keys("E")), EPS, "nothing here rolls right");
    }

    @Test
    void theHudSaysTheTurnsTheShipCanReallyMake() {
        double[] coverage = ThrustAllocator.rotationCoverage(List.of(at(0, 0, -3, 0, 1, 0), at(3, 0, 0, 0, 1, 0)), FRAME);
        assertEquals(1.0, coverage[1], EPS, "roll left");
        assertEquals(0.0, coverage[0], EPS, "roll right");
        assertEquals(1.0, coverage[2], EPS, "nose up");
        assertEquals(0.0, coverage[3], EPS, "nose down");
    }

    @Test
    void theCentreOfMassMattersForTurning() {
        // The same jet turns the other way when the centre of mass is on the other side of it.
        Thruster jet = at(0, 0, -3, 1, 0, 0);
        Frame behind = new Frame(FRAME.forward(), FRAME.right(), FRAME.up(), new Vector3d(0, 0, 3));
        Frame ahead = new Frame(FRAME.forward(), FRAME.right(), FRAME.up(), new Vector3d(0, 0, -6));
        boolean turnsRightWhenBehind = ThrustAllocator.throttles(List.of(jet), behind, keys("right"))[0] > 0;
        boolean turnsRightWhenAhead = ThrustAllocator.throttles(List.of(jet), ahead, keys("right"))[0] > 0;
        assertTrue(turnsRightWhenBehind != turnsRightWhenAhead);
    }

    @Test
    void coverageReportsWhatTheShipCannotDo() {
        List<Thruster> ship = List.of(at(0, 0, 2, 0, 0, -1));  // only a main engine
        double[] move = ThrustAllocator.translationCoverage(ship, FRAME);
        assertEquals(1.0, move[0], EPS, "forward");
        assertEquals(0.0, move[1], EPS, "no reverse");
        assertEquals(0.0, move[2] + move[3] + move[4] + move[5], EPS, "no strafe or lift");
        double[] turn = ThrustAllocator.rotationCoverage(ship, FRAME);
        assertEquals(0.0, turn[0] + turn[1] + turn[2] + turn[3] + turn[4] + turn[5], EPS, "a centred engine turns nothing");
    }

    @Test
    void inputCodecRoundTrips() {
        for (int bits = 0; bits < 1 << 12; bits += 37) {
            PilotInput in = new PilotInput((short) bits);
            io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
            PilotInput.STREAM_CODEC.encode(buf, in);
            assertEquals(in, PilotInput.STREAM_CODEC.decode(buf));
        }
        PilotInput all = keys("W D space E up left");
        assertEquals(1, all.surge());
        assertEquals(1, all.sway());
        assertEquals(1, all.heave());
        assertEquals(1, all.roll());
        assertEquals(1, all.pitch());
        assertEquals(1, all.yaw());
        assertEquals(-1, keys("S A shift Q down right").yaw());
    }

    /** JOML's equals is exact, and a cross product can give -0.0: compare within a tolerance. */
    private static void assertVec(double x, double y, double z, Vector3d actual, String what) {
        assertTrue(actual.distance(x, y, z) < EPS, what + ": expected (" + x + ", " + y + ", " + z + ") but was " + actual);
    }

    @Test
    void theHelmsAxesFollowTheChair() {
        Vector3d[] north = HelmBlockEntity.axes(net.minecraft.core.Direction.NORTH);
        assertVec(0, 0, -1, north[0], "forward");
        assertVec(1, 0, 0, north[1], "right of north is east");
        assertVec(0, 1, 0, north[2], "up");
        assertVec(0, 0, 1, HelmBlockEntity.axes(net.minecraft.core.Direction.EAST)[1], "right of east is south (+Z)");
        assertVec(-1, 0, 0, HelmBlockEntity.axes(net.minecraft.core.Direction.SOUTH)[1], "right of south is west");
        assertVec(0, 0, -1, HelmBlockEntity.axes(net.minecraft.core.Direction.WEST)[1], "right of west is north");
    }

    @Test
    void swayOpensTheJetOnThePilotsRight() {
        // Chair north. A jet on the keel pushing east: D (sway right) opens it, A does not.
        Vector3d[] a = HelmBlockEntity.axes(net.minecraft.core.Direction.NORTH);
        Frame frame = new Frame(a[0], a[1], a[2], new Vector3d(0.5, 0.5, 0.5));
        Thruster eastJet = new Thruster(new Vector3d(0.5, 0.5, 0.5), new Vector3d(1, 0, 0), 40);
        assertEquals(1.0, ThrustAllocator.throttles(List.of(eastJet), frame, keys("D"))[0], EPS);
        assertEquals(0.0, ThrustAllocator.throttles(List.of(eastJet), frame, keys("A"))[0], EPS);
    }

    @Test
    void theTestShipsBowJetYawsRight() {
        // The Aeronautics test ship: chair north, an RCS jet 4 blocks ahead of the Helm pushing east (facing west).
        Vector3d[] a = HelmBlockEntity.axes(net.minecraft.core.Direction.NORTH);
        Frame frame = new Frame(a[0], a[1], a[2], new Vector3d(0.5, 0.5, 0.5));
        Thruster bowJet = new Thruster(new Vector3d(0.5, 0.5, -3.5), new Vector3d(1, 0, 0), 40);
        assertEquals(1.0, ThrustAllocator.throttles(List.of(bowJet), frame, keys("right"))[0], EPS, "yaw right opens it");
        assertEquals(0.0, ThrustAllocator.throttles(List.of(bowJet), frame, keys("left"))[0], EPS, "yaw left would turn the wrong way");
    }

    @Test
    void theDampenerAsksForTheOppositeOfTheVelocityWhereNoKeyIsHeld() {
        // Drifting forward (-Z) and up: ask for back and down. The pilot holds nothing.
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(0, 1, -2), new Vector3d(), true, true, 0.05, 1.0);
        assertEquals(new Vector3d(0, -1, 1), hold[0], "force against the drift");
        assertEquals(new Vector3d(0, 0, 0), hold[1], "no spin, no torque");
    }

    @Test
    void aHeldKeyOwnsItsAxisSoTheHoldLeavesItAlone() {
        // Drifting forward while holding W: no braking along forward, but a sideways drift is still countered.
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, keys("W"), new Vector3d(1, 0, -2), new Vector3d(), true, true, 0.05, 1.0);
        assertEquals(new Vector3d(-1, 0, 0), hold[0]);
    }

    @Test
    void theRcsLockCountersSpinAndTheDeadbandIgnoresACrawl() {
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(), new Vector3d(0, 2, 0), true, true, 0.05, 1.0);
        assertEquals(new Vector3d(0, -1, 0), hold[1], "torque against a yaw spin");
        // Below the ramp the hold eases off: half the ramp spin asks for half the torque, and the throttle follows.
        Vector3d[] gentle = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(), new Vector3d(0, 0.5, 0), true, true, 0.05, 1.0);
        assertEquals(new Vector3d(0, -0.5, 0), gentle[1], "half the ramp, half the torque");
        List<Thruster> bowJet = List.of(at(0, 0, -2, 1, 0, 0));
        assertArrayEquals(new double[] {0.5}, ThrustAllocator.throttles(bowJet, FRAME, gentle[0], gentle[1]), EPS);
        Vector3d[] still = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(0.01, 0, 0), new Vector3d(0, 0.01, 0), true, true, 0.05, 1.0);
        assertEquals(new Vector3d(), still[0]);
        assertEquals(new Vector3d(), still[1]);
    }

    @Test
    void holdsOffAskForNothing() {
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(0, 0, -2), new Vector3d(0, 1, 0), false, false, 0.05, 1.0);
        assertEquals(new Vector3d(), hold[0]);
        assertEquals(new Vector3d(), hold[1]);
    }

    @Test
    void aHoldRequestOpensTheThrusterThatBrakes() {
        // Drifting forward (-Z); the stern thruster pushes forward, the bow thruster pushes back: braking opens the bow one.
        List<Thruster> ship = List.of(at(0, 0, 2, 0, 0, -1), at(0, 0, -2, 0, 0, 1));
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(0, 0, -2), new Vector3d(), true, false, 0.05, 1.0);
        assertArrayEquals(new double[] {0, 1}, ThrustAllocator.throttles(ship, FRAME, hold[0], hold[1]), EPS);
    }

    @Test
    void aJetThatBrakesTheDriftButFeedsTheSpinStaysShut() {
        // Drifting right (+X) and spinning +Y. Four side jets: at the bow (-Z) and the stern (+Z), pushing west (-X) and east.
        // A bow jet pushing west brakes the drift but its torque is +Y, the spin's own way; the stern jet pushing west
        // brakes the drift and turns -Y: only the stern one opens.
        List<Thruster> ship = List.of(at(1, 0, -2, -1, 0, 0), at(-1, 0, -2, 1, 0, 0), at(1, 0, 2, -1, 0, 0), at(-1, 0, 2, 1, 0, 0));
        Vector3d[] hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(2, 0, 0), new Vector3d(0, 1, 0), true, true, 0.05, 1.0);
        assertArrayEquals(new double[] {0, 0, 1, 0}, ThrustAllocator.throttles(ship, FRAME, hold[0], hold[1]), EPS);
        // Once the drift is gone, the pure yaw pair opens: bow east and stern west.
        hold = ThrustAllocator.holdRequest(FRAME, PilotInput.NONE, new Vector3d(), new Vector3d(0, 1, 0), true, true, 0.05, 1.0);
        assertArrayEquals(new double[] {0, 1, 1, 0}, ThrustAllocator.throttles(ship, FRAME, hold[0], hold[1]), EPS);
    }
}
