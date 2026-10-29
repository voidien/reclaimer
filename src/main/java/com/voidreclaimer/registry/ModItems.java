package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.PartKind;
import com.voidreclaimer.body.PartStats;
import com.voidreclaimer.body.SizeClass;
import com.voidreclaimer.body.part.PartItem;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidReclaimer.MOD_ID);

    // ---- modules (installed in sensor and tool slots) -----------------------------------------------------

    public static final DeferredItem<PartItem> DUST_SENSOR = part("dust_sensor", PartStats.simple(PartKind.SENSOR, 1, 0.1f));
    public static final DeferredItem<PartItem> SALVAGE_TOOL = part("salvage_tool", PartStats.simple(PartKind.TOOL, 1, 0.1f));
    public static final DeferredItem<PartItem> NANITE_SWARM = part("nanite_swarm", PartStats.simple(PartKind.TOOL, 1, 0.15f));

    // ---- frames ---------------------------------------------------------------------------------------

    /** Starter frame: everything the AI begins with fits under its budget (1.0 of 1.6). */
    public static final DeferredItem<PartItem> DRONE_FRAME = part("drone_frame", PartStats.frame(1, 0.5f,
            new PartStats.Frame(SizeClass.DRONE, 1.6f, 12f, 0f, 1.0f, 1, Map.of(
                    PartKind.THRUSTER, 2, PartKind.STORAGE, 1, PartKind.SENSOR, 1, PartKind.TOOL, 2, PartKind.COSMETIC, 1))));
    public static final DeferredItem<PartItem> MECH_FRAME = part("mech_frame", PartStats.frame(2, 2.0f,
            new PartStats.Frame(SizeClass.MECH, 5.0f, 20f, 1f, 1.5f, 2, Map.of(
                    PartKind.THRUSTER, 4, PartKind.STORAGE, 2, PartKind.SENSOR, 2, PartKind.TOOL, 3, PartKind.COSMETIC, 2))));
    /** Defined now; its recipe arrives with Phase 6 materials. */
    public static final DeferredItem<PartItem> SHIP_FRAME = part("ship_frame", PartStats.frame(3, 8.0f,
            new PartStats.Frame(SizeClass.SHIP, 20f, 40f, 3f, 3.0f, 3, Map.of(
                    PartKind.THRUSTER, 8, PartKind.STORAGE, 4, PartKind.SENSOR, 3, PartKind.TOOL, 4, PartKind.COSMETIC, 4))));

    // ---- cores, thrusters, storage --------------------------------------------------------------------

    public static final DeferredItem<PartItem> CORE_MK1 = part("core_mk1", PartStats.core(1, 0.2f, new PartStats.Core(100f, 1.0f)));
    public static final DeferredItem<PartItem> CORE_MK2 = part("core_mk2", PartStats.core(2, 0.5f, new PartStats.Core(250f, 1.5f)));
    /** Starter thruster: exactly the Phase 2 starter drone handling (thrust 0.03, brake 0.04, 10 m/s). */
    public static final DeferredItem<PartItem> THRUSTER_MK1 = part("thruster_mk1",
            PartStats.thruster(1, 0.3f, SizeClass.DRONE, new PartStats.Thruster(0.03f, 0.04f, 10f)));
    public static final DeferredItem<PartItem> THRUSTER_MK2 = part("thruster_mk2",
            PartStats.thruster(2, 0.6f, SizeClass.MECH, new PartStats.Thruster(0.07f, 0.08f, 16f)));
    public static final DeferredItem<PartItem> STORAGE_MK1 = part("storage_mk1", PartStats.storage(1, 0.2f, new PartStats.Storage(1, 100f)));
    public static final DeferredItem<PartItem> STORAGE_MK2 = part("storage_mk2", PartStats.storage(2, 0.5f, new PartStats.Storage(2, 300f)));

    // ---- cosmetics (no stats; drawn in Phase 5b) --------------------------------------------------------

    public static final DeferredItem<PartItem> BRASS_TRIM = part("brass_trim", PartStats.simple(PartKind.COSMETIC, 1, 0f));
    public static final DeferredItem<PartItem> COGWHEEL_CROWN = part("cogwheel_crown", PartStats.simple(PartKind.COSMETIC, 1, 0f));

    // ---- blocks -------------------------------------------------------------------------------------------

    public static final DeferredItem<BlockItem> SCAFFOLD = ITEMS.registerSimpleBlockItem(ModBlocks.SCAFFOLD);
    public static final DeferredItem<BlockItem> TETHER_ANCHOR = ITEMS.registerSimpleBlockItem(ModBlocks.TETHER_ANCHOR);
    public static final DeferredItem<BlockItem> BACKUP_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.BACKUP_CORE);

    private ModItems() {}

    private static DeferredItem<PartItem> part(String name, PartStats stats) {
        return ITEMS.register(name, () -> new PartItem(new Item.Properties().stacksTo(1).component(ModDataComponents.PART_STATS.get(), stats)));
    }
}
