package com.voidreclaimer.compat.cc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/** Turns our state into the plain tables CC hands to Lua (maps, lists, numbers and strings). */
final class LuaValues {
    private LuaValues() {}

    /** Slot number (1-based, as Lua counts) to {name, count}; empty slots are left out. */
    static Map<Integer, Map<String, Object>> items(IItemHandler handler) {
        Map<Integer, Map<String, Object>> slots = new LinkedHashMap<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                slots.put(slot + 1, item(stack));
            }
        }
        return slots;
    }

    static Map<String, Object> item(ItemStack stack) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        item.put("count", stack.getCount());
        return item;
    }

    /** One {name, amount, capacity} per tank, in order; an empty tank has name "minecraft:empty". */
    static List<Map<String, Object>> tanks(FluidTank... tanks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FluidTank tank : tanks) {
            FluidStack fluid = tank.getFluid();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            entry.put("amount", fluid.getAmount());
            entry.put("capacity", tank.getCapacity());
            list.add(entry);
        }
        return list;
    }
}
