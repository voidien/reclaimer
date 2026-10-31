package com.voidreclaimer.debug;

import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.link.BodySnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Counts items, by kind, so that a test or a person can say nothing appeared or vanished on the way from one place to another.
 * It counts everything a body carries: armor, offhand, the stack on the cursor, and the sections, modules and pending items of
 * the body itself. Dropped items are only counted when asked for, with {@link #nearby}.
 */
public record ItemCensus(Map<ResourceLocation, Integer> counts) {
    public static ItemCensus of(ServerPlayer player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            add(counts, inventory.getItem(slot));
        }
        add(counts, player.containerMenu.getCarried());
        add(counts, Bodies.data(player));
        return new ItemCensus(counts);
    }

    public static ItemCensus of(BodySnapshot snapshot, HolderLookup.Provider registries) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        ListTag items = snapshot.items();
        for (int i = 0; i < items.size(); i++) {
            add(counts, ItemStack.parse(registries, items.getCompound(i)).orElse(ItemStack.EMPTY));
        }
        add(counts, snapshot.body());
        return new ItemCensus(counts);
    }

    /** Items lying within {@code range} blocks of {@code player}: a stack on the floor has not vanished. Entity lookups only see ticking chunks. */
    public static ItemCensus nearby(ServerPlayer player, double range) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        AABB area = player.getBoundingBox().inflate(range);
        for (ItemEntity item : player.serverLevel().getEntitiesOfClass(ItemEntity.class, area)) {
            add(counts, item.getItem());
        }
        return new ItemCensus(counts);
    }

    /** Both counted together: a swap moves things between two places, so only the total has to stay the same. */
    public ItemCensus plus(ItemCensus other) {
        Map<ResourceLocation, Integer> total = new HashMap<>(counts);
        other.counts.forEach((item, count) -> total.merge(item, count, Integer::sum));
        return new ItemCensus(total);
    }

    /** How many items in all. */
    public int total() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** What is different from {@code earlier}, kind by kind in a fixed order: positive for items that appeared, negative for items that vanished. */
    public Map<ResourceLocation, Integer> since(ItemCensus earlier) {
        Map<ResourceLocation, Integer> changes = new TreeMap<>();
        counts.forEach((item, count) -> changes.put(item, count));
        earlier.counts.forEach((item, count) -> changes.merge(item, -count, Integer::sum));
        changes.values().removeIf(change -> change == 0);
        return changes;
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
