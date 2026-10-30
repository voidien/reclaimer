package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyInventoryMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VoidReclaimer.MOD_ID);

    /** The body's inventory: the slots its storage unlocks, in place of the vanilla inventory (no armor or crafting slots). */
    public static final Supplier<MenuType<BodyInventoryMenu>> BODY_INVENTORY = MENUS.register("body_inventory",
            () -> new MenuType<>(BodyInventoryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
