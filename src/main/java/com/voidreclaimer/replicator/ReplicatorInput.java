package com.voidreclaimer.replicator;

import com.voidreclaimer.dust.DustData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Recipe input for the Replicator: the AI's dust reserve. It has no item slots. */
public record ReplicatorInput(DustData dust) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
