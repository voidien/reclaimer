package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * An audit of the whole-model rigs: what stays on screen when a section comes off. A bone with cubes that no socket owns is drawn
 * whatever the body is made of, which is how the Mech's thruster pods hung in the air with their arm gone. The audit works both ways:
 * every bone has an owner, and every section that can come off has something to hide.
 */
class BodyRigBonesTest {
    /** Bones of the core section, which cannot come off: drawn on purpose. */
    private static final Map<String, Set<String>> ALWAYS_ON = Map.of(
            "drone", Set.of("probe"),
            "mech", Set.of("chest", "spine"),
            "shuttle", Set.of("body"),
            "titan", Set.of("saucer"));
    /**
     * Found by this audit and not fixed yet: the Drone's arms are its tool claw, and wait on its new model. A fix must take its entry
     * out, and a new bone with no owner fails.
     */
    private static final Map<String, Set<String>> KNOWN_LOOSE_BONES = Map.of(
            "drone", Set.of("arm_r", "elbow_r", "arm_r_x", "elbow_r_x"),
            "mech", Set.of(),
            "shuttle", Set.of(),
            "titan", Set.of());
    /** Found by this audit: sections that can come off with nothing changing on the model. The same rule as above. */
    private static final Map<String, Set<String>> KNOWN_EMPTY_SOCKETS = Map.of(
            "drone", Set.of("sensor_dome", "rotor_front_left", "rotor_front_right", "rotor_back_left", "rotor_back_right", "tool_claw"),
            "mech", Set.of(),
            "shuttle", Set.of(),
            "titan", Set.of());

    /** The Ship's core section's own bones, drawn whatever else is fitted. */
    private static final Set<String> SHIP_CORE_BONES = Set.of("keel_casing", "keel_frame", "cog_internal");

    private record Bone(String parent, boolean hasCubes) {}

    private static JsonObject json(String path) throws Exception {
        var stream = BodyRigBonesTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject rig(String name) throws Exception {
        return json("/assets/void_reclaimer/body_rigs/" + name + ".json");
    }

    private static Map<String, Bone> bones(JsonObject rig) throws Exception {
        JsonObject model = json("/assets/void_reclaimer/" + rig.get("model").getAsString().split(":")[1]);
        Map<String, Bone> bones = new HashMap<>();
        for (JsonElement element : model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            boolean cubes = bone.has("cubes") && !bone.getAsJsonArray("cubes").isEmpty();
            bones.put(bone.get("name").getAsString(), new Bone(bone.has("parent") ? bone.get("parent").getAsString() : null, cubes));
        }
        return bones;
    }

    /** The bones a rig's map lists, whichever socket lists them. */
    private static Set<String> listed(JsonObject rig, String key) {
        Set<String> names = new HashSet<>();
        if (rig.has(key)) {
            rig.getAsJsonObject(key).entrySet().forEach(socket -> socket.getValue().getAsJsonArray().forEach(bone -> names.add(bone.getAsString())));
        }
        return names;
    }

    /** Whether hiding a socket hides {@code name}: {@code sockets} takes the bone's children with it, {@code socket_parts} does not. */
    private static boolean owned(String name, Map<String, Bone> bones, Set<String> withChildren, Set<String> alone) {
        if (alone.contains(name)) {
            return true;
        }
        for (String bone = name; bone != null; bone = bones.get(bone).parent()) {
            if (withChildren.contains(bone)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> loose(String name) throws Exception {
        JsonObject rig = rig(name);
        Map<String, Bone> bones = bones(rig);
        Set<String> withChildren = listed(rig, "sockets");
        Set<String> alone = listed(rig, "socket_parts");
        Set<String> found = new HashSet<>();
        bones.forEach((bone, data) -> {
            if (data.hasCubes() && !owned(bone, bones, withChildren, alone)) {
                found.add(bone);
            }
        });
        found.removeAll(ALWAYS_ON.get(name));
        return found;
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void everyBoneWithCubesHasAnOwner(String name) throws Exception {
        assertEquals(KNOWN_LOOSE_BONES.get(name), loose(name), name + ": bones no socket hides and nothing marks as always on");
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void everySectionThatComesOffChangesTheModel(String name) throws Exception {
        JsonObject chassis = json("/data/void_reclaimer/void_reclaimer/chassis/" + name + ".json");
        JsonObject rig = rig(name);
        Set<String> empty = new HashSet<>();
        for (JsonElement element : chassis.getAsJsonArray("sockets")) {
            String socket = element.getAsJsonObject().get("id").getAsString();
            boolean drawn = rig.has("sockets") && rig.getAsJsonObject("sockets").has(socket)
                    || rig.has("socket_parts") && rig.getAsJsonObject("socket_parts").has(socket);
            if (!drawn && !socket.equals(chassis.get("core_socket").getAsString())) {
                empty.add(socket);
            }
        }
        assertEquals(KNOWN_EMPTY_SOCKETS.get(name), empty, name + ": sections that can come off with nothing changing on the model");
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void alwaysOnMeansACoreBoneNoSocketHides(String name) throws Exception {
        JsonObject rig = rig(name);
        Map<String, Bone> bones = bones(rig);
        for (String bone : ALWAYS_ON.get(name)) {
            assertTrue(bones.containsKey(bone) && bones.get(bone).hasCubes(), name + " " + bone + " is not a bone with cubes");
            assertFalse(owned(bone, bones, listed(rig, "sockets"), listed(rig, "socket_parts")), name + " " + bone + " has a socket: it is not always on");
        }
    }

    /**
     * The Ship has no spec: it is composed from section models, each drawn at the bone named after its socket, and a {@code cap_} bone
     * closes the gap when a socket is empty. So a socket needs both, and the only other cubes are the core's.
     */
    @Test
    void theComposedShipHasABoneAndACapForEverySocket() throws Exception {
        JsonObject chassis = json("/data/void_reclaimer/void_reclaimer/chassis/ship.json");
        JsonObject model = json("/assets/void_reclaimer/geo/body/ship.geo.json");
        String core = chassis.get("core_socket").getAsString();
        Set<String> sockets = new HashSet<>();
        chassis.getAsJsonArray("sockets").forEach(socket -> sockets.add(socket.getAsJsonObject().get("id").getAsString()));
        Set<String> names = new HashSet<>();
        Set<String> loose = new HashSet<>();
        for (JsonElement element : model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            names.add(name);
            boolean cap = name.startsWith("cap_") && sockets.contains(name.substring(4));
            if (bone.has("cubes") && !bone.getAsJsonArray("cubes").isEmpty() && !cap && !SHIP_CORE_BONES.contains(name)) {
                loose.add(name);
            }
        }
        assertEquals(Set.of(), loose, "cubes that belong to no socket");
        for (String socket : sockets) {
            assertTrue(names.contains(BodyLook.socketBone(socket)), "no bone to draw the section in " + socket);
            assertTrue(socket.equals(core) || names.contains("cap_" + socket), "nothing closes the gap when " + socket + " is empty");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"drone", "mech", "shuttle", "titan"})
    void everyListedBoneIsInTheModel(String name) throws Exception {
        JsonObject rig = rig(name);
        Set<String> names = bones(rig).keySet();
        Set<String> listed = listed(rig, "sockets");
        listed.addAll(listed(rig, "socket_parts"));
        listed.removeAll(names);
        assertTrue(listed.isEmpty(), name + " lists bones the model does not have: " + listed);
    }
}
