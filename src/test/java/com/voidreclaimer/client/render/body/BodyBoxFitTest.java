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
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A big body's boxes match its model, so new art or a moved box cannot drift apart unnoticed. Boxes are data, read here from the
 * chassis, and the model is read from its {@code .geo.json} and placed exactly as GeckoLib draws it: X flipped, a turned cube turned
 * about its pivot, Z then Y then X, with its X and Y angles negated ({@code BakedModelFactory}, {@code RenderUtil.rotateMatrixAroundCube}).
 * Everything is in blocks at the body's scale, on the pilot's axes: right, up, forward.
 *
 * <p>The Ship has no one model: its keel is the rig's own cubes, and each other section is its own model at its rig bone's pivot, as
 * {@code BodyPartsLayer} draws it. {@code BodyBounds} cannot stand in for this: it stops at each section's pivot, so a Ship reads 15.4
 * blocks long and not its real 21.4.
 */
class BodyBoxFitTest {
    /** How far a model corner may be from every box, as a share of the scale: a Titan's saucer is round and its boxes are not. */
    private static final double MOST_UNCOVERED_PER_SCALE = 0.125;
    /** How far a box may stand past its own section, in blocks. */
    private static final double MOST_OVERHANG = 0.35;
    /** The vanilla player box is this tall at scale 1, and a rig that is not grounded starts from its middle. */
    private static final double VANILLA_HEIGHT = 1.8;
    /** The Ship's section models, by what its sockets start with. */
    private static final Map<String, String> SHIP_SECTIONS = Map.ofEntries(Map.entry("bow", "ship_bow"), Map.entry("bridge", "ship_bridge"),
            Map.entry("hull", "ship_hull_segment"), Map.entry("wing", "ship_wing"), Map.entry("nacelle", "ship_engine_nacelle"),
            Map.entry("hardpoint", "ship_hardpoint"), Map.entry("cargo", "ship_cargo_bay"), Map.entry("mast", "ship_sensor_mast"),
            Map.entry("rcs", "ship_rcs_cluster"), Map.entry("dorsal", "ship_dorsal_spine"), Map.entry("ventral", "ship_ventral_fin"));

    private record Cube(String socket, String bone, List<double[]> corners) {}

    private record Box(String socket, double[] low, double[] high) {
        double distance(double[] point) {
            double sum = 0;
            for (int i = 0; i < 3; i++) {
                double away = Math.max(Math.max(low[i] - point[i], 0), point[i] - high[i]);
                sum += away * away;
            }
            return Math.sqrt(sum);
        }
    }

    private static JsonObject json(String path) throws Exception {
        var stream = BodyBoxFitTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static double[] vector(JsonArray array) {
        return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    private static JsonObject chassis(String name) throws Exception {
        return json("/data/void_reclaimer/void_reclaimer/chassis/" + name + ".json");
    }

    private static double scale(JsonObject chassis) throws Exception {
        String size = chassis.get("size_class").getAsString().split(":")[1];
        return json("/data/void_reclaimer/void_reclaimer/size_class/" + size + ".json").get("scale").getAsDouble();
    }

    /** Where the model's origin stands above the feet, as {@code BodyRenderer.originHeight} puts it. */
    private static double origin(JsonObject rig, double scale) {
        boolean grounded = rig.has("grounded") && rig.get("grounded").getAsBoolean();
        double offset = rig.has("offset_y") ? rig.get("offset_y").getAsDouble() : 0.0;
        return (grounded ? 0.0 : VANILLA_HEIGHT * scale / 2.0) + offset / 16.0 * scale;
    }

    /** The socket that hides each bone on its own, then the ones that hide it with its children. */
    private static Map<String, String> owners(JsonObject rig, String key) {
        Map<String, String> owners = new HashMap<>();
        if (rig.has(key)) {
            rig.getAsJsonObject(key).entrySet().forEach(socket -> socket.getValue().getAsJsonArray().forEach(bone -> owners.put(bone.getAsString(), socket.getKey())));
        }
        return owners;
    }

    private static String owner(String bone, Map<String, String> parents, Map<String, String> alone, Map<String, String> withChildren, String core) {
        if (alone.containsKey(bone)) {
            return alone.get(bone);
        }
        for (String at = bone; at != null; at = parents.get(at)) {
            if (withChildren.containsKey(at)) {
                return withChildren.get(at);
            }
        }
        return core;
    }

    private static JsonArray bones(String model) throws Exception {
        return json("/assets/void_reclaimer/" + model).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
    }

    private static List<Cube> cubes(String name) throws Exception {
        return "ship".equals(name) ? composedCubes(name) : wholeCubes(name);
    }

    /** A rig drawn as one model. Flames ({@code fx_}) are not hull. */
    private static List<Cube> wholeCubes(String name) throws Exception {
        JsonObject chassis = chassis(name);
        JsonObject rig = json("/assets/void_reclaimer/body_rigs/" + name + ".json");
        double scale = scale(chassis);
        double origin = origin(rig, scale);
        // A rig built facing +Z is turned 180 degrees: drawn X is then the pilot's left, and drawn Z the pilot's forward.
        double facing = rig.has("yaw") && rig.get("yaw").getAsFloat() == 180f ? 1.0 : -1.0;
        JsonArray bones = bones(rig.get("model").getAsString().split(":")[1]);
        Map<String, String> parents = new HashMap<>();
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("parent")) {
                parents.put(bone.get("name").getAsString(), bone.get("parent").getAsString());
            }
        }
        Map<String, String> alone = owners(rig, "socket_parts");
        Map<String, String> withChildren = owners(rig, "sockets");
        List<Cube> cubes = new ArrayList<>();
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            if (boneName.startsWith("fx") || !bone.has("cubes")) {
                continue;
            }
            String socket = owner(boneName, parents, alone, withChildren, chassis.get("core_socket").getAsString());
            for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                cubes.add(new Cube(socket, boneName, pilot(drawn(cube.getAsJsonObject(), bone), scale, origin, facing)));
            }
        }
        return cubes;
    }

    /** A body drawn from parts: a composed rig is built facing -Z, and its origin is the middle of the vanilla box. */
    private static List<Cube> composedCubes(String name) throws Exception {
        JsonObject chassis = chassis(name);
        double scale = scale(chassis);
        double origin = VANILLA_HEIGHT * scale / 2.0;
        JsonArray rig = bones("geo/body/" + name + ".geo.json");
        Map<String, double[]> pivots = new HashMap<>();
        List<Cube> cubes = new ArrayList<>();
        for (JsonElement element : rig) {
            JsonObject bone = element.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            pivots.put(boneName, vector(bone.getAsJsonArray("pivot")));
            // A cap only shows while its socket is empty.
            if (bone.has("cubes") && !boneName.startsWith("cap_")) {
                for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                    cubes.add(new Cube(chassis.get("core_socket").getAsString(), boneName, pilot(drawn(cube.getAsJsonObject(), bone), scale, origin, -1.0)));
                }
            }
        }
        for (JsonElement element : chassis.getAsJsonArray("sockets")) {
            JsonObject socket = element.getAsJsonObject();
            String id = socket.get("id").getAsString();
            if (id.equals(chassis.get("core_socket").getAsString())) {
                continue;
            }
            String model = SHIP_SECTIONS.get(id.split("_")[0]);
            assertNotNull(model, "no section model for socket " + id);
            boolean mirrored = socket.has("mirror") && socket.get("mirror").getAsBoolean();
            double[] pivot = pivots.get(id);
            Vector3d at = new Vector3d(-pivot[0], pivot[1], pivot[2]).div(16.0);
            for (JsonElement boneElement : bones("geo/section/" + model + ".geo.json")) {
                JsonObject bone = boneElement.getAsJsonObject();
                if (bone.has("cubes")) {
                    for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                        List<Vector3d> placed = new ArrayList<>();
                        for (Vector3d corner : drawn(cube.getAsJsonObject(), bone)) {
                            placed.add(new Vector3d(mirrored ? -corner.x : corner.x, corner.y, corner.z).add(at));
                        }
                        cubes.add(new Cube(id, model, pilot(placed, scale, origin, -1.0)));
                    }
                }
            }
        }
        return cubes;
    }

    /** A cube's corners as GeckoLib draws them, in blocks at scale 1. */
    private static List<Vector3d> drawn(JsonObject cube, JsonObject bone) {
        double[] from = vector(cube.getAsJsonArray("origin"));
        double[] size = vector(cube.getAsJsonArray("size"));
        double[] angles = cube.has("rotation") ? vector(cube.getAsJsonArray("rotation")) : new double[3];
        JsonArray pivotJson = cube.has("pivot") ? cube.getAsJsonArray("pivot") : bone.getAsJsonArray("pivot");
        double[] pivot = pivotJson == null ? new double[3] : vector(pivotJson);
        Vector3d turnAbout = new Vector3d(-pivot[0], pivot[1], pivot[2]).div(16.0);
        Matrix3d turn = new Matrix3d().rotateZ(Math.toRadians(angles[2])).rotateY(Math.toRadians(-angles[1])).rotateX(Math.toRadians(-angles[0]));
        List<Vector3d> corners = new ArrayList<>();
        for (int mask = 0; mask < 8; mask++) {
            Vector3d corner = new Vector3d(-(from[0] + ((mask & 1) == 0 ? 0 : size[0])), from[1] + ((mask & 2) == 0 ? 0 : size[1]), from[2] + ((mask & 4) == 0 ? 0 : size[2]))
                    .div(16.0).sub(turnAbout);
            corners.add(turn.transform(corner).add(turnAbout));
        }
        return corners;
    }

    /** Drawn corners on the pilot's axes, at the body's scale, measured from the feet. {@code facing} is 1 for a rig turned round, -1 for one built facing -Z. */
    private static List<double[]> pilot(List<Vector3d> drawn, double scale, double origin, double facing) {
        List<double[]> corners = new ArrayList<>();
        for (Vector3d corner : drawn) {
            corners.add(new double[] {-facing * corner.x * scale, corner.y * scale + origin, facing * corner.z * scale});
        }
        return corners;
    }

    private static List<Box> boxes(String name) throws Exception {
        JsonObject chassis = chassis(name);
        double scale = scale(chassis);
        List<Box> boxes = new ArrayList<>();
        for (JsonElement socket : chassis.getAsJsonArray("sockets")) {
            JsonObject entry = socket.getAsJsonObject();
            if (entry.has("boxes")) {
                for (JsonElement box : entry.getAsJsonArray("boxes")) {
                    double[] at = vector(box.getAsJsonObject().getAsJsonArray("at"));
                    double[] size = vector(box.getAsJsonObject().getAsJsonArray("size"));
                    double[] low = new double[3];
                    double[] high = new double[3];
                    for (int i = 0; i < 3; i++) {
                        low[i] = (at[i] - size[i] / 2.0) * scale;
                        high[i] = (at[i] + size[i] / 2.0) * scale;
                    }
                    boxes.add(new Box(entry.get("id").getAsString(), low, high));
                }
            }
        }
        return boxes;
    }

    private static double[][] outline(List<double[]> points) {
        double[][] outline = {{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE}};
        for (double[] point : points) {
            for (int i = 0; i < 3; i++) {
                outline[0][i] = Math.min(outline[0][i], point[i]);
                outline[1][i] = Math.max(outline[1][i], point[i]);
            }
        }
        return outline;
    }

    private static boolean overlaps(Box box, double[][] outline) {
        for (int i = 0; i < 3; i++) {
            if (box.high()[i] <= outline[0][i] || box.low()[i] >= outline[1][i]) {
                return false;
            }
        }
        return true;
    }

    @ParameterizedTest
    @ValueSource(strings = {"titan", "shuttle", "ship"})
    void everyBoxSitsOnItsOwnSection(String name) throws Exception {
        List<Cube> cubes = cubes(name);
        for (Box box : boxes(name)) {
            List<double[]> own = new ArrayList<>();
            boolean touches = false;
            for (Cube cube : cubes) {
                if (cube.socket().equals(box.socket())) {
                    own.addAll(cube.corners());
                    touches |= overlaps(box, outline(cube.corners()));
                }
            }
            assertTrue(touches, name + ": a box of " + box.socket() + " touches none of its section's cubes");
            double[][] outline = outline(own);
            for (int i = 0; i < 3; i++) {
                assertTrue(outline[0][i] - box.low()[i] <= MOST_OVERHANG && box.high()[i] - outline[1][i] <= MOST_OVERHANG,
                        name + ": a box of " + box.socket() + " stands past its section on axis " + i);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"titan", "shuttle", "ship"})
    void everyCubeIsCoveredByABox(String name) throws Exception {
        List<Box> boxes = boxes(name);
        double most = MOST_UNCOVERED_PER_SCALE * scale(chassis(name));
        for (Cube cube : cubes(name)) {
            for (double[] corner : cube.corners()) {
                double nearest = boxes.stream().mapToDouble(box -> box.distance(corner)).min().orElseThrow();
                assertTrue(nearest <= most, name + ": bone " + cube.bone() + " has a corner " + nearest + " from every box");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"titan", "shuttle", "ship"})
    void theBoxesHaveTheModelsOutline(String name) throws Exception {
        List<double[]> model = new ArrayList<>();
        cubes(name).forEach(cube -> model.addAll(cube.corners()));
        List<double[]> ends = new ArrayList<>();
        boxes(name).forEach(box -> {
            ends.add(box.low());
            ends.add(box.high());
        });
        double[][] drawn = outline(model);
        double[][] boxed = outline(ends);
        for (int i = 0; i < 3; i++) {
            assertEquals(drawn[0][i], boxed[0][i], MOST_OVERHANG, name + " low end of axis " + i);
            assertEquals(drawn[1][i], boxed[1][i], MOST_OVERHANG, name + " high end of axis " + i);
        }
    }
}
