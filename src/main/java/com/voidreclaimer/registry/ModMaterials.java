package com.voidreclaimer.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The plain items of the nebula chains, named as in the design doc. They do nothing by themselves: Create machines and the
 * Replicator make and use them. Registered into {@link ModItems#ITEMS}; {@link #init()} loads this class in time.
 */
public final class ModMaterials {
    /** One material: its registry name, and its English name for the lang file. */
    public record Material(String id, String name, DeferredItem<Item> item) {}

    public static final List<Material> ALL = new ArrayList<>();

    // Ice nebula
    public static final DeferredItem<Item> RAW_ICE_SHARDS = add("raw_ice_shards", "Raw Ice Shards");
    public static final DeferredItem<Item> CRUSHED_CRYO_ICE = add("crushed_cryo_ice", "Crushed Cryo-Ice");
    public static final DeferredItem<Item> COOLANT_FRAME_PLATE = add("coolant_frame_plate", "Coolant Frame Plate");
    // Carbon nebula
    public static final DeferredItem<Item> CARBON_FLAKES = add("carbon_flakes", "Carbon Flakes");
    public static final DeferredItem<Item> FINE_CARBON_POWDER = add("fine_carbon_powder", "Fine Carbon Powder");
    public static final DeferredItem<Item> CARBON_PASTE = add("carbon_paste", "Carbon Paste");
    public static final DeferredItem<Item> CARBON_COMPOSITE_PLATE = add("carbon_composite_plate", "Carbon Composite Plate");
    public static final DeferredItem<Item> SILICON_CARBIDE_SLURRY = add("silicon_carbide_slurry", "Silicon-Carbide Slurry");
    public static final DeferredItem<Item> THERMAL_SHIELDING_TILE = add("thermal_shielding_tile", "Thermal Shielding Tile");
    public static final DeferredItem<Item> SILICON_CARBIDE_COATING = add("silicon_carbide_coating", "Silicon-Carbide Coating");
    public static final DeferredItem<Item> HIGH_PURITY_GRAPHITE = add("high_purity_graphite", "High-Purity Graphite");
    public static final DeferredItem<Item> CARBON_THREAD = add("carbon_thread", "Carbon Thread");
    // Silicon nebula
    public static final DeferredItem<Item> SILICON_GRIT = add("silicon_grit", "Silicon Grit");
    public static final DeferredItem<Item> PURE_SILICON_DUST = add("pure_silicon_dust", "Pure Silicon Dust");
    public static final DeferredItem<Item> QUARTZ_WAFER = add("quartz_wafer", "Quartz Wafer");
    public static final DeferredItem<Item> DOPED_SILICON_INGOT = add("doped_silicon_ingot", "Doped Silicon Ingot");
    public static final DeferredItem<Item> HIGH_EFFICIENCY_SOLAR_CELL = add("high_efficiency_solar_cell", "High-Efficiency Solar Cell");
    public static final DeferredItem<Item> ADVANCED_SENSOR_UNIT = add("advanced_sensor_unit", "Advanced Sensor Unit");
    // Metallic nebula
    public static final DeferredItem<Item> METALLIC_GRAINS = add("metallic_grains", "Metallic Grains");
    public static final DeferredItem<Item> REFINED_METAL_NODULE = add("refined_metal_nodule", "Refined Metal Nodule");
    public static final DeferredItem<Item> MICRO_ALLOY_SHEET = add("micro_alloy_sheet", "Micro-Alloy Sheet");
    // Cross-nebula machinery
    public static final DeferredItem<Item> HIGH_OUTPUT_SOLAR_SAIL = add("high_output_solar_sail", "High-Output Solar Sail");

    /** The transitional item of the solar sail's Sequenced Assembly recipe. */
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_SOLAR_SAIL = ModItems.ITEMS.register("incomplete_high_output_solar_sail",
            () -> new SequencedAssemblyItem(new Item.Properties()));

    private ModMaterials() {}

    private static DeferredItem<Item> add(String id, String name) {
        DeferredItem<Item> item = ModItems.ITEMS.registerSimpleItem(id);
        ALL.add(new Material(id, name, item));
        return item;
    }

    /** Loads the class so its items are registered before the item registry event. */
    public static void init() {}
}
