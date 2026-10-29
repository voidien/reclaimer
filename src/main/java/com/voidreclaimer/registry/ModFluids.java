package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The fluids of the nebula chains. They live in Create tanks, pipes, basins and spouts; they have no block, so they can't
 * be placed in the world, and each has a bucket. All use vanilla water's textures, tinted.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, VoidReclaimer.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, VoidReclaimer.MOD_ID);

    /** One fluid: names, tint (ARGB) and its registrations. */
    public record Entry(String id, String name, int tint, DeferredHolder<FluidType, FluidType> type, Supplier<Fluid> source,
                        DeferredItem<BucketItem> bucket) {}

    public static final List<Entry> ALL = new ArrayList<>();

    public static final Entry PURIFIED_WATER = fluid("purified_water", "Purified Water", 0xFFA8DCFF, 1000, 1000, 0);
    public static final Entry OXYGEN_GAS = fluid("oxygen_gas", "Oxygen Gas", 0xFFB4C8FF, -1000, 200, 0);
    public static final Entry HYDROGEN_GAS = fluid("hydrogen_gas", "Hydrogen Gas", 0xFFFFC8C8, -1000, 200, 0);
    public static final Entry NEBULITE_PROPELLANT = fluid("nebulite_propellant", "Nebulite Propellant", 0xFFFF9638, 900, 1500, 8);
    public static final Entry COOLANT_FLUID = fluid("coolant_fluid", "Coolant Fluid", 0xFF78DCE6, 1100, 1200, 0);
    public static final Entry LIQUID_SILICON = fluid("liquid_silicon", "Liquid Silicon", 0xFFD0D8EE, 2600, 4000, 10);

    private ModFluids() {}

    private static Entry fluid(String id, String name, int tint, int density, int viscosity, int light) {
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + VoidReclaimer.MOD_ID + "." + id)
                .density(density).viscosity(viscosity).lightLevel(light)));
        Supplier<Fluid> source = DeferredHolder.create(ResourceKey.create(Registries.FLUID, VoidReclaimer.id(id)));
        Supplier<Fluid> flowing = DeferredHolder.create(ResourceKey.create(Registries.FLUID, VoidReclaimer.id(id + "_flowing")));
        Supplier<Item> bucketSupplier = () -> BucketHolder.get(id);
        FLUIDS.register(id, () -> new BaseFlowingFluid.Source(properties(type, source, flowing, bucketSupplier)));
        FLUIDS.register(id + "_flowing", () -> new BaseFlowingFluid.Flowing(properties(type, source, flowing, bucketSupplier)));
        DeferredItem<BucketItem> bucket = ModItems.ITEMS.register(id + "_bucket",
                () -> new BucketItem(source.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        Entry entry = new Entry(id, name, tint, type, source, bucket);
        ALL.add(entry);
        return entry;
    }

    private static BaseFlowingFluid.Properties properties(Supplier<? extends FluidType> type, Supplier<? extends Fluid> source,
                                                          Supplier<? extends Fluid> flowing, Supplier<? extends Item> bucket) {
        return new BaseFlowingFluid.Properties(type, source, flowing).bucket(bucket);
    }

    /** Buckets by fluid id, for the fluid properties (which are built before the bucket items exist). */
    private static final class BucketHolder {
        static Item get(String id) {
            for (Entry entry : ALL) {
                if (entry.id().equals(id)) {
                    return entry.bucket().get();
                }
            }
            return Items.BUCKET;
        }
    }
}
