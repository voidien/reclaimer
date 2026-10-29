package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.body.ToolModules;
import com.voidreclaimer.body.part.ModuleItem;
import com.voidreclaimer.body.part.SectionItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidReclaimer.MOD_ID);

    // ---- modules (mounted on sections) --------------------------------------------------------------------

    public static final DeferredItem<ModuleItem> DUST_SENSOR = module("dust_sensor", ModuleStats.simple(ModuleKind.SENSOR, 1, 0.1f));
    public static final DeferredItem<ModuleItem> SALVAGE_TOOL = tool("salvage_tool", ModuleStats.simple(ModuleKind.TOOL, 1, 0.1f), "salvage");
    public static final DeferredItem<ModuleItem> NANITE_SWARM = tool("nanite_swarm", ModuleStats.simple(ModuleKind.TOOL, 1, 0.15f), "nanite_swarm");

    public static final DeferredItem<ModuleItem> CORE_MK1 = module("core_mk1", ModuleStats.core(1, 0.2f, new ModuleStats.Core(100f, 1.0f)));
    public static final DeferredItem<ModuleItem> CORE_MK2 = module("core_mk2", ModuleStats.core(2, 0.5f, new ModuleStats.Core(250f, 1.5f)));
    /** Starter thruster: exactly the Phase 2 starter drone handling (thrust 0.03, brake 0.04, 10 m/s). */
    public static final DeferredItem<ModuleItem> THRUSTER_MK1 = module("thruster_mk1",
            ModuleStats.thruster(1, 0.3f, Optional.empty(), new ModuleStats.Thruster(0.03f, 0.04f, 10f)));
    public static final DeferredItem<ModuleItem> THRUSTER_MK2 = module("thruster_mk2",
            ModuleStats.thruster(2, 0.6f, Optional.of(BuiltinBodies.MECH_SIZE), new ModuleStats.Thruster(0.07f, 0.08f, 16f)));
    public static final DeferredItem<ModuleItem> STORAGE_MK1 = module("storage_mk1", ModuleStats.storage(1, 0.2f, new ModuleStats.Storage(1, 100f)));
    public static final DeferredItem<ModuleItem> STORAGE_MK2 = module("storage_mk2", ModuleStats.storage(2, 0.5f, new ModuleStats.Storage(2, 300f)));

    /** Cosmetic modules: no stats; drawn in Phase 5b. */
    public static final DeferredItem<ModuleItem> BRASS_TRIM = module("brass_trim", ModuleStats.simple(ModuleKind.COSMETIC, 1, 0f));
    public static final DeferredItem<ModuleItem> COGWHEEL_CROWN = module("cogwheel_crown", ModuleStats.simple(ModuleKind.COSMETIC, 1, 0f));

    // ---- Drone sections (tier 1) --------------------------------------------------------------------------

    /** The Drone's core section; was the Drone frame. Its sections weigh 0.5 in all, like the old frame. */
    public static final DeferredItem<SectionItem> DRONE_HULL = section("drone_hull", BuiltinBodies.DRONE_HULL, 1,
            s -> s.mass(0.2f).budget(1.6f).health(12f).intake(1f).rows(1).mount(ModuleKind.CORE, 1).mount(ModuleKind.STORAGE, 1)
                    .mount(ModuleKind.COSMETIC, 1).chassis(BuiltinBodies.DRONE));
    public static final DeferredItem<SectionItem> SENSOR_DOME = section("sensor_dome", BuiltinBodies.SENSOR_DOME, 1,
            s -> s.mass(0.05f).mount(ModuleKind.SENSOR, 1));
    public static final DeferredItem<SectionItem> ROTOR_ARM = section("rotor_arm", BuiltinBodies.ROTOR_ARM, 1,
            s -> s.mass(0.05f).turn(0.02f).mount(ModuleKind.THRUSTER, 1));
    public static final DeferredItem<SectionItem> ROTOR_ARM_MK2 = section("rotor_arm_mk2", BuiltinBodies.ROTOR_ARM, 2,
            s -> s.mass(0.04f).health(1f).turn(0.04f).wear(0.5f).mount(ModuleKind.THRUSTER, 1));
    public static final DeferredItem<SectionItem> TOOL_CLAW = section("tool_claw", BuiltinBodies.TOOL_CLAW, 1,
            s -> s.mass(0.05f).mount(ModuleKind.TOOL, 2));

    // ---- Mech sections (tier 2) ---------------------------------------------------------------------------

    /** The Mech's core section; was the Mech frame. A full Mech adds up to about the old frame's stats. */
    public static final DeferredItem<SectionItem> MECH_TORSO = section("mech_torso", BuiltinBodies.MECH_TORSO, 2,
            s -> s.mass(0.8f).budget(3.5f).health(8f).intake(1.5f).rows(2).mount(ModuleKind.CORE, 1).mount(ModuleKind.STORAGE, 2)
                    .mount(ModuleKind.COSMETIC, 1).chassis(BuiltinBodies.MECH));
    public static final DeferredItem<SectionItem> MECH_PELVIS = section("mech_pelvis", BuiltinBodies.PELVIS, 2,
            s -> s.mass(0.2f).budget(0.5f).health(2f).stability(0.1f).mount(ModuleKind.COSMETIC, 1));
    public static final DeferredItem<SectionItem> MECH_HEAD = section("mech_head", BuiltinBodies.HEAD, 2,
            s -> s.mass(0.15f).health(2f).mount(ModuleKind.SENSOR, 2));
    public static final DeferredItem<SectionItem> MECH_UPPER_ARM = section("mech_upper_arm", BuiltinBodies.UPPER_ARM, 2,
            s -> s.mass(0.1f).budget(0.25f).health(1f).reach(0.25f));
    public static final DeferredItem<SectionItem> MECH_FOREARM = section("mech_forearm", BuiltinBodies.FOREARM, 2,
            s -> s.mass(0.1f).health(1f).reach(0.25f));
    public static final DeferredItem<SectionItem> MECH_FOREARM_MK2 = section("mech_forearm_mk2", BuiltinBodies.FOREARM, 2,
            s -> s.mass(0.1f).health(1.5f).reach(0.25f).wear(0.5f).mount(ModuleKind.TOOL, 1));
    public static final DeferredItem<SectionItem> MECH_HAND = section("mech_hand", BuiltinBodies.HAND, 2,
            s -> s.mass(0.05f).mount(ModuleKind.TOOL, 1));
    public static final DeferredItem<SectionItem> MECH_THIGH = section("mech_thigh", BuiltinBodies.THIGH, 2,
            s -> s.mass(0.1f).budget(0.25f).health(1f).stability(0.1f));
    public static final DeferredItem<SectionItem> MECH_SHIN = section("mech_shin", BuiltinBodies.SHIN, 2,
            s -> s.mass(0.08f).health(1f).stability(0.05f).mount(ModuleKind.THRUSTER, 1));
    public static final DeferredItem<SectionItem> MECH_SHIN_MK2 = section("mech_shin_mk2", BuiltinBodies.SHIN, 2,
            s -> s.mass(0.08f).health(1.5f).stability(0.1f).wear(0.5f).mount(ModuleKind.THRUSTER, 1));
    public static final DeferredItem<SectionItem> MECH_FOOT = section("mech_foot", BuiltinBodies.FOOT, 2,
            s -> s.mass(0.05f).stability(0.1f).mount(ModuleKind.THRUSTER, 1));

    // ---- Ship sections (tier 3; craftable from Phase 6) ---------------------------------------------------

    /** The Ship's core section; was the Ship frame. */
    public static final DeferredItem<SectionItem> SHIP_KEEL = section("ship_keel", BuiltinBodies.SHIP_KEEL, 3,
            s -> s.mass(2f).budget(11f).health(14f).intake(2f).rows(3).mount(ModuleKind.CORE, 1).mount(ModuleKind.COSMETIC, 1)
                    .chassis(BuiltinBodies.SHIP));
    public static final DeferredItem<SectionItem> SHIP_BOW = section("ship_bow", BuiltinBodies.BOW, 3,
            s -> s.mass(0.5f).budget(1f).health(2f).intake(0.5f));
    public static final DeferredItem<SectionItem> SHIP_BRIDGE = section("ship_bridge", BuiltinBodies.BRIDGE, 3,
            s -> s.mass(0.4f).health(2f).reach(1f).mount(ModuleKind.SENSOR, 1));
    public static final DeferredItem<SectionItem> SHIP_HULL_SEGMENT = section("ship_hull_segment", BuiltinBodies.HULL_SEGMENT, 3,
            s -> s.mass(0.4f).budget(1f).health(3f));
    public static final DeferredItem<SectionItem> SHIP_HULL_SEGMENT_MK2 = section("ship_hull_segment_mk2", BuiltinBodies.HULL_SEGMENT, 3,
            s -> s.mass(0.4f).budget(1.2f).health(4f).wear(0.5f).mount(ModuleKind.STORAGE, 1));
    public static final DeferredItem<SectionItem> SHIP_WING = section("ship_wing", BuiltinBodies.WING, 3,
            s -> s.mass(0.3f).turn(0.1f).mount(ModuleKind.COSMETIC, 1));
    public static final DeferredItem<SectionItem> SHIP_ENGINE_NACELLE = section("ship_engine_nacelle", BuiltinBodies.ENGINE_NACELLE, 3,
            s -> s.mass(0.4f).mount(ModuleKind.THRUSTER, 2));
    public static final DeferredItem<SectionItem> SHIP_ENGINE_NACELLE_MK2 = section("ship_engine_nacelle_mk2", BuiltinBodies.ENGINE_NACELLE, 3,
            s -> s.mass(0.35f).health(1f).wear(0.5f).mount(ModuleKind.THRUSTER, 2));
    public static final DeferredItem<SectionItem> SHIP_HARDPOINT = section("ship_hardpoint", BuiltinBodies.HARDPOINT, 3,
            s -> s.mass(0.1f).mount(ModuleKind.TOOL, 1));
    public static final DeferredItem<SectionItem> SHIP_CARGO_BAY = section("ship_cargo_bay", BuiltinBodies.CARGO_BAY, 3,
            s -> s.mass(0.3f).mount(ModuleKind.STORAGE, 1));
    public static final DeferredItem<SectionItem> SHIP_SENSOR_MAST = section("ship_sensor_mast", BuiltinBodies.SENSOR_MAST, 3,
            s -> s.mass(0.1f).reach(1f).mount(ModuleKind.SENSOR, 1));
    public static final DeferredItem<SectionItem> SHIP_RCS_CLUSTER = section("ship_rcs_cluster", BuiltinBodies.RCS_CLUSTER, 3,
            s -> s.mass(0.15f).turn(0.1f));
    public static final DeferredItem<SectionItem> SHIP_DORSAL_SPINE = section("ship_dorsal_spine", BuiltinBodies.DORSAL_SPINE, 3,
            s -> s.mass(0.5f).budget(2f).health(3f).mount(ModuleKind.COSMETIC, 1));
    public static final DeferredItem<SectionItem> SHIP_VENTRAL_FIN = section("ship_ventral_fin", BuiltinBodies.VENTRAL_FIN, 3,
            s -> s.mass(0.3f).health(1f).stability(0.1f).mount(ModuleKind.COSMETIC, 1));

    /** A saved body design: save, rebuild from the inventory, queue what's missing. */
    public static final DeferredItem<com.voidreclaimer.body.part.BlueprintItem> BODY_BLUEPRINT = ITEMS.register("body_blueprint",
            () -> new com.voidreclaimer.body.part.BlueprintItem(new Item.Properties().stacksTo(1)));

    // ---- blocks -------------------------------------------------------------------------------------------

    public static final DeferredItem<BlockItem> SCAFFOLD = ITEMS.registerSimpleBlockItem(ModBlocks.SCAFFOLD);
    public static final DeferredItem<BlockItem> TETHER_ANCHOR = ITEMS.registerSimpleBlockItem(ModBlocks.TETHER_ANCHOR);
    public static final DeferredItem<BlockItem> BACKUP_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.BACKUP_CORE);

    static {
        // Frames became core sections; worlds from before keep their items.
        ITEMS.addAlias(VoidReclaimer.id("drone_frame"), DRONE_HULL.getId());
        ITEMS.addAlias(VoidReclaimer.id("mech_frame"), MECH_TORSO.getId());
        ITEMS.addAlias(VoidReclaimer.id("ship_frame"), SHIP_KEEL.getId());
    }

    private ModItems() {}

    private static DeferredItem<ModuleItem> module(String name, ModuleStats stats) {
        return ITEMS.register(name, () -> new ModuleItem(new Item.Properties().stacksTo(1).component(ModDataComponents.MODULE.get(), stats)));
    }

    private static DeferredItem<ModuleItem> tool(String name, ModuleStats stats, String type) {
        return ITEMS.register(name, () -> new ModuleItem(new Item.Properties().stacksTo(1)
                .component(ModDataComponents.MODULE.get(), stats)
                .component(ModDataComponents.TOOL_MODULE.get(), ToolModules.key(type))));
    }

    private static DeferredItem<SectionItem> section(String name, ResourceLocation socketType, int tier,
                                                     java.util.function.UnaryOperator<SectionBuilder> spec) {
        SectionStats stats = spec.apply(new SectionBuilder(socketType, tier)).build();
        return ITEMS.register(name, () -> new SectionItem(new Item.Properties().stacksTo(1).component(ModDataComponents.SECTION.get(), stats)));
    }

    /** Fluent {@link SectionStats} for the definitions above (zero for anything not set). */
    private static final class SectionBuilder {
        private final ResourceLocation socketType;
        private final int tier;
        private float mass, budget, health, reach, intake, stability, turn, wear;
        private int rows;
        private final Map<ModuleKind, Integer> mounts = new EnumMap<>(ModuleKind.class);
        private Optional<net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis>> chassis = Optional.empty();

        SectionBuilder(ResourceLocation socketType, int tier) {
            this.socketType = socketType;
            this.tier = tier;
        }

        SectionBuilder mass(float v) { mass = v; return this; }
        SectionBuilder budget(float v) { budget = v; return this; }
        SectionBuilder health(float v) { health = v; return this; }
        SectionBuilder reach(float v) { reach = v; return this; }
        SectionBuilder intake(float v) { intake = v; return this; }
        SectionBuilder rows(int v) { rows = v; return this; }
        SectionBuilder stability(float v) { stability = v; return this; }
        SectionBuilder turn(float v) { turn = v; return this; }
        SectionBuilder wear(float v) { wear = v; return this; }
        SectionBuilder mount(ModuleKind kind, int count) { mounts.put(kind, count); return this; }
        SectionBuilder chassis(net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> key) { chassis = Optional.of(key); return this; }

        SectionStats build() {
            return new SectionStats(socketType, tier, mass, budget, health, reach, intake, rows, stability, turn, wear,
                    Map.copyOf(mounts), chassis);
        }
    }
}
