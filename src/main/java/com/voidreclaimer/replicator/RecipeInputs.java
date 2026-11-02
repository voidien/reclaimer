package com.voidreclaimer.replicator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Finds the items a recipe takes in a row of inventory slots. Nothing is taken until the whole recipe can be paid. */
public final class RecipeInputs {
    /** What a recipe would take: how many from each slot, and the items as they are, wear and all, to give back on a cancel. */
    public record Plan(Map<Integer, Integer> slots, List<ItemStack> items) {
        public void take(List<ItemStack> inventory) {
            slots.forEach((slot, count) -> inventory.get(slot).shrink(count));
        }
    }

    public static final Plan NOTHING = new Plan(Map.of(), List.of());

    private RecipeInputs() {}

    /** Where a recipe looks: the main inventory, then the offhand, then armor, so something worn goes last. */
    public static List<ItemStack> slots(Inventory inventory) {
        List<ItemStack> slots = new ArrayList<>(inventory.items);
        slots.addAll(inventory.offhand);
        slots.addAll(inventory.armor);
        return slots;
    }

    /** How many of the items {@code input} takes are in {@code inventory}. */
    public static int held(List<ItemStack> inventory, SizedIngredient input) {
        int held = 0;
        for (ItemStack stack : inventory) {
            if (input.ingredient().test(stack)) {
                held += stack.getCount();
            }
        }
        return held;
    }

    /** How to pay {@code inputs} from {@code inventory}, or null when something is missing. Changes nothing. */
    @Nullable
    public static Plan plan(List<ItemStack> inventory, List<SizedIngredient> inputs) {
        if (inputs.isEmpty()) {
            return NOTHING;
        }
        int[] spare = inventory.stream().mapToInt(ItemStack::getCount).toArray();
        Map<Integer, Integer> slots = new LinkedHashMap<>();
        List<ItemStack> items = new ArrayList<>();
        for (SizedIngredient input : inputs) {
            int needed = input.count();
            for (int slot = 0; slot < spare.length && needed > 0; slot++) {
                ItemStack stack = inventory.get(slot);
                if (spare[slot] > 0 && input.ingredient().test(stack)) {
                    int taken = Math.min(needed, spare[slot]);
                    spare[slot] -= taken;
                    needed -= taken;
                    slots.merge(slot, taken, Integer::sum);
                    items.add(stack.copyWithCount(taken));
                }
            }
            if (needed > 0) {
                return null;
            }
        }
        return new Plan(slots, items);
    }
}
