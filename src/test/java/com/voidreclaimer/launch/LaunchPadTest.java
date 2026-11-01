package com.voidreclaimer.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class LaunchPadTest {
    private static final BlockPos CONTROLLER = new BlockPos(100, 64, -20);

    @Test
    void theRingIsThePadsEdgeWithoutTheController() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> ring = LaunchPad.ring(CONTROLLER, facing);
            assertEquals(4 * LaunchPad.SIZE - 4 - 1, ring.size(), "ring cells facing " + facing);
            assertFalse(ring.contains(CONTROLLER), "the controller is its own cell");
            assertEquals(ring.size(), new HashSet<>(ring).size(), "no cell twice");
            // The controller's two neighbours along the edge and the far corners are in the ring.
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, 1, 0)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, -1, 0)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, LaunchPad.HALF, LaunchPad.SIZE - 1)));
            assertTrue(ring.contains(LaunchPad.cell(CONTROLLER, facing, -LaunchPad.HALF, LaunchPad.SIZE - 1)));
        }
    }

    @Test
    void theInteriorFillsTheRingWithoutTouchingIt() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> interior = LaunchPad.interior(CONTROLLER, facing);
            Set<BlockPos> ring = new HashSet<>(LaunchPad.ring(CONTROLLER, facing));
            assertEquals((LaunchPad.SIZE - 2) * (LaunchPad.SIZE - 2), interior.size());
            for (BlockPos cell : interior) {
                assertFalse(ring.contains(cell), cell + " is in both the ring and the interior");
                assertEquals(CONTROLLER.getY(), cell.getY(), "the pad is one layer");
            }
            assertFalse(interior.contains(CONTROLLER));
            assertTrue(interior.contains(LaunchPad.centre(CONTROLLER, facing)), "the centre is inside");
        }
    }

    @Test
    void theCentreAndMuzzleLieInFrontOfTheController() {
        assertEquals(CONTROLLER.south(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.SOUTH));
        assertEquals(CONTROLLER.north(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.NORTH));
        assertEquals(CONTROLLER.east(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.EAST));
        assertEquals(CONTROLLER.west(LaunchPad.HALF), LaunchPad.centre(CONTROLLER, Direction.WEST));
        // Across is to the controller's right: facing south, right is west.
        assertEquals(CONTROLLER.west(3).south(2), LaunchPad.cell(CONTROLLER, Direction.SOUTH, 3, 2));
        assertEquals(CONTROLLER.south(3).east(2), LaunchPad.cell(CONTROLLER, Direction.EAST, 3, 2), "facing east, right is south");
        Vec3 muzzle = LaunchPad.muzzle(CONTROLLER, Direction.WEST);
        assertEquals(CONTROLLER.getX() - LaunchPad.HALF - LaunchPad.MUZZLE_FORWARD + 0.5, muzzle.x, 1e-9);
        assertEquals(CONTROLLER.getY() + LaunchPad.MUZZLE_HEIGHT, muzzle.y, 1e-9);
        assertEquals(CONTROLLER.getZ() + 0.5, muzzle.z, 1e-9);
    }

    /** The muzzle is where the model's gun ends at full wind-up, not just near it: launches smoke and streak from there. */
    @Test
    void theMuzzleIsTheMouthOfTheModelsGunAtFullWindUp() throws IOException {
        JsonObject gun;
        try (var in = getClass().getResourceAsStream("/assets/void_reclaimer/geo/entity/mass_driver.geo.json")) {
            JsonObject model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            gun = StreamSupport.stream(model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").spliterator(), false)
                    .map(JsonElement::getAsJsonObject).filter(bone -> bone.get("name").getAsString().equals("gun")).findFirst().orElseThrow();
        }
        // The last ring of the barrel is the highest cube of the gun, tilted about X with the barrel, which runs up towards -Z.
        JsonObject mouth = StreamSupport.stream(gun.getAsJsonArray("cubes").spliterator(), false).map(JsonElement::getAsJsonObject)
                .max(Comparator.comparingDouble(cube -> cube.getAsJsonArray("origin").get(1).getAsDouble())).orElseThrow();
        double tilt = Math.toRadians(mouth.getAsJsonArray("rotation").get(0).getAsDouble());
        double halfLength = mouth.getAsJsonArray("size").get(1).getAsDouble() / 2.0;
        double y = mouth.getAsJsonArray("pivot").get(1).getAsDouble() + halfLength * Math.cos(tilt);
        double z = mouth.getAsJsonArray("pivot").get(2).getAsDouble() - halfLength * Math.sin(tilt);
        // A negative lean takes the barrel further from the vertical, about the gun bone's pivot.
        double pivotY = gun.getAsJsonArray("pivot").get(1).getAsDouble();
        double pivotZ = gun.getAsJsonArray("pivot").get(2).getAsDouble();
        double reach = Math.hypot(y - pivotY, z - pivotZ);
        double lean = Math.atan2(pivotZ - z, y - pivotY) - Math.toRadians(LaunchAnimation.WINDUP_DEGREES);
        assertEquals((pivotY + reach * Math.cos(lean)) / 16.0, LaunchPad.MUZZLE_HEIGHT, 0.02);
        assertEquals((reach * Math.sin(lean) - pivotZ) / 16.0, LaunchPad.MUZZLE_FORWARD, 0.02);
    }
}
