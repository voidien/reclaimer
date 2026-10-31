package com.voidreclaimer.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class AttitudeTest {
    private static final double EPS = 1e-6;
    private static final Vector3d[] NORTH = HelmBlockEntity.axes(Direction.NORTH);

    private static Attitude of(Quaterniond orientation, Vector3d[] axes) {
        return Attitude.of(orientation, axes[0], axes[1], axes[2]);
    }

    @Test
    void anUnturnedShipFacingNorthIsLevelAndHeadsNorth() {
        Attitude a = of(new Quaterniond(), NORTH);
        assertEquals(0, a.pitch(), EPS);
        assertEquals(0, a.roll(), EPS);
        assertEquals(0, a.heading(), EPS);
    }

    @Test
    void theHeadingIsACompassBearingOfTheNose() {
        assertEquals(90, of(new Quaterniond(), HelmBlockEntity.axes(Direction.EAST)).heading(), EPS, "a chair facing east");
        assertEquals(180, of(new Quaterniond(), HelmBlockEntity.axes(Direction.SOUTH)).heading(), EPS);
        assertEquals(270, of(new Quaterniond(), HelmBlockEntity.axes(Direction.WEST)).heading(), EPS);
        // A north-facing ship yawed right a quarter turn (clockwise seen from above, a negative turn about +Y) heads east.
        assertEquals(90, of(new Quaterniond().rotateY(Math.toRadians(-90)), NORTH).heading(), EPS);
    }

    @Test
    void noseUpIsPositivePitch() {
        // Nose up for a north-facing ship is a positive turn about its right axis, east (+X).
        Quaterniond noseUp = new Quaterniond().rotateAxis(Math.toRadians(20), 1, 0, 0);
        Attitude a = of(noseUp, NORTH);
        assertEquals(20, a.pitch(), EPS);
        assertEquals(0, a.roll(), EPS);
        assertEquals(0, a.heading(), EPS);
    }

    @Test
    void rollingRightIsPositiveRoll() {
        // Roll right tips the ship's up toward its right: a positive turn about its forward axis, north (-Z).
        Quaterniond rollRight = new Quaterniond().rotateAxis(Math.toRadians(30), 0, 0, -1);
        Attitude a = of(rollRight, NORTH);
        assertEquals(30, a.roll(), EPS);
        assertEquals(0, a.pitch(), EPS);
        Quaterniond rollLeft = new Quaterniond().rotateAxis(Math.toRadians(-30), 0, 0, -1);
        assertEquals(-30, of(rollLeft, NORTH).roll(), EPS);
    }

    @Test
    void theReadoutRoundTripsThroughItsCodec() {
        HelmBlockEntity.Readout readout = new HelmBlockEntity.Readout(true, 7, new double[] {1, 0, 0.5, 0.5, 0, 0},
                new double[] {0, 0, 0, 0, 1, 1}, 1200, 4000, 8000, 16000, new double[] {3, -0.5, 0.25}, new double[] {0, 10, -45},
                new Attitude(12.5, -30, 270));
        ByteBuf buf = Unpooled.buffer();
        HelmBlockEntity.Readout.STREAM_CODEC.encode(buf, readout);
        HelmBlockEntity.Readout back = HelmBlockEntity.Readout.STREAM_CODEC.decode(buf);
        assertEquals(readout.onShip(), back.onShip());
        assertEquals(readout.thrusters(), back.thrusters());
        org.junit.jupiter.api.Assertions.assertArrayEquals(readout.move(), back.move(), EPS);
        org.junit.jupiter.api.Assertions.assertArrayEquals(readout.turn(), back.turn(), EPS);
        assertEquals(readout.fuel(), back.fuel(), EPS);
        assertEquals(readout.fuelCapacity(), back.fuelCapacity(), EPS);
        assertEquals(readout.energy(), back.energy());
        assertEquals(readout.energyCapacity(), back.energyCapacity());
        org.junit.jupiter.api.Assertions.assertArrayEquals(readout.velocity(), back.velocity(), EPS);
        org.junit.jupiter.api.Assertions.assertArrayEquals(readout.spin(), back.spin(), EPS);
        assertEquals(readout.attitude(), back.attitude());
        assertEquals(Math.sqrt(9 + 0.25 + 0.0625), back.speed(), EPS);
    }
}
