package com.voidreclaimer.body;

import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.ToolModuleType;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModRegistries;
import net.minecraft.resources.ResourceKey;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

/** Reads section, module and tool data components from item stacks. */
public final class Parts {
    private Parts() {}

    @Nullable
    public static SectionStats section(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.SECTION.get());
    }

    @Nullable
    public static ModuleStats module(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.MODULE.get());
    }

    public static boolean isModule(ItemStack stack, ModuleKind kind) {
        ModuleStats stats = module(stack);
        return stats != null && stats.kind() == kind;
    }

    /** What the tool module does, or null if it isn't one. */
    @Nullable
    public static ToolModuleType toolType(ItemStack stack) {
        ResourceKey<ToolModuleType> key = stack.isEmpty() ? null : stack.get(ModDataComponents.TOOL_MODULE.get());
        return key == null ? null : ModRegistries.TOOL_MODULE_TYPES.get(key);
    }
}
