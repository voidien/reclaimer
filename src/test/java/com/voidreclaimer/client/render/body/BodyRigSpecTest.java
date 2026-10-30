package com.voidreclaimer.client.render.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final java.util.Map<String, net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis>> RIGS = java.util.Map.of(
            "mech", BuiltinBodies.MECH, "titan", BuiltinBodies.TITAN, "drone", BuiltinBodies.DRONE, "shuttle", BuiltinBodies.SHUTTLE);
    private static final java.util.regex.Pattern MOUNT_NAME =
            java.util.regex.Pattern.compile("^(?:([a-z_]+)__)?mount_(core|thruster|storage|sensor|tool|cosmetic)_[1-9]$");

    private static BodyRigSpec spec(String rig) throws IOException {
        return BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, json("body_rigs/" + rig + ".json")).getOrThrow();
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
    void everySpecParsesAndPointsAtRealFiles() throws IOException {
        for (String rig : RIGS.keySet()) {
            BodyRigSpec spec = spec(rig);
            json(file(spec.model()));
            json(file(spec.animations()));
            assertNotNull(BodyRigSpecTest.class.getResourceAsStream("/assets/void_reclaimer/" + file(spec.texture())), rig);
        }
    }

    @Test
    void everySocketOfASpecIsASocketOfItsChassis() throws IOException {
        for (var entry : RIGS.entrySet()) {
            Set<String> sockets = new HashSet<>();
            for (Socket socket : BuiltinBodies.CHASSIS.get(entry.getValue()).sockets()) {
                sockets.add(socket.id());
            }
            BodyRigSpec spec = spec(entry.getKey());
            for (String socket : java.util.stream.Stream.concat(spec.sockets().keySet().stream(), spec.socketParts().keySet().stream()).toList()) {
                assertTrue(sockets.contains(socket), entry.getKey() + ": not a socket: " + socket);
            }
        }
    }

    @Test
    void everyBoneAndMountBoneOfASpecExistsInItsModel() throws IOException {
        for (String rig : RIGS.keySet()) {
            BodyRigSpec spec = spec(rig);
            Set<String> bones = boneNames(json(file(spec.model())));
            spec.sockets().forEach((socket, names) -> names.forEach(name -> assertTrue(bones.contains(name), rig + ": " + socket + " -> " + name)));
            spec.socketParts().forEach((socket, names) -> names.forEach(name -> assertTrue(bones.contains(name), rig + ": " + socket + " -> " + name)));
            spec.mounts().forEach((mount, place) -> assertTrue(bones.contains(place.bone()), rig + ": mount " + mount + " -> " + place.bone()));
        }
    }

    @Test
    void everyMountNameIsAMountOfARealSocket() throws IOException {
        for (var entry : RIGS.entrySet()) {
            var chassis = BuiltinBodies.CHASSIS.get(entry.getValue());
            Set<String> sockets = new HashSet<>();
            chassis.sockets().forEach(socket -> sockets.add(socket.id()));
            spec(entry.getKey()).mounts().keySet().forEach(name -> {
                var matcher = MOUNT_NAME.matcher(name);
                assertTrue(matcher.matches(), entry.getKey() + ": bad mount name " + name);
                String socket = matcher.group(1);
                assertTrue(socket == null || sockets.contains(socket), entry.getKey() + ": mount of an unknown socket: " + name);
            });
        }
    }

    @Test
    void everyPoseAnimationExistsInTheAnimationFile() throws IOException {
        for (String rig : RIGS.keySet()) {
            BodyRigSpec spec = spec(rig);
            JsonObject animations = json(file(spec.animations())).getAsJsonObject("animations");
            for (String key : spec.poses().keySet()) {
                String id = spec.animation(key);
                assertNotNull(id);
                assertTrue(animations.has(id), rig + ": no animation " + id + " for pose " + key);
            }
            assertFalse(spec.poses().isEmpty(), rig + " has no animation at all");
        }
        assertNull(spec("mech").animation("low_power"), "the mech has no low-power animation (it falls back to idle)");
    }

    @Test
    void everyPoseKeyIsOneTheAnimatableAsks() throws IOException {
        Set<String> known = new HashSet<>();
        for (BodyLook.Pose pose : BodyLook.Pose.values()) {
            known.add(pose.animation());
        }
        known.add(BodyLook.MODULE_WORK);
        for (String rig : RIGS.keySet()) {
            for (String key : spec(rig).poses().keySet()) {
                assertTrue(known.contains(key), rig + ": unknown pose key " + key);
            }
        }
    }

    @Test
    void theMechAndTheTitanStandOnTheirOriginTheOthersAreCentred() throws IOException {
        assertTrue(spec("mech").grounded());
        assertTrue(spec("titan").grounded());
        assertFalse(spec("drone").grounded());
        assertFalse(spec("shuttle").grounded());
    }

    @Test
    void theDroneIsASkinAndTheShuttleHidesPartsBySocket() throws IOException {
        assertTrue(spec("drone").sockets().isEmpty() && spec("drone").socketParts().isEmpty(), "the probe skin shows no part by part");
        assertEquals(java.util.Set.of("nose", "windshield", "dome", "ridge", "aft", "strut_l", "strut_l2", "strut_r", "strut_r2", "nacelle_l", "nacelle_r"),
                spec("shuttle").sockets().keySet());
        assertTrue(spec("drone").pitches() && spec("shuttle").pitches());
        assertEquals("shuttle", BodyLook.rig(BuiltinBodies.SHUTTLE));
    }

    @Test
    void theTitanFliesLikeAShipAndTheMechStaysUpright() throws IOException {
        assertTrue(spec("titan").pitches());
        assertFalse(spec("mech").pitches());
        assertFalse(spec("mech").lean().isNone(), "the upright Mech leans into its motion");
        assertEquals(180f, spec("titan").yaw());
        assertEquals("titan", BodyLook.rig(BuiltinBodies.TITAN));
    }

    @Test
    void missingFieldsAreRejectedAndOptionalOnesDefault() {
        assertTrue(BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"model\":\"a:b\"}")).error().isPresent());
        BodyRigSpec minimal = BodyRigSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"model\":\"void_reclaimer:a\",\"texture\":\"void_reclaimer:b\",\"animations\":\"void_reclaimer:c\"}")).getOrThrow();
        assertEquals("", minimal.animationPrefix());
        assertTrue(minimal.sockets().isEmpty() && minimal.poses().isEmpty() && minimal.mounts().isEmpty() && minimal.socketParts().isEmpty());
        assertFalse(minimal.pitches());
        assertTrue(minimal.lean().isNone(), "no lean unless a rig asks for it");
        assertEquals(0f, minimal.yaw());
        assertNull(minimal.animation("idle"));
    }

    @Test
    void aMountReadsItsBoneOffsetAndYaw() {
        BodyRigSpec.Mount mount = BodyRigSpec.Mount.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"bone\":\"head\",\"offset\":[1,2,3],\"yaw\":180}")).getOrThrow();
        assertEquals("head", mount.bone());
        assertEquals(1f, mount.x());
        assertEquals(2f, mount.y());
        assertEquals(3f, mount.z());
        assertEquals(180f, mount.yaw());
        BodyRigSpec.Mount bare = BodyRigSpec.Mount.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"bone\":\"head\"}")).getOrThrow();
        assertEquals(0f, bare.x() + bare.y() + bare.z() + bare.yaw());
        assertEquals(0f, BodyRigSpec.Mount.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"bone\":\"head\",\"offset\":[1,2]}")).getOrThrow().x(), "a bad offset falls back to zero");
    }
}
