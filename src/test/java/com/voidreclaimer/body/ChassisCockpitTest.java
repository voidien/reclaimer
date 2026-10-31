package com.voidreclaimer.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.api.body.Chassis;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChassisCockpitTest {
    @Test
    void onlyTheBigShipsHaveACockpit() {
        assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE).cockpit().isEmpty());
        assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.MECH).cockpit().isEmpty());
        assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP).cockpit().isPresent());
        assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.SHUTTLE).cockpit().isPresent());
        assertTrue(BuiltinBodies.CHASSIS.get(BuiltinBodies.TITAN).cockpit().isPresent());
    }

    @Test
    void theCockpitIsOptionalInTheCodecAndRoundTrips() {
        Chassis ship = BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP);
        Chassis again = Chassis.CODEC.parse(JsonOps.INSTANCE, Chassis.CODEC.encodeStart(JsonOps.INSTANCE, ship).getOrThrow()).getOrThrow();
        assertEquals(ship.cockpit(), again.cockpit());
        var json = Chassis.CODEC.encodeStart(JsonOps.INSTANCE, BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE)).getOrThrow().getAsJsonObject();
        assertTrue(!json.has("cockpit"), "no cockpit key when there is none");
        Chassis parsed = Chassis.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())).getOrThrow();
        assertEquals(Optional.empty(), parsed.cockpit());
    }

    @Test
    void onlyTheMechWalksAndTheFlagIsOptionalInTheCodec() {
        for (var entry : BuiltinBodies.CHASSIS.entrySet()) {
            assertEquals(entry.getKey().equals(BuiltinBodies.MECH), entry.getValue().walks(), entry.getKey().location().toString());
        }
        Chassis mech = BuiltinBodies.CHASSIS.get(BuiltinBodies.MECH);
        var json = Chassis.CODEC.encodeStart(JsonOps.INSTANCE, mech).getOrThrow().getAsJsonObject();
        assertTrue(json.get("walks").getAsBoolean());
        assertTrue(Chassis.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json.toString())).getOrThrow().walks());
        var drone = Chassis.CODEC.encodeStart(JsonOps.INSTANCE, BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE)).getOrThrow().getAsJsonObject();
        assertTrue(!drone.has("walks"), "no walks key for a body that flies");
        assertTrue(!Chassis.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(drone.toString())).getOrThrow().walks());
    }

    @Test
    void theFarSideOfTheShipsLopsidedPairsIsMirrored() {
        var sockets = BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP).sockets();
        assertEquals(java.util.Set.of("hull_2", "hull_4", "hull_6", "wing_starboard", "hardpoint_2", "hardpoint_4", "hardpoint_6"),
                sockets.stream().filter(com.voidreclaimer.api.body.Socket::mirror).map(com.voidreclaimer.api.body.Socket::id)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void mirrorIsOptionalInAChassisFileAndSurvivesTheCodec() {
        var plain = new com.voidreclaimer.api.body.Socket("a", java.util.List.of(), false, java.util.Optional.empty(),
                net.minecraft.world.phys.Vec3.ZERO, "body");
        assertTrue(!plain.mirror(), "the old constructor draws the normal way round");
        var codec = com.voidreclaimer.api.body.Socket.CODEC;
        var json = codec.encodeStart(JsonOps.INSTANCE, plain).getOrThrow().getAsJsonObject();
        assertTrue(!json.has("mirror"), "no key for a normal socket, so existing files stay as they are");
        var mirrored = codec.encodeStart(JsonOps.INSTANCE, plain.mirrored()).getOrThrow();
        assertTrue(codec.parse(JsonOps.INSTANCE, mirrored).getOrThrow().mirror());
    }
}
