package com.voidreclaimer.gametest;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.link.BodySnapshot;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Counts items, by kind, so a test can say that nothing appeared or vanished on the way from one place to another. It counts
 * everything a body carries: armor, offhand, the stack on the cursor, and the sections, modules and pending items of the body
 * itself. It does not count items lying in the world.
 */
record ItemCensus(Map<ResourceLocation, Integer> counts) {
    static ItemCensus of(ServerPlayer player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            add(counts, inventory.getItem(slot));
        }
        add(counts, player.containerMenu.getCarried());
        add(counts, Bodies.data(player));
        return new ItemCensus(counts);
    }

    static ItemCensus of(BodySnapshot snapshot, HolderLookup.Provider registries) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        ListTag items = snapshot.items();
        for (int i = 0; i < items.size(); i++) {
            add(counts, ItemStack.parse(registries, items.getCompound(i)).orElse(ItemStack.EMPTY));
        }
        add(counts, snapshot.body());
        return new ItemCensus(counts);
    }

    /** Both counted together: a swap moves things between two places, so only the total has to stay the same. */
    ItemCensus plus(ItemCensus other) {
        Map<ResourceLocation, Integer> total = new HashMap<>(counts);
        other.counts.forEach((item, count) -> total.merge(item, count, Integer::sum));
        return new ItemCensus(total);
    }

    private static void add(Map<ResourceLocation, Integer> counts, BodyData body) {
        body.sections().values().forEach(stack -> add(counts, stack));
        body.modules().values().forEach(stack -> add(counts, stack));
        body.pending().forEach(stack -> add(counts, stack));
    }

    private static void add(Map<ResourceLocation, Integer> counts, ItemStack stack) {
        if (!stack.isEmpty()) {
            counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
        }
    }
}
