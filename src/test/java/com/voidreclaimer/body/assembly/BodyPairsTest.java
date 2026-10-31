package com.voidreclaimer.body.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.body.BuiltinBodies;
import org.junit.jupiter.api.Test;

class BodyPairsTest {
    private static String partner(Chassis chassis, String socket) {
        Socket found = BodyPairs.partner(chassis, socket);
        assertNotNull(found, socket + " has no partner");
        return found.id();
    }

    @Test
    void theMechsLimbsPairDownToTheHandsAndFeet() {
        Chassis mech = BuiltinBodies.CHASSIS.get(BuiltinBodies.MECH);
        assertEquals("right_upper_arm", partner(mech, "left_upper_arm"));
        assertEquals("right_forearm", partner(mech, "left_forearm"));
        assertEquals("left_hand", partner(mech, "right_hand"));
        assertEquals("right_foot", partner(mech, "left_foot"));
        assertNull(BodyPairs.partner(mech, "head"), "on the centre line");
        assertNull(BodyPairs.partner(mech, "torso"), "the core");
    }

    @Test
    void theShipsPairsIncludeTheHardpointsOnItsHull() {
        Chassis ship = BuiltinBodies.CHASSIS.get(BuiltinBodies.SHIP);
        assertEquals("wing_starboard", partner(ship, "wing_port"));
        assertEquals("hull_2", partner(ship, "hull_1"));
        assertEquals("hardpoint_2", partner(ship, "hardpoint_1"));
        assertEquals("hardpoint_5", partner(ship, "hardpoint_6"));
        assertNull(BodyPairs.partner(ship, "bow"));
    }

    @Test
    void everyPairPointsBothWays() {
        for (Chassis chassis : BuiltinBodies.CHASSIS.values()) {
            for (Socket socket : chassis.sockets()) {
                Socket other = BodyPairs.partner(chassis, socket.id());
                if (other != null) {
                    assertEquals(socket.id(), partner(chassis, other.id()), socket.id() + " and " + other.id());
                }
            }
        }
    }
}
