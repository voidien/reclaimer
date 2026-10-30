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
}
