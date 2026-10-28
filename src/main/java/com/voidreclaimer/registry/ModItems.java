package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustSensorItem;
import com.voidreclaimer.salvage.SalvageToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidReclaimer.MOD_ID);

    public static final DeferredItem<DustSensorItem> DUST_SENSOR = ITEMS.register("dust_sensor",
            () -> new DustSensorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> SCAFFOLD = ITEMS.registerSimpleBlockItem(ModBlocks.SCAFFOLD);

    public static final DeferredItem<SalvageToolItem> SALVAGE_TOOL = ITEMS.register("salvage_tool",
            () -> new SalvageToolItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
