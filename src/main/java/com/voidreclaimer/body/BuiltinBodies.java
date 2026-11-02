package com.voidreclaimer.body;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.HitBox;
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
    public static final ResourceKey<SizeClass> SHUTTLE_SIZE = sizeKey("shuttle");
    public static final ResourceKey<SizeClass> TITAN_SIZE = sizeKey("titan");

    /** The same pods hold four times as much on a Titan, so it holds the most by a large margin: 5,000 of each dust with Mk II pods. */
    private static final double TITAN_STORAGE = 4.0;

    public static final Map<ResourceKey<SizeClass>, SizeClass> SIZE_CLASSES = Map.of(
            DRONE_SIZE, new SizeClass(0.4, 0),
            MECH_SIZE, new SizeClass(1.2, 1, 1.4),
            SHIP_SIZE, new SizeClass(3.0, 2, 3.0),
            // The same rank as the Ship, so parts and unlocks treat it alike. The model is 8.5 blocks long at scale 1: about 17 at 2.
            SHUTTLE_SIZE, new SizeClass(2.0, 2, 4.0),
            // The Titan model is 8.4 blocks long at scale 1: at 8 it is about 67 blocks.
            TITAN_SIZE, new SizeClass(8.0, 3, 2.0, TITAN_STORAGE));

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
    /** A smaller, faster layout with its own size class: a shuttle, about 17 blocks long at scale 2. */
    public static final ResourceKey<Chassis> SHUTTLE = chassisKey("shuttle");

    /** How far under the feet the Shuttle's model origin stands, in blocks: half its 3.6-block box, less its rig's {@code offset_y} of 18.5 px at scale 2. */
    private static final double SHUTTLE_ORIGIN_BELOW_FEET = 0.5125;

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
        shuttle.add(root("body", SHUTTLE_HULL, "core").withBoxes(shuttleBox(0, 2, 2.5, 4.2, 3.9, 5), shuttleBox(0, 2, -2.5, 4.2, 3.9, 5)));
        shuttle.add(socket("nose", "body", 0, 14.08, 40, "hull", SHUTTLE_NOSE).withBoxes(shuttleBox(0, 1.8, 6.4, 3.6, 2, 2.8)));
        shuttle.add(socket("windshield", "body", 0, 28.8, 35.3, "control", SHUTTLE_WINDSHIELD));
        shuttle.add(socket("dome", "body", 0, 33, 11.2, "sensors", SHUTTLE_DOME).withBoxes(shuttleBox(0, 4.25, 2.4, 2, 0.7, 2)));
        shuttle.add(socket("ridge", "body", 0, 31, -14.4, "hull", SHUTTLE_RIDGE).withBoxes(shuttleBox(0, 4.12, -1.8, 1.5, 0.5, 5)));
        shuttle.add(socket("aft", "body", 0, 17.9, -32, "cargo", SHUTTLE_AFT).withBoxes(shuttleBox(0, 2.5, -5, 5, 3.4, 2)));
        shuttle.add(socket("strut_l", "body", 16.1, 14.1, -12.8, "propulsion", SHUTTLE_STRUT).withBoxes(shuttleBox(-3.25, 1.75, -1.6, 2.5, 0.5, 1.5)));
        shuttle.add(socket("strut_l2", "body", 16.1, 14.1, -28.8, "propulsion", SHUTTLE_STRUT).withBoxes(shuttleBox(-3.25, 1.75, -3.6, 2.5, 0.5, 1.5)));
        shuttle.add(socket("strut_r", "body", -16.1, 14.1, -12.8, "propulsion", SHUTTLE_STRUT).withBoxes(shuttleBox(3.25, 1.75, -1.6, 2.5, 0.5, 1.5)));
        shuttle.add(socket("strut_r2", "body", -16.1, 14.1, -28.8, "propulsion", SHUTTLE_STRUT).withBoxes(shuttleBox(3.25, 1.75, -3.6, 2.5, 0.5, 1.5)));
        shuttle.add(socket("nacelle_l", "body", 40, 10, -19.2, "propulsion", SHUTTLE_NACELLE)
                .withBoxes(shuttleBox(-5, 1.25, -4.7, 1.6, 1.5, 4.6), shuttleBox(-5, 1.25, -0.2, 1.6, 1.5, 4.6)));
        shuttle.add(socket("nacelle_r", "body", -40, 10, -19.2, "propulsion", SHUTTLE_NACELLE)
                .withBoxes(shuttleBox(5, 1.25, -4.7, 1.6, 1.5, 4.6), shuttleBox(5, 1.25, -0.2, 1.6, 1.5, 4.6)));
        map.put(SHUTTLE, new Chassis(SHUTTLE_SIZE, "body", List.copyOf(shuttle), new Chassis.Cockpit(2.0f, 0.0f, 0f)));

        // The Titan: the saucer is the core. Socket ids are the bones of the art rig (pivots are its positions, in pixels).
        List<Socket> titan = new ArrayList<>();
        titan.add(root("saucer", TITAN_SAUCER, "core").withBoxes(titanBox(0, 13.5, 14, 16, 8, 32),
                titanBox(-12, 13.5, 14, 8, 8, 22), titanBox(12, 13.5, 14, 8, 8, 22)));
        // The neck's section is also the drive hull, the rig's root bone.
        titan.add(socket("neck", "saucer", 0, 17.6, -6.4, "hull", TITAN_NECK).withBoxes(titanBox(0, 11, 0.8, 3, 3, 10),
                titanBox(0, 5.25, -17, 10, 10.5, 10), titanBox(0, 5.25, -7, 10, 10.5, 10), titanBox(0, 5.25, 1, 10, 10.5, 10)));
        titan.add(socket("bridge", "saucer", 0, 35.04, 28, "control", TITAN_BRIDGE).withBoxes(titanBox(0, 18, 14, 4, 1.2, 4)));
        titan.add(socket("sensor", "saucer", 0, 18.5, 28, "sensors", TITAN_SENSOR_ARRAY));
        titan.add(socket("deflector", "neck", 0, 10, 9.6, "hull", TITAN_DEFLECTOR));
        titan.add(socket("bay_door", "neck", 0, 6, -48, "cargo", TITAN_BAY_DOOR).withBoxes(titanBox(0, 5.5, -23.5, 6, 5, 3)));
        titan.add(socket("pylon_l", "neck", 6.4, 17.6, -32, "propulsion", TITAN_PYLON)
                .withBoxes(titanBox(-4.75, 10.9, -16, 4.5, 5.7, 4), titanBox(-9.25, 16.6, -16, 4.5, 5.7, 4)));
        titan.add(socket("pylon_r", "neck", -6.4, 17.6, -32, "propulsion", TITAN_PYLON)
                .withBoxes(titanBox(4.75, 10.9, -16, 4.5, 5.7, 4), titanBox(9.25, 16.6, -16, 4.5, 5.7, 4)));
        titan.add(socket("nacelle_l", "pylon_l", 22, 38, -34, "propulsion", TITAN_NACELLE).withBoxes(titanBox(-11, 19, -26.5, 5, 5, 10),
                titanBox(-11, 19, -17, 5, 5, 10), titanBox(-11, 19, -7.5, 5, 5, 10)));
        titan.add(socket("nacelle_r", "pylon_r", -22, 38, -34, "propulsion", TITAN_NACELLE).withBoxes(titanBox(11, 19, -26.5, 5, 5, 10),
                titanBox(11, 19, -17, 5, 5, 10), titanBox(11, 19, -7.5, 5, 5, 10)));
        map.put(TITAN, new Chassis(TITAN_SIZE, "saucer", List.copyOf(titan), new Chassis.Cockpit(1.7f, 0.5f, 0f)));
        return Map.copyOf(map);
    }

    /** A Titan box, in blocks at the Titan's scale: its feet are the model's origin. */
    private static HitBox titanBox(double right, double up, double forward, double width, double height, double length) {
        return box(SIZE_CLASSES.get(TITAN_SIZE).scale(), right, up, forward, width, height, length);
    }

    /** A Shuttle box, in blocks at the Shuttle's scale, measured from the model's origin, which stands below the feet. */
    private static HitBox shuttleBox(double right, double up, double forward, double width, double height, double length) {
        return box(SIZE_CLASSES.get(SHUTTLE_SIZE).scale(), right, up - SHUTTLE_ORIGIN_BELOW_FEET, forward, width, height, length);
    }

    private static HitBox box(double scale, double right, double up, double forward, double width, double height, double length) {
        return new HitBox(new Vec3(right / scale, up / scale, forward / scale), new Vec3(width / scale, height / scale, length / scale));
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
