package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustSensorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VoidReclaimer.MOD_ID);

    public static final DeferredItem<DustSensorItem> DUST_SENSOR = ITEMS.register("dust_sensor",
            () -> new DustSensorItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}
}
