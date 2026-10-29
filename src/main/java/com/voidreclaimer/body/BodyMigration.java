package com.voidreclaimer.body;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Bodies from before sections: a frame plus parts per kind. The frame item is now the chassis's core section (registry
 * alias), the body gets that chassis's basic Mk I sections, and the parts go onto matching mounts in layout order.
 * Parts that don't fit wait in {@link BodyData#pending()} and are handed back (or dropped) on the next tick.
 */
public final class BodyMigration {
    private BodyMigration() {}

    /** The basic sections each built-in chassis comes with (core section included). */
    public static Map<String, ItemStack> basicSections(ResourceKey<Chassis> chassis) {
        Map<String, ItemStack> sections = new LinkedHashMap<>();
        if (chassis.equals(BuiltinBodies.MECH)) {
            put(sections, "torso", ModItems.MECH_TORSO.get());
            put(sections, "head", ModItems.MECH_HEAD.get());
            put(sections, "pelvis", ModItems.MECH_PELVIS.get());
            for (String side : List.of("left", "right")) {
                put(sections, side + "_upper_arm", ModItems.MECH_UPPER_ARM.get());
                put(sections, side + "_forearm", ModItems.MECH_FOREARM.get());
                put(sections, side + "_hand", ModItems.MECH_HAND.get());
                put(sections, side + "_thigh", ModItems.MECH_THIGH.get());
                put(sections, side + "_shin", ModItems.MECH_SHIN.get());
                put(sections, side + "_foot", ModItems.MECH_FOOT.get());
            }
        } else if (chassis.equals(BuiltinBodies.SHIP)) {
            put(sections, "keel", ModItems.SHIP_KEEL.get());
            put(sections, "bow", ModItems.SHIP_BOW.get());
            put(sections, "bridge", ModItems.SHIP_BRIDGE.get());
            for (int i = 1; i <= 6; i++) {
                put(sections, "hull_" + i, ModItems.SHIP_HULL_SEGMENT.get());
                put(sections, "hardpoint_" + i, ModItems.SHIP_HARDPOINT.get());
            }
            put(sections, "wing_port", ModItems.SHIP_WING.get());
            put(sections, "wing_starboard", ModItems.SHIP_WING.get());
            for (int i = 1; i <= 4; i++) {
                put(sections, "nacelle_" + i, ModItems.SHIP_ENGINE_NACELLE.get());
                put(sections, "cargo_" + i, ModItems.SHIP_CARGO_BAY.get());
            }
            put(sections, "mast_port", ModItems.SHIP_SENSOR_MAST.get());
            put(sections, "mast_starboard", ModItems.SHIP_SENSOR_MAST.get());
            put(sections, "rcs_fore", ModItems.SHIP_RCS_CLUSTER.get());
            put(sections, "rcs_aft", ModItems.SHIP_RCS_CLUSTER.get());
            put(sections, "dorsal_spine", ModItems.SHIP_DORSAL_SPINE.get());
            put(sections, "ventral_fin", ModItems.SHIP_VENTRAL_FIN.get());
        } else if (chassis.equals(BuiltinBodies.TITAN)) {
            put(sections, "saucer", ModItems.TITAN_SAUCER.get());
            put(sections, "neck", ModItems.TITAN_NECK.get());
            put(sections, "bridge", ModItems.TITAN_BRIDGE.get());
            put(sections, "sensor", ModItems.TITAN_SENSOR_ARRAY.get());
            put(sections, "deflector", ModItems.TITAN_DEFLECTOR.get());
            put(sections, "bay_door", ModItems.TITAN_BAY_DOOR.get());
            for (String side : List.of("l", "r")) {
                put(sections, "pylon_" + side, ModItems.TITAN_PYLON.get());
                put(sections, "nacelle_" + side, ModItems.TITAN_NACELLE.get());
            }
        } else {
            put(sections, "hull", ModItems.DRONE_HULL.get());
            put(sections, "sensor_dome", ModItems.SENSOR_DOME.get());
            put(sections, "rotor_front_left", ModItems.ROTOR_ARM.get());
            put(sections, "rotor_front_right", ModItems.ROTOR_ARM.get());
            put(sections, "rotor_back_left", ModItems.ROTOR_ARM.get());
            put(sections, "rotor_back_right", ModItems.ROTOR_ARM.get());
            put(sections, "tool_claw", ModItems.TOOL_CLAW.get());
        }
        return sections;
    }

    static BodyData fromFrame(ItemStack frame, Map<ModuleKind, List<ItemStack>> parts, int activeTool, Cosmetics cosmetics) {
        SectionStats core = Parts.section(frame);
        ResourceKey<Chassis> key = core != null && core.chassis().isPresent() ? core.chassis().get() : BuiltinBodies.DRONE;
        Chassis chassis = BuiltinBodies.CHASSIS.getOrDefault(key, BuiltinBodies.CHASSIS.get(BuiltinBodies.DRONE));
        List<ItemStack> pending = new ArrayList<>();
        Map<String, ItemStack> sections = basicSections(BuiltinBodies.CHASSIS.containsKey(key) ? key : BuiltinBodies.DRONE);
        if (core != null && core.isCore()) {
            sections.put(chassis.coreSocket(), frame.copy());
        } else {
            pending.add(frame.copy());
        }
        Map<BodyData.Mount, ItemStack> modules = new LinkedHashMap<>();
        for (ModuleKind kind : ModuleKind.values()) {
            List<BodyData.Mount> free = new ArrayList<>();
            for (Socket socket : chassis.sockets()) {
                SectionStats section = Parts.section(sections.getOrDefault(socket.id(), ItemStack.EMPTY));
                for (int i = 0; section != null && i < section.mounts(kind); i++) {
                    free.add(new BodyData.Mount(socket.id(), kind, i));
                }
            }
            for (ItemStack part : parts.getOrDefault(kind, List.of())) {
                if (part.isEmpty()) {
                    continue;
                }
                if (free.isEmpty()) {
                    pending.add(part.copy());
                } else {
                    modules.put(free.removeFirst(), part.copy());
                }
            }
        }
        return new BodyData(chassis.coreSocket(), sections, modules, activeTool, cosmetics, pending);
    }

    private static void put(Map<String, ItemStack> sections, String socket, Item item) {
        sections.put(socket, new ItemStack(item));
    }
}
