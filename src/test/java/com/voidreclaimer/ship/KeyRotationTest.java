package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.voidreclaimer.ship.ThrustAllocator.Frame;
import com.voidreclaimer.ship.ThrustAllocator.Thruster;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Each rotation key does what its name says, read from the ship's real motion: the jets a key opens give a torque, the torque a spin,
 * and the spin moves the nose or the right wing. Nothing here trusts a comment or the code's own idea of which way is which, so the
 * reversed pitch and roll keys cannot come back unnoticed. It checks a ship facing each way a Helm can.
 */
class KeyRotationTest {
    private static final double EPS = 1e-9;
    private static final double FAR = 3.0;
    private static final Vector3d[] AXES = {new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1)};

    private static final PilotInput NOSE_UP = keys(false, false, true, false, false, false);
    private static final PilotInput NOSE_DOWN = keys(false, false, false, true, false, false);
    private static final PilotInput YAW_LEFT = keys(false, false, false, false, true, false);
    private static final PilotInput YAW_RIGHT = keys(false, false, false, false, false, true);
    private static final PilotInput ROLL_RIGHT = keys(true, false, false, false, false, false);
    private static final PilotInput ROLL_LEFT = keys(false, true, false, false, false, false);

    private static PilotInput keys(boolean rollRight, boolean rollLeft, boolean noseUp, boolean noseDown, boolean yawLeft, boolean yawRight) {
        return PilotInput.of(false, false, false, false, false, false, rollRight, rollLeft, noseUp, noseDown, yawLeft, yawRight);
    }

    /** A jet on every face of the ship pushing each way across that face, so it can turn any way there is. */
    private static List<Thruster> jets() {
        List<Thruster> jets = new ArrayList<>();
        for (Vector3d arm : AXES) {
            for (Vector3d push : AXES) {
                if (arm != push) {
                    for (double side : new double[] {-FAR, FAR}) {
                        for (double way : new double[] {-1.0, 1.0}) {
                            jets.add(new Thruster(new Vector3d(arm).mul(side), new Vector3d(push).mul(way), 100));
                        }
                    }
                }
            }
        }
        return jets;
    }

    private static Frame frame(Direction facing) {
        Vector3d[] axes = HelmBlockEntity.axes(facing);
        return new Frame(axes[0], axes[1], axes[2], new Vector3d());
    }

    /** The spin these throttles give: the sum of each jet's {@code lever x force}, with unit inertia. */
    private static Vector3d spin(List<Thruster> jets, double[] throttles) {
        Vector3d spin = new Vector3d();
        for (int i = 0; i < throttles.length; i++) {
            Thruster jet = jets.get(i);
            spin.add(new Vector3d(jet.at()).cross(jet.push()).mul(jet.maxForce() * throttles[i]));
        }
        return spin;
    }

    /** Which way a point at {@code point} on the ship moves under {@code spin}. */
    private static Vector3d motion(Vector3dc spin, Vector3dc point) {
        return spin.cross(point, new Vector3d());
    }

    private static void check(Direction facing, Function<PilotInput, Vector3d> spinFrom) {
        Frame frame = frame(facing);
        assertNose(frame, spinFrom.apply(NOSE_UP), frame.up(), 1, "nose up", facing);
        assertNose(frame, spinFrom.apply(NOSE_DOWN), frame.up(), -1, "nose down", facing);
        assertNose(frame, spinFrom.apply(YAW_RIGHT), frame.right(), 1, "yaw right", facing);
        assertNose(frame, spinFrom.apply(YAW_LEFT), frame.right(), -1, "yaw left", facing);
        assertRoll(frame, spinFrom.apply(ROLL_RIGHT), -1, "roll right", facing);
        assertRoll(frame, spinFrom.apply(ROLL_LEFT), 1, "roll left", facing);
    }

    private static void assertNose(Frame frame, Vector3d spin, Vector3dc toward, int sign, String key, Direction facing) {
        Vector3d nose = motion(spin, frame.forward());
        assertTrue(nose.dot(toward) * sign > 1.0, key + " facing " + facing + " moved the nose " + nose);
        assertEquals(0.0, nose.dot(frame.right()) * nose.dot(frame.up()), EPS, key + " facing " + facing + " moved the nose on both axes");
        assertEquals(0.0, motion(spin, frame.right()).dot(frame.up()), EPS, key + " facing " + facing + " also rolled the ship");
    }

    private static void assertRoll(Frame frame, Vector3d spin, int sign, String key, Direction facing) {
        Vector3d wing = motion(spin, frame.right());
        assertTrue(wing.dot(frame.up()) * sign > 1.0, key + " facing " + facing + " moved the right wing " + wing);
        assertEquals(0.0, motion(spin, frame.forward()).length(), EPS, key + " facing " + facing + " also moved the nose");
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    void theAllocatorTurnsTheShipTheWayEachKeySays(Direction facing) {
        List<Thruster> jets = jets();
        check(facing, keys -> spin(jets, ThrustAllocator.throttles(jets, frame(facing), keys)));
    }

    @ParameterizedTest
    @EnumSource(value = Direction.class, names = {"NORTH", "SOUTH", "EAST", "WEST"})
    void theSteeringTurnsTheShipTheWayEachKeySays(Direction facing) {
        List<Thruster> jets = jets();
        Frame frame = frame(facing);
        AimController.Authority authority = new AimController.Authority(4, 4, 4);
        for (AimController steering : new AimController[] {AimController.Direct.DEFAULT, AimController.Aimed.DEFAULT}) {
            check(facing, keys -> {
                Vector3d torque = steering.torque(frame, new AimController.Request(keys, 0, 0, frame.forward(), new Vector3d(), true, authority));
                return spin(jets, ThrustAllocator.throttles(jets, frame, new Vector3d(), torque));
            });
        }
    }
}
