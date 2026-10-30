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
    void pitchAndRollPickTheRightJets() {
        // A jet under the bow pushing up (nose up) and a jet on the right wing tip pushing up (roll left).
        Thruster bowUp = at(0, 0, -3, 0, 1, 0);      // lever (0,0,-3) x (0,1,0) = (3,0,0): about +X = nose down? right-hand: +X torque
        Thruster wingUp = at(3, 0, 0, 0, 1, 0);      // lever (3,0,0) x (0,1,0) = (0,0,3): about +Z = roll left (forward is -Z)
        List<Thruster> ship = List.of(bowUp, wingUp);
        double[] noseDown = ThrustAllocator.throttles(ship, FRAME, keys("down"));
        double[] noseUp = ThrustAllocator.throttles(ship, FRAME, keys("up"));
        assertTrue(noseDown[0] > 0.99 && noseDown[1] == 0.0 || noseUp[0] > 0.99 && noseUp[1] == 0.0,
                "the bow jet serves exactly one pitch direction: down=" + noseDown[0] + " up=" + noseUp[0]);
        assertEquals(0.0, noseDown[0] * noseUp[0], EPS, "never both");
        double[] rollLeft = ThrustAllocator.throttles(ship, FRAME, keys("Q"));
        double[] rollRight = ThrustAllocator.throttles(ship, FRAME, keys("E"));
        assertEquals(1.0, Math.max(rollLeft[1], rollRight[1]), EPS, "the wing jet rolls one way");
        assertEquals(0.0, rollLeft[1] * rollRight[1], EPS, "never both");
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
}
