package com.voidreclaimer.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.junit.jupiter.api.Test;

class StationMathTest {
    private static Quaterniond yaw(double degrees) {
        return new Quaterniond().rotateY(Math.toRadians(degrees));
    }

    @Test
    void theNearestQuarterTurnIsFound() {
        assertEquals(0, StationMath.quarterTurns(new Quaterniond()));
        assertEquals(1, StationMath.quarterTurns(yaw(90)));
        assertEquals(2, StationMath.quarterTurns(yaw(180)));
        assertEquals(3, StationMath.quarterTurns(yaw(270)));
        assertEquals(3, StationMath.quarterTurns(yaw(-90)));
        assertEquals(1, StationMath.quarterTurns(yaw(80)), "80 degrees rounds to one turn");
        assertEquals(0, StationMath.quarterTurns(yaw(-40)), "-40 degrees rounds to none");
    }

    @Test
    void rotateYAgreesWithVanillasYRot() {
        // Vec3.yRot(quarter turns * 90 degrees) is what Sable's assembly transform applies.
        BlockPos offset = new BlockPos(3, 1, 7);
        for (int turns = 0; turns < 4; turns++) {
            Vec3 expected = new Vec3(3, 1, 7).yRot((float) (turns * Math.PI / 2));
            BlockPos got = StationMath.rotateY(offset, turns);
            assertEquals(Math.round(expected.x), got.getX(), "x after " + turns + " turns");
            assertEquals(Math.round(expected.z), got.getZ(), "z after " + turns + " turns");
            assertEquals(1, got.getY());
        }
    }

    @Test
    void aLeaningShipIsNotLevel() {
        assertTrue(StationMath.isLevel(new Quaterniond(), 10));
        assertTrue(StationMath.isLevel(yaw(135), 10), "yaw is not a lean");
        assertTrue(StationMath.isLevel(new Quaterniond().rotateX(Math.toRadians(8)), 10));
        assertFalse(StationMath.isLevel(new Quaterniond().rotateX(Math.toRadians(12)), 10));
        assertFalse(StationMath.isLevel(new Quaterniond().rotateZ(Math.toRadians(45)), 10));
    }

    @Test
    void theBlockRotationIsSablesFourMinusOrdinal() {
        assertEquals(Rotation.NONE, StationMath.blockRotation(0));
        assertEquals(Rotation.COUNTERCLOCKWISE_90, StationMath.blockRotation(1));
        assertEquals(Rotation.CLOCKWISE_180, StationMath.blockRotation(2));
        assertEquals(Rotation.CLOCKWISE_90, StationMath.blockRotation(3));
    }
}
