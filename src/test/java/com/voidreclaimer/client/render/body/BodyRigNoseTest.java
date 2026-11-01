package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every rig faces where the body looks, read from the models themselves: which end is the front comes from a named part at each end,
 * so a new or rebuilt rig facing backwards fails here rather than in flight.
 */
class BodyRigNoseTest {
    private static JsonObject json(String path) throws Exception {
        var stream = BodyRigNoseTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject bone(JsonObject model, String name) {
        for (JsonElement bone : model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            if (bone.getAsJsonObject().get("name").getAsString().equals(name)) {
                return bone.getAsJsonObject();
            }
        }
        throw new AssertionError("no bone " + name);
    }

    /** The middle of a bone's cubes along the model's z, in pixels. */
    private static double cubesZ(JsonObject model, String name) {
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (JsonElement cube : bone(model, name).getAsJsonArray("cubes")) {
            double from = cube.getAsJsonObject().getAsJsonArray("origin").get(2).getAsDouble();
            low = Math.min(low, from);
            high = Math.max(high, from + cube.getAsJsonObject().getAsJsonArray("size").get(2).getAsDouble());
        }
        return (low + high) / 2;
    }

    private static double pivotZ(JsonObject model, String name) {
        return bone(model, name).getAsJsonArray("pivot").get(2).getAsDouble();
    }

    /** Where the front of a model built this way ends up, for a body looking along +Z at yaw 0. */
    private static void assertNoseFaces(String rig, double frontZ, double backZ, float rigYaw) {
        assertNotEquals(frontZ, backZ, rig + ": the two ends are level, so this says nothing");
        Vector3f nose = BodyLook.turn(0f, 0f, rigYaw).transform(new Vector3f(0f, 0f, (float) Math.signum(frontZ - backZ)));
        assertEquals(1f, nose.z, 1e-3f, rig + " faces away from its look: front at z " + frontZ + ", back at z " + backZ + ", rig yaw " + rigYaw);
    }

    @ParameterizedTest
    @CsvSource({"drone, elbow_r, probe", "mech, toe_l, backpack", "shuttle, nose, exhaust_l", "titan, saucer, bay_door"})
    void aWholeModelRigFacesWhereItLooks(String rig, String front, String back) throws Exception {
        JsonObject spec = json("/assets/void_reclaimer/body_rigs/" + rig + ".json");
        JsonObject model = json("/assets/void_reclaimer/" + spec.get("model").getAsString().split(":")[1]);
        assertNoseFaces(rig, cubesZ(model, front), cubesZ(model, back), spec.get("yaw").getAsFloat());
    }

    /** The Ship has no spec: it is composed from sections plugged in at its bones, so it reads their pivots, and has no rig yaw. */
    @Test
    void theComposedShipFacesWhereItLooks() throws Exception {
        JsonObject model = json("/assets/void_reclaimer/geo/body/ship.geo.json");
        assertNoseFaces("ship", pivotZ(model, "bow"), pivotZ(model, "hull_6"), 0f);
    }
}
