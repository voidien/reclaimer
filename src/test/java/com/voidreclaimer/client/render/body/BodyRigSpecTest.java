package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.body.BuiltinBodies;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The whole-model body rig specs agree with the models, animations and chassis they name. */
class BodyRigSpecTest {
    private static JsonObject json(String assetPath) throws IOException {
        try (InputStream in = BodyRigSpecTest.class.getResourceAsStream("/assets/void_reclaimer/" + assetPath)) {
            assertNotNull(in, assetPath);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String file(ResourceLocation id) {
        assertEquals("void_reclaimer", id.getNamespace());
        return id.getPath();
    }

    private static BodyRigSpec mech() throws IOException {
        return BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, json("body_rigs/mech.json")).getOrThrow();
    }

    private static Set<String> boneNames(JsonObject geo) {
        Set<String> names = new HashSet<>();
        JsonArray bones = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (JsonElement bone : bones) {
            names.add(bone.getAsJsonObject().get("name").getAsString());
        }
        return names;
    }

    @Test
    void mechSpecParsesAndPointsAtRealFiles() throws IOException {
        BodyRigSpec spec = mech();
        json(file(spec.model()));
        json(file(spec.animations()));
        assertNotNull(BodyRigSpecTest.class.getResourceAsStream("/assets/void_reclaimer/" + file(spec.texture())));
        assertTrue(spec.grounded());
    }

    @Test
    void everySocketOfTheSpecIsASocketOfItsChassis() throws IOException {
        Set<String> sockets = new HashSet<>();
        for (Socket socket : BuiltinBodies.CHASSIS.get(BuiltinBodies.MECH).sockets()) {
            sockets.add(socket.id());
        }
        for (String socket : mech().sockets().keySet()) {
            assertTrue(sockets.contains(socket), "not a mech socket: " + socket);
        }
    }

    @Test
    void everyHiddenBoneExistsInTheModel() throws IOException {
        BodyRigSpec spec = mech();
        Set<String> bones = boneNames(json(file(spec.model())));
        spec.sockets().forEach((socket, names) -> names.forEach(name -> assertTrue(bones.contains(name), socket + " -> " + name)));
    }

    @Test
    void everyPoseAnimationExistsInTheAnimationFile() throws IOException {
        BodyRigSpec spec = mech();
        JsonObject animations = json(file(spec.animations())).getAsJsonObject("animations");
        for (String key : spec.poses().keySet()) {
            String id = spec.animation(key);
            assertNotNull(id);
            assertTrue(animations.has(id), "no animation " + id + " for pose " + key);
        }
        assertNull(spec.animation("low_power"), "the mech has no low-power animation (it falls back to idle)");
    }

    @Test
    void everyPoseKeyIsOneTheAnimatableAsks() throws IOException {
        Set<String> known = new HashSet<>();
        for (BodyLook.Pose pose : BodyLook.Pose.values()) {
            known.add(pose.animation());
        }
        known.add(BodyLook.MODULE_WORK);
        for (String key : mech().poses().keySet()) {
            assertTrue(known.contains(key), "unknown pose key " + key);
        }
    }

    @Test
    void missingFieldsAreRejectedAndOptionalOnesDefault() {
        assertTrue(BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"model\":\"a:b\"}")).error().isPresent());
        BodyRigSpec minimal = BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"model\":\"void_reclaimer:a\",\"texture\":\"void_reclaimer:b\",\"animations\":\"void_reclaimer:c\"}")).getOrThrow();
        assertEquals("", minimal.animationPrefix());
        assertTrue(minimal.sockets().isEmpty() && minimal.poses().isEmpty());
        assertNull(minimal.animation("idle"));
    }
}
