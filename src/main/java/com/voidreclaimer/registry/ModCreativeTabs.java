package com.voidreclaimer.registry;

import com.voidreclaimer.VoidReclaimer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VoidReclaimer.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.void_reclaimer"))
            .icon(() -> ModItems.DUST_SENSOR.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.DUST_SENSOR.get());
                output.accept(ModItems.SCAFFOLD.get());
                output.accept(ModItems.SALVAGE_TOOL.get());
            })
            .build());

    private ModCreativeTabs() {}
}
