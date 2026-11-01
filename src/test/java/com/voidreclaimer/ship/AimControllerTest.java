package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.ship.AimController.Aimed;
import com.voidreclaimer.ship.AimController.Authority;
import com.voidreclaimer.ship.AimController.Direct;
import com.voidreclaimer.ship.AimController.Request;
import com.voidreclaimer.ship.ThrustAllocator.Frame;
import com.voidreclaimer.ship.ThrustAllocator.Thruster;
import java.util.List;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Test;

class AimControllerTest {
    private static final double EPS = 1e-9;
    /** A ship facing -Z: right is +X, up is +Y. */
    private static final Frame FRAME = new Frame(new Vector3d(0, 0, -1), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d());
    private static final Authority STRONG = new Authority(4, 4, 4);
    private static final Vector3d AHEAD = new Vector3d(0, 0, -1);
    private static final Vector3d STILL = new Vector3d();

    private static Thruster jet(double x, double y, double z, double px, double py, double pz) {
        return new Thruster(new Vector3d(x, y, z), ThrustAllocator.unit(px, py, pz), 100);
    }

    private static PilotInput keys(String held) {
        return PilotInput.of(false, false, false, false, false, false, held.contains("E"), held.contains("Q"), held.contains("up"),
                held.contains("down"), held.contains("left"), held.contains("right"));
    }

    private static Request mouse(double right, double up, Vector3dc spin, Authority authority) {
        return new Request(PilotInput.NONE, right, up, AHEAD, spin, true, authority);
    }

    private static Request looking(Vector3dc look, Vector3dc spin, Authority authority) {
        return new Request(PilotInput.NONE, 0, 0, look, spin, true, authority);
    }

    private static double[] opened(Vector3dc torque, Thruster... jets) {
        return ThrustAllocator.throttles(List.of(jets), FRAME, new Vector3d(), torque);
    }

    /** A ship in free space: a request acts on the tick after it, as it does through the Helm. */
    private static final class Sim {
        final Quaterniond orientation = new Quaterniond();
        final Vector3d spin = new Vector3d();
        Vector3d asked = new Vector3d();

        void step(AimController controller, Vector3dc lookInWorld, double mouseRight, double mouseUp, Authority authority) {
            spin.add(asked.x * authority.pitch() * AimController.TICK_SECONDS, asked.y * authority.yaw() * AimController.TICK_SECONDS,
                    asked.z * -authority.roll() * AimController.TICK_SECONDS);
            orientation.integrate(AimController.TICK_SECONDS, spin.x, spin.y, spin.z);
            Vector3d look = new Quaterniond(orientation).conjugate().transform(new Vector3d(lookInWorld));
            asked = controller.torque(FRAME, new Request(PilotInput.NONE, mouseRight, mouseUp, look.normalize(), spin, true, authority));
            assertTrue(Math.abs(asked.x) <= 1 + EPS && Math.abs(asked.y) <= 1 + EPS && Math.abs(asked.z) <= 1 + EPS, "more than the thrusters have: " + asked);
        }

        Vector3d nose() {
            return orientation.transform(new Vector3d(AHEAD));
        }
    }

    private static Vector3d yawed(double degrees) {
        double radians = Math.toRadians(degrees);
        return new Vector3d(-Math.sin(radians), 0, -Math.cos(radians));
    }

    @Test
    void movingTheMouseUpFiresTheJetThatRaisesTheNose() {
        Vector3d torque = Direct.DEFAULT.torque(FRAME, mouse(0, 2, STILL, STRONG));
        double[] throttles = opened(torque, jet(0, 0, -3, 0, 1, 0), jet(0, 0, -3, 0, -1, 0));
        assertTrue(throttles[0] > 0.0, "the jet under the bow stayed shut");
        assertEquals(0.0, throttles[1], EPS);
    }

    @Test
    void movingTheMouseRightFiresTheJetThatTurnsRight() {
        Vector3d torque = Direct.DEFAULT.torque(FRAME, mouse(2, 0, STILL, STRONG));
        double[] throttles = opened(torque, jet(0, 0, -3, 1, 0, 0), jet(0, 0, -3, -1, 0, 0));
        assertTrue(throttles[0] > 0.0, "the jet pushing the bow right stayed shut");
        assertEquals(0.0, throttles[1], EPS);
    }

    @Test
    void theRollKeyFiresTheJetThatRollsThatWay() {
        Vector3d torque = Direct.DEFAULT.torque(FRAME, new Request(keys("E"), 0, 0, AHEAD, STILL, true, STRONG));
        double[] throttles = opened(torque, jet(3, 0, 0, 0, -1, 0), jet(3, 0, 0, 0, 1, 0));
        assertTrue(throttles[0] > 0.0, "the jet pushing the right wing down stayed shut");
        assertEquals(0.0, throttles[1], EPS);
    }

    @Test
    void aStillMouseAsksTheShipToStop() {
        Vector3d spin = new Vector3d(0.1, -0.1, 0);
        Vector3d torque = Direct.DEFAULT.torque(FRAME, mouse(0, 0, spin, STRONG));
        assertEquals(-0.5, torque.x, EPS);
        assertEquals(0.5, torque.y, EPS);
    }

    @Test
    void aTurnEndsSoonAfterTheMouseStops() {
        Sim sim = new Sim();
        for (int i = 0; i < 20; i++) {
            sim.step(Direct.DEFAULT, AHEAD, 2.0, 0, STRONG);
        }
        assertEquals(Math.toRadians(40.0), -sim.spin.y, 0.01, "the ship follows the mouse's speed");
        for (int i = 0; i < 12; i++) {
            sim.step(Direct.DEFAULT, AHEAD, 0, 0, STRONG);
        }
        assertEquals(0.0, sim.spin.length(), 1e-6);
    }

    @Test
    void theTurnIsCappedAtMaxRate() {
        Sim sim = new Sim();
        for (int i = 0; i < 100; i++) {
            sim.step(new Direct(1.0, 0.5, 2.0), AHEAD, 90.0, 0, STRONG);
        }
        assertEquals(0.5, -sim.spin.y, 1e-6);
    }

    @Test
    void aWeakShipTurnsNoFasterThanItCanStop() {
        Authority weak = new Authority(0.1, 0.1, 0.1);
        Sim sim = new Sim();
        for (int i = 0; i < 200; i++) {
            sim.step(Direct.DEFAULT, AHEAD, 90.0, 0, weak);
        }
        assertEquals(0.2, -sim.spin.y, 1e-6, "0.1 radians per second squared stops 0.2 in the 2 seconds allowed");
        for (int i = 0; i < 41; i++) {
            sim.step(Direct.DEFAULT, AHEAD, 0, 0, weak);
        }
        assertEquals(0.0, sim.spin.length(), 1e-6);
    }

    @Test
    void aKeyBeatsTheMouseOnItsAxis() {
        for (AimController controller : new AimController[] {Direct.DEFAULT, Aimed.DEFAULT}) {
            Request request = new Request(keys("left"), 5, 0, yawed(-90), STILL, true, STRONG);
            assertEquals(1.0, controller.torque(FRAME, request).y, EPS);
            Request nose = new Request(keys("down"), 0, 5, new Vector3d(0, 1, 0), STILL, true, STRONG);
            assertEquals(-1.0, controller.torque(FRAME, nose).x, EPS);
        }
    }

    @Test
    void rollStopsOnlyWithTheLockOn() {
        Vector3d spin = new Vector3d(0, 0, -0.1);
        for (AimController controller : new AimController[] {Direct.DEFAULT, Aimed.DEFAULT}) {
            assertEquals(0.5, controller.torque(FRAME, new Request(PilotInput.NONE, 0, 0, AHEAD, spin, true, STRONG)).z, EPS);
            assertEquals(0.0, controller.torque(FRAME, new Request(PilotInput.NONE, 0, 0, AHEAD, spin, false, STRONG)).z, EPS);
        }
    }

    @Test
    void noThrustersAskNothing() {
        Authority none = new Authority(0, 0, 0);
        for (AimController controller : new AimController[] {Direct.DEFAULT, Aimed.DEFAULT}) {
            assertEquals(0.0, controller.torque(FRAME, new Request(PilotInput.NONE, 5, 5, yawed(90), new Vector3d(0.1, 0.1, 0.1), true, none)).length(), EPS);
        }
    }

    @Test
    void lookingDeadAheadOnAStillShipAsksNothing() {
        assertEquals(0.0, Aimed.DEFAULT.torque(FRAME, looking(AHEAD, STILL, STRONG)).length(), EPS);
    }

    @Test
    void lookingDeadAsternStillTurns() {
        assertTrue(Math.abs(Aimed.DEFAULT.torque(FRAME, looking(new Vector3d(0, 0, 1), STILL, STRONG)).y) > 0.5);
    }

    @Test
    void theNoseSwingsTowardTheLook() {
        assertTrue(Aimed.DEFAULT.torque(FRAME, looking(yawed(-30), STILL, STRONG)).y < 0.0, "a look to the right turns right");
        assertTrue(Aimed.DEFAULT.torque(FRAME, looking(new Vector3d(0, 1, -1).normalize(), STILL, STRONG)).x > 0.0, "a look up raises the nose");
    }

    @Test
    void theAimSettlesOnTheLookWithoutOvershootingWhateverTheShip() {
        for (double strength : new double[] {0.2, 1, 5, 40, 400}) {
            Authority authority = new Authority(strength, strength, strength);
            for (double degrees : new double[] {2, 30, 90, 170}) {
                Sim sim = new Sim();
                Vector3d look = yawed(degrees);
                double furthest = -1.0;
                for (int i = 0; i < 3000; i++) {
                    sim.step(Aimed.DEFAULT, look, 0, 0, authority);
                    Vector3d nose = sim.nose();
                    furthest = Math.max(furthest, Math.toDegrees(Math.atan2(-nose.x, -nose.z)) - degrees);
                }
                String ship = strength + " radians per second squared, " + degrees + " degrees: ";
                assertTrue(furthest < 0.25, ship + "overshot by " + furthest);
                assertTrue(sim.nose().angle(look) < Math.toRadians(0.5), ship + "stopped " + Math.toDegrees(sim.nose().angle(look)) + " degrees off");
                assertTrue(sim.spin.length() < 0.01, ship + "still turning at " + sim.spin.length());
            }
        }
    }

    @Test
    void theAimEasesOverTheLastOfTheWay() {
        double radians = 0.1;
        Authority strong = new Authority(400, 400, 400);
        Vector3d spin = new Vector3d(0, -Aimed.DEFAULT.settle() * radians, 0);
        Vector3d torque = Aimed.DEFAULT.torque(FRAME, looking(yawed(-Math.toDegrees(radians)), spin, strong));
        assertEquals(0.0, torque.y, 1e-6, "turning at settle times what is left is already enough");
    }

    @Test
    void theAimReachesALookOnTwoAxes() {
        Vector3d look = new Vector3d(-0.6, 0.5, -0.4).normalize();
        Sim sim = new Sim();
        for (int i = 0; i < 400; i++) {
            sim.step(Aimed.DEFAULT, look, 0, 0, STRONG);
        }
        assertTrue(sim.nose().angle(look) < Math.toRadians(0.5), "stopped " + Math.toDegrees(sim.nose().angle(look)) + " degrees off");
    }

    @Test
    void aimedTurnsNoFasterThanMaxRate() {
        Sim sim = new Sim();
        double fastest = 0.0;
        for (int i = 0; i < 200; i++) {
            sim.step(new Aimed(0.5, 3.0), yawed(170), 0, 0, new Authority(400, 400, 400));
            fastest = Math.max(fastest, sim.spin.length());
        }
        assertTrue(fastest <= 0.5 + 1e-6, "turned at " + fastest);
    }

    @Test
    void theStoppableRateReallyStopsInTime() {
        for (double authority : new double[] {0.2, 1, 5, 40}) {
            for (double radians : new double[] {0.01, 0.2, 1, 3}) {
                double rate = AimController.stoppableRate(radians, authority);
                double travelled = rate * AimController.TICK_SECONDS;
                double spin = rate;
                while (spin > 0.0) {
                    spin = Math.max(0.0, spin - authority * AimController.TICK_SECONDS);
                    travelled += spin * AimController.TICK_SECONDS;
                }
                assertTrue(travelled <= radians + 1e-9, "went " + travelled + " of " + radians);
                assertTrue(AimController.stoppableRate(radians * 2, authority) > rate, "more room allows more speed");
            }
        }
        assertEquals(0.0, AimController.stoppableRate(1, 0));
        assertEquals(0.0, AimController.stoppableRate(0, 5), EPS);
    }

    @Test
    void authorityIsTheWeakerWayRoundOverTheInertia() {
        List<Thruster> ship = List.of(jet(0, 0, -3, 0, 1, 0), jet(0, 0, -3, 0, -1, 0), jet(0, 0, 3, 0, 1, 0));
        Matrix3d inverseInertia = new Matrix3d().scaling(0.01, 0.02, 0.04);
        Authority authority = Authority.of(ship, FRAME, inverseInertia);
        // Nose up: the bow jet alone, 300. Nose down: the bow jet and the stern jet, 600. The weaker is 300.
        assertEquals(3.0, authority.pitch(), EPS);
        assertEquals(0.0, authority.yaw(), EPS);
        assertEquals(0.0, authority.roll(), EPS);
    }

    @Test
    void authorityCountsWhatTheAllocatorReallyOpens() {
        // One jet each way, its torque 60 degrees off the pitch axis: the allocator opens it halfway, and half of that torque is pitch.
        double side = Math.sqrt(3.0);
        List<Thruster> ship = List.of(jet(side, 0, -1, 0, 1, 0), jet(side, 0, -1, 0, -1, 0));
        Authority authority = Authority.of(ship, FRAME, new Matrix3d().scaling(0.01));
        assertEquals(0.5, authority.pitch(), 1e-6, "100 force, lever 1 along the axis, half open: 50 torque");
    }

    @Test
    void aOneWayShipCannotStopSoItHasNoAuthority() {
        Authority authority = Authority.of(List.of(jet(0, 0, -3, 0, 1, 0)), FRAME, new Matrix3d().scaling(0.01));
        assertEquals(0.0, authority.pitch(), EPS);
    }
}
