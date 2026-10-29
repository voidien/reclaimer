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

    public static final Map<ResourceKey<SizeClass>, SizeClass> SIZE_CLASSES = Map.of(
            DRONE_SIZE, new SizeClass(0.4, 0),
            MECH_SIZE, new SizeClass(1.2, 1),
            SHIP_SIZE, new SizeClass(3.0, 2));

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

    // ---- chassis ------------------------------------------------------------------------------------------

    public static final ResourceKey<Chassis> DRONE = chassisKey("drone");
    public static final ResourceKey<Chassis> MECH = chassisKey("mech");
    public static final ResourceKey<Chassis> SHIP = chassisKey("ship");

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
        map.put(MECH, new Chassis(MECH_SIZE, "torso", List.copyOf(mech)));

        List<Socket> ship = new ArrayList<>();
        ship.add(root("keel", SHIP_KEEL, "core"));
        ship.add(socket("bow", "keel", 0, 0, -24, "hull", BOW));
        ship.add(socket("bridge", "keel", 0, 6, -12, "control", BRIDGE));
        for (int i = 1; i <= 6; i++) {
            ship.add(socket("hull_" + i, "keel", i % 2 == 0 ? -6 : 6, 0, -12 + 8 * ((i - 1) / 2), "hull", HULL_SEGMENT));
        }
        ship.add(socket("wing_port", "keel", 14, 0, 4, "control", WING));
        ship.add(socket("wing_starboard", "keel", -14, 0, 4, "control", WING));
        for (int i = 1; i <= 4; i++) {
            ship.add(socket("nacelle_" + i, "keel", i <= 2 ? 8 : -8, i % 2 == 0 ? -3 : 3, 20, "propulsion", ENGINE_NACELLE));
        }
        for (int i = 1; i <= 6; i++) {
            // Hardpoints also take a Mech hand.
            ship.add(socket("hardpoint_" + i, "hull_" + i, i % 2 == 0 ? -3 : 3, -2, 0, "weapons", HARDPOINT, HAND));
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
        map.put(SHIP, new Chassis(SHIP_SIZE, "keel", List.copyOf(ship)));
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
