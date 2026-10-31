package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.SizeClass;
import com.voidreclaimer.api.body.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The built-in size classes, chassis layouts and socket types, exactly as datagen writes them. Datapacks can change
 * or add layouts; code only refers to these ids for the starter body and save migration.
 */
public final class BuiltinBodies {
    // ---- size classes -------------------------------------------------------------------------------------

    public static final ResourceKey<SizeClass> DRONE_SIZE = sizeKey("drone");
    public static final ResourceKey<SizeClass> MECH_SIZE = sizeKey("mech");
    public static final ResourceKey<SizeClass> SHIP_SIZE = sizeKey("ship");
    public static final ResourceKey<SizeClass> TITAN_SIZE = sizeKey("titan");

    public static final Map<ResourceKey<SizeClass>, SizeClass> SIZE_CLASSES = Map.of(
            DRONE_SIZE, new SizeClass(0.4, 0),
            MECH_SIZE, new SizeClass(1.2, 1),
            SHIP_SIZE, new SizeClass(3.0, 2),
            // The Titan model is 8.4 blocks long at scale 1: at 8 it is about 67 blocks.
            TITAN_SIZE, new SizeClass(8.0, 3));

    // ---- socket types (what sections are) -----------------------------------------------------------------

    public static final ResourceLocation DRONE_HULL = id("drone_hull");
    public static final ResourceLocation SENSOR_DOME = id("sensor_dome");
    public static final ResourceLocation ROTOR_ARM = id("rotor_arm");
    public static final ResourceLocation TOOL_CLAW = id("tool_claw");

    public static final ResourceLocation MECH_TORSO = id("mech_torso");
    public static final ResourceLocation PELVIS = id("pelvis");
    public static final ResourceLocation HEAD = id("head");
    public static final ResourceLocation UPPER_ARM = id("upper_arm");
    public static final ResourceLocation FOREARM = id("forearm");
    public static final ResourceLocation HAND = id("hand");
    public static final ResourceLocation THIGH = id("thigh");
    public static final ResourceLocation SHIN = id("shin");
    public static final ResourceLocation FOOT = id("foot");

    public static final ResourceLocation SHIP_KEEL = id("ship_keel");
    public static final ResourceLocation BOW = id("bow");
    public static final ResourceLocation BRIDGE = id("bridge");
    public static final ResourceLocation HULL_SEGMENT = id("hull_segment");
    public static final ResourceLocation WING = id("wing");
    public static final ResourceLocation ENGINE_NACELLE = id("engine_nacelle");
    public static final ResourceLocation HARDPOINT = id("hardpoint");
    public static final ResourceLocation CARGO_BAY = id("cargo_bay");
    public static final ResourceLocation SENSOR_MAST = id("sensor_mast");
    public static final ResourceLocation RCS_CLUSTER = id("rcs_cluster");
    public static final ResourceLocation DORSAL_SPINE = id("dorsal_spine");
    public static final ResourceLocation VENTRAL_FIN = id("ventral_fin");

    public static final ResourceLocation TITAN_SAUCER = id("titan_saucer");
    public static final ResourceLocation TITAN_NECK = id("titan_neck");
    public static final ResourceLocation TITAN_BRIDGE = id("titan_bridge");
    public static final ResourceLocation TITAN_SENSOR_ARRAY = id("titan_sensor_array");
    public static final ResourceLocation TITAN_DEFLECTOR = id("titan_deflector");
    public static final ResourceLocation TITAN_BAY_DOOR = id("titan_bay_door");
    public static final ResourceLocation TITAN_PYLON = id("titan_pylon");
    public static final ResourceLocation TITAN_NACELLE = id("titan_nacelle");

    public static final ResourceLocation SHUTTLE_HULL = id("shuttle_hull");
    public static final ResourceLocation SHUTTLE_NOSE = id("shuttle_nose");
    public static final ResourceLocation SHUTTLE_WINDSHIELD = id("shuttle_windshield");
    public static final ResourceLocation SHUTTLE_DOME = id("shuttle_dome");
    public static final ResourceLocation SHUTTLE_RIDGE = id("shuttle_ridge");
    public static final ResourceLocation SHUTTLE_AFT = id("shuttle_aft");
    public static final ResourceLocation SHUTTLE_STRUT = id("shuttle_strut");
    public static final ResourceLocation SHUTTLE_NACELLE = id("shuttle_nacelle");

    // ---- chassis ------------------------------------------------------------------------------------------

    public static final ResourceKey<Chassis> DRONE = chassisKey("drone");
    public static final ResourceKey<Chassis> MECH = chassisKey("mech");
    public static final ResourceKey<Chassis> SHIP = chassisKey("ship");
    public static final ResourceKey<Chassis> TITAN = chassisKey("titan");
    /** A second layout of the Ship size class: a shuttle, 25 blocks long at scale 3. */
    public static final ResourceKey<Chassis> SHUTTLE = chassisKey("shuttle");

    public static final Map<ResourceKey<Chassis>, Chassis> CHASSIS = chassis();

    private BuiltinBodies() {}

    private static Map<ResourceKey<Chassis>, Chassis> chassis() {
        Map<ResourceKey<Chassis>, Chassis> map = new LinkedHashMap<>();
        map.put(DRONE, new Chassis(DRONE_SIZE, "hull", List.of(
                root("hull", DRONE_HULL, "core"),
                socket("sensor_dome", "hull", 0, 4, 0, "sensors", SENSOR_DOME),
                socket("rotor_front_left", "hull", -4, 1, -4, "propulsion", ROTOR_ARM),
                socket("rotor_front_right", "hull", 4, 1, -4, "propulsion", ROTOR_ARM),
                socket("rotor_back_left", "hull", -4, 1, 4, "propulsion", ROTOR_ARM),
                socket("rotor_back_right", "hull", 4, 1, 4, "propulsion", ROTOR_ARM),
                socket("tool_claw", "hull", 0, -3, -2, "tools", TOOL_CLAW))));

        List<Socket> mech = new ArrayList<>();
        mech.add(root("torso", MECH_TORSO, "core"));
        mech.add(socket("head", "torso", 0, 12, 0, "head", HEAD));
        mech.add(socket("pelvis", "torso", 0, 0, 0, "core", PELVIS));
        // The back takes a Drone rotor arm as a jetpack.
        mech.add(socket("back", "torso", 0, 8, 3, "back", ROTOR_ARM));
        for (String side : List.of("left", "right")) {
            int x = side.equals("left") ? 5 : -5;
            mech.add(socket(side + "_upper_arm", "torso", x, 10, 0, "arms", UPPER_ARM));
            mech.add(socket(side + "_forearm", side + "_upper_arm", 0, -5, 0, "arms", FOREARM));
            mech.add(socket(side + "_hand", side + "_forearm", 0, -5, 0, "arms", HAND));
        }
        for (String side : List.of("left", "right")) {
            int x = side.equals("left") ? 2 : -2;
            mech.add(socket(side + "_thigh", "pelvis", x, 0, 0, "legs", THIGH));
            mech.add(socket(side + "_shin", side + "_thigh", 0, -6, 0, "legs", SHIN));
            mech.add(socket(side + "_foot", side + "_shin", 0, -6, 0, "legs", FOOT));
        }
        map.put(MECH, new Chassis(MECH_SIZE, "torso", List.copyOf(mech)).withWalks(true));

        List<Socket> ship = new ArrayList<>();
        ship.add(root("keel", SHIP_KEEL, "core"));
        ship.add(socket("bow", "keel", 0, 0, -24, "hull", BOW));
        ship.add(socket("bridge", "keel", 0, 6, -12, "control", BRIDGE));
        for (int i = 1; i <= 6; i++) {
            Socket hull = socket("hull_" + i, "keel", i % 2 == 0 ? -6 : 6, 0, -12 + 8 * ((i - 1) / 2), "hull", HULL_SEGMENT);
            // The hull segment, wing and hardpoint models are lopsided: the far side of each pair is drawn flipped.
            ship.add(i % 2 == 0 ? hull.mirrored() : hull);
        }
        ship.add(socket("wing_port", "keel", 14, 0, 4, "control", WING));
        ship.add(socket("wing_starboard", "keel", -14, 0, 4, "control", WING).mirrored());
        for (int i = 1; i <= 4; i++) {
            ship.add(socket("nacelle_" + i, "keel", i <= 2 ? 8 : -8, i % 2 == 0 ? -3 : 3, 20, "propulsion", ENGINE_NACELLE));
        }
        for (int i = 1; i <= 6; i++) {
            // Hardpoints also take a Mech hand.
            Socket hardpoint = socket("hardpoint_" + i, "hull_" + i, i % 2 == 0 ? -3 : 3, -2, 0, "weapons", HARDPOINT, HAND);
            ship.add(i % 2 == 0 ? hardpoint.mirrored() : hardpoint);
        }
        for (int i = 1; i <= 4; i++) {
            ship.add(socket("cargo_" + i, "keel", i % 2 == 0 ? -4 : 4, -4, -4 + 6 * ((i - 1) / 2), "cargo", CARGO_BAY));
        }
        ship.add(socket("mast_port", "keel", 6, 10, 0, "sensors", SENSOR_MAST));
        ship.add(socket("mast_starboard", "keel", -6, 10, 0, "sensors", SENSOR_MAST));
        ship.add(socket("rcs_fore", "keel", 0, 2, -20, "control", RCS_CLUSTER));
        ship.add(socket("rcs_aft", "keel", 0, 2, 16, "control", RCS_CLUSTER));
        ship.add(socket("dorsal_spine", "keel", 0, 8, 4, "hull", DORSAL_SPINE));
        ship.add(socket("ventral_fin", "keel", 0, -8, 8, "control", VENTRAL_FIN));
        map.put(SHIP, new Chassis(SHIP_SIZE, "keel", List.copyOf(ship), new Chassis.Cockpit(0.9f, -0.3f, 0f)));

        // The Shuttle: a second Ship-class layout. Socket ids are the bones of the art rig (pivots are its positions, in pixels).
        List<Socket> shuttle = new ArrayList<>();
        shuttle.add(root("body", SHUTTLE_HULL, "core"));
        shuttle.add(socket("nose", "body", 0, 14.08, 40, "hull", SHUTTLE_NOSE));
        shuttle.add(socket("windshield", "body", 0, 28.8, 35.3, "control", SHUTTLE_WINDSHIELD));
        shuttle.add(socket("dome", "body", 0, 33, 11.2, "sensors", SHUTTLE_DOME));
        shuttle.add(socket("ridge", "body", 0, 31, -14.4, "hull", SHUTTLE_RIDGE));
        shuttle.add(socket("aft", "body", 0, 17.9, -32, "cargo", SHUTTLE_AFT));
        shuttle.add(socket("strut_l", "body", 16.1, 14.1, -12.8, "propulsion", SHUTTLE_STRUT));
        shuttle.add(socket("strut_l2", "body", 16.1, 14.1, -28.8, "propulsion", SHUTTLE_STRUT));
        shuttle.add(socket("strut_r", "body", -16.1, 14.1, -12.8, "propulsion", SHUTTLE_STRUT));
        shuttle.add(socket("strut_r2", "body", -16.1, 14.1, -28.8, "propulsion", SHUTTLE_STRUT));
        shuttle.add(socket("nacelle_l", "body", 40, 10, -19.2, "propulsion", SHUTTLE_NACELLE));
        shuttle.add(socket("nacelle_r", "body", -40, 10, -19.2, "propulsion", SHUTTLE_NACELLE));
        map.put(SHUTTLE, new Chassis(SHIP_SIZE, "body", List.copyOf(shuttle), new Chassis.Cockpit(2.0f, 0.0f, 0f)));

        // The Titan: the saucer is the core. Socket ids are the bones of the art rig (pivots are its positions, in pixels).
        List<Socket> titan = new ArrayList<>();
        titan.add(root("saucer", TITAN_SAUCER, "core"));
        titan.add(socket("neck", "saucer", 0, 17.6, -6.4, "hull", TITAN_NECK));
        titan.add(socket("bridge", "saucer", 0, 35.04, 28, "control", TITAN_BRIDGE));
        titan.add(socket("sensor", "saucer", 0, 18.5, 28, "sensors", TITAN_SENSOR_ARRAY));
        titan.add(socket("deflector", "neck", 0, 10, 9.6, "hull", TITAN_DEFLECTOR));
        titan.add(socket("bay_door", "neck", 0, 6, -48, "cargo", TITAN_BAY_DOOR));
        titan.add(socket("pylon_l", "neck", 6.4, 17.6, -32, "propulsion", TITAN_PYLON));
        titan.add(socket("pylon_r", "neck", -6.4, 17.6, -32, "propulsion", TITAN_PYLON));
        titan.add(socket("nacelle_l", "pylon_l", 22, 38, -34, "propulsion", TITAN_NACELLE));
        titan.add(socket("nacelle_r", "pylon_r", -22, 38, -34, "propulsion", TITAN_NACELLE));
        map.put(TITAN, new Chassis(TITAN_SIZE, "saucer", List.copyOf(titan), new Chassis.Cockpit(1.7f, 0.5f, 0f)));
        return Map.copyOf(map);
    }

    private static Socket root(String id, ResourceLocation type, String area) {
        return new Socket(id, List.of(type), true, Optional.empty(), Vec3.ZERO, area);
    }

    private static Socket socket(String id, String parent, double x, double y, double z, String area, ResourceLocation... types) {
        return new Socket(id, List.of(types), false, Optional.of(parent), new Vec3(x, y, z), area);
    }

    private static ResourceLocation id(String name) {
        return VoidReclaimer.id(name);
    }

    private static ResourceKey<SizeClass> sizeKey(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.SIZE_CLASS, id(name));
    }

    private static ResourceKey<Chassis> chassisKey(String name) {
        return ResourceKey.create(VoidReclaimerRegistries.CHASSIS, id(name));
    }
}
