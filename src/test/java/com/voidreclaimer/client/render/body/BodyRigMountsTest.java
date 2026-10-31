package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * An audit of every module attachment point on the whole-model rigs, from the rig and model files: each one touches the model, a
 * left and right pair mirrors, and neighbours leave room for their modules. A mount placed by eye and off by a few pixels is a block
 * off at the Titan's scale.
 */
class BodyRigMountsTest {
    /** How far a module may stand off the model, in pixels. A module grows up from its mount, so its whole height counts. */
    private static final double TOUCHING = 1.6;
    /** The tallest module, in pixels: {@code dust_sensor}. */
    private static final double MODULE_HEIGHT = 5.0;
    /** The widest module's half width, in pixels: {@code dust_sensor}. */
    private static final double MODULE_HALF_WIDTH = 1.95;
    /** Mounts closer than this have modules that sink into each other, in pixels. */
    private static final double MIN_SPACING = 2.8;

    private record Cube(double[] from, double[] to) {
        double distance(double[] point) {
            double sum = 0;
            for (int i = 0; i < 3; i++) {
                double away = Math.max(Math.max(from[i] - point[i], 0), point[i] - to[i]);
                sum += away * away;
            }
            return Math.sqrt(sum);
        }
    }

    private record Bone(double[] pivot, List<Cube> cubes) {}

    private static JsonObject json(String path) throws Exception {
        var stream = BodyRigMountsTest.class.getResourceAsStream("/assets/void_reclaimer/" + path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static double[] vector(JsonArray array) {
        return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    private static Map<String, Bone> bones(JsonObject rig) throws Exception {
        String model = rig.get("model").getAsString().split(":")[1];
        Map<String, Bone> bones = new HashMap<>();
        for (JsonElement element : json(model).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            List<Cube> cubes = new ArrayList<>();
            if (bone.has("cubes")) {
                for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                    double[] from = vector(cube.getAsJsonObject().getAsJsonArray("origin"));
                    double[] size = vector(cube.getAsJsonObject().getAsJsonArray("size"));
                    cubes.add(new Cube(from, new double[] {from[0] + size[0], from[1] + size[1], from[2] + size[2]}));
                }
            }
            bones.put(bone.get("name").getAsString(), new Bone(bone.has("pivot") ? vector(bone.getAsJsonArray("pivot")) : new double[3], cubes));
        }
        return bones;
    }

    /** Each mount's point in the model file's axes. */
    private static Map<String, double[]> mounts(JsonObject rig, Map<String, Bone> bones) {
        Map<String, double[]> points = new HashMap<>();
        for (var entry : rig.getAsJsonObject("mounts").entrySet()) {
            JsonObject mount = entry.getValue().getAsJsonObject();
            Bone bone = bones.get(mount.get("bone").getAsString());
            assertNotNull(bone, entry.getKey() + " follows a bone that is not in the model: " + mount.get("bone"));
            double[] offset = mount.has("offset") ? vector(mount.getAsJsonArray("offset")) : new double[3];
            points.put(entry.getKey(), new double[] {bone.pivot()[0] + offset[0], bone.pivot()[1] + offset[1], bone.pivot()[2] + offset[2]});
        }
        return points;
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void everyMountTouchesTheModel(String rigName) throws Exception {
        JsonObject rig = json("body_rigs/" + rigName + ".json");
        Map<String, Bone> bones = bones(rig);
        mounts(rig, bones).forEach((name, point) -> {
            double nearest = Double.MAX_VALUE;
            for (double up = 0; up <= MODULE_HEIGHT; up += 0.5) {
                double[] along = {point[0], point[1] + up, point[2]};
                for (Bone bone : bones.values()) {
                    for (Cube cube : bone.cubes()) {
                        nearest = Math.min(nearest, cube.distance(along));
                    }
                }
            }
            assertTrue(nearest <= TOUCHING, rigName + " " + name + " floats " + nearest + " px off the model");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void neighboursLeaveRoomForTheirModules(String rigName) throws Exception {
        JsonObject rig = json("body_rigs/" + rigName + ".json");
        var points = new ArrayList<>(mounts(rig, bones(rig)).entrySet());
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                double[] a = points.get(i).getValue();
                double[] b = points.get(j).getValue();
                double distance = Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
                assertTrue(distance >= MIN_SPACING, rigName + ": " + points.get(i).getKey() + " and " + points.get(j).getKey() + " are " + distance + " px apart");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void leftAndRightMountsMirrorEachOther(String rigName) throws Exception {
        JsonObject rig = json("body_rigs/" + rigName + ".json");
        Map<String, double[]> points = mounts(rig, bones(rig));
        points.forEach((name, point) -> {
            String other = name.contains("left") ? name.replace("left", "right") : name.contains("storage_1") ? name.replace("storage_1", "storage_2")
                    : name.contains("sensor_1") ? name.replace("sensor_1", "sensor_2") : null;
            if (other == null || !points.containsKey(other)) {
                return;
            }
            double[] mirror = points.get(other);
            assertEquals(point[0], -mirror[0], 0.3, rigName + " " + name + " and " + other + " across");
            assertEquals(point[1], mirror[1], 0.3, rigName + " " + name + " and " + other + " up");
            assertEquals(point[2], mirror[2], 0.3, rigName + " " + name + " and " + other + " along");
        });
    }

    @Test
    void theMechsShoulderSensorsClearItsHeadAndDoNotTurnWithIt() throws Exception {
        JsonObject rig = json("body_rigs/mech.json");
        Map<String, Bone> bones = bones(rig);
        Map<String, double[]> points = mounts(rig, bones);
        double headEdge = bones.get("head").cubes().stream().mapToDouble(cube -> Math.max(Math.abs(cube.from()[0]), Math.abs(cube.to()[0]))).max().orElseThrow();
        for (String name : List.of("head__mount_sensor_1", "head__mount_sensor_2")) {
            double innerEdge = Math.abs(points.get(name)[0]) - MODULE_HALF_WIDTH;
            assertTrue(innerEdge > headEdge, name + ": the module's inner edge is at " + innerEdge + ", the head's at " + headEdge);
            assertEquals("chest", rig.getAsJsonObject("mounts").getAsJsonObject(name).get("bone").getAsString(), "a shoulder piece stays with the shoulders");
        }
    }
}
