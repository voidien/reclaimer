package com.voidreclaimer.body;

import com.voidreclaimer.registry.ModDataComponents;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

/** Reads part stats from item stacks. */
public final class Parts {
    private Parts() {}

    @Nullable
    public static PartStats stats(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.PART_STATS.get());
    }

    public static boolean isPart(ItemStack stack, PartKind kind) {
        PartStats stats = stats(stack);
        return stats != null && stats.kind() == kind;
    }
}
