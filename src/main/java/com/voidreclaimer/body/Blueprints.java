package com.voidreclaimer.body;

import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorLogic;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Saving a body to a Blueprint, building it from the inventory, and queueing what's missing in the Replicator. */
public final class Blueprints {
    private Blueprints() {}

    /** Saves the current body onto the blueprint item. */
    public static void save(ServerPlayer player, ItemStack blueprint) {
        blueprint.set(ModDataComponents.BLUEPRINT.get(), Blueprint.of(Bodies.data(player)));
        player.displayClientMessage(Component.translatable("message.void_reclaimer.blueprint.saved").withStyle(ChatFormatting.AQUA), true);
    }

    /**
     * Builds the design with what's in the inventory: the core section first (which may switch the layout), then the
     * other sections, then modules, then the looks. Parts that are already right stay. Returns what was missing.
     */
    public static Map<Item, Integer> apply(ServerPlayer player, Blueprint blueprint) {
        Map<Item, Integer> missing = new LinkedHashMap<>();
        BodyData body = Bodies.data(player);
        if (!Blueprint.wants(blueprint.core(), body.core())) {
            install(player, blueprint.core(), slot -> Bodies.installSection(player, Bodies.data(player).coreSocket(), slot), missing);
        }
        blueprint.sections().forEach((socket, item) -> {
            if (!socket.equals(blueprint.coreSocket()) && !Blueprint.wants(item, Bodies.data(player).section(socket))) {
                install(player, item, slot -> Bodies.installSection(player, socket, slot), missing);
            }
        });
        blueprint.modules().forEach((mount, item) -> {
            if (!Blueprint.wants(item, Bodies.data(player).module(mount))) {
                install(player, item, slot -> Bodies.installModule(player, mount, slot), missing);
            }
        });
        Bodies.setCosmetics(player, blueprint.cosmetics());
        player.displayClientMessage(report(missing), false);
        return missing;
    }

    /**
     * Queues the cheapest unlocked Replicator recipe for each missing part, until the queue is full. Returns how many
     * jobs were queued.
     */
    public static int queueMissing(ServerPlayer player, Blueprint blueprint) {
        Map<Item, Integer> missing = missing(player, blueprint);
        int queued = 0;
        List<RecipeHolder<ReplicatorRecipe>> recipes = player.serverLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.REPLICATOR.get());
        for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
            Optional<RecipeHolder<ReplicatorRecipe>> recipe = recipes.stream()
                    .filter(h -> h.value().count(entry.getKey()) > 0)
                    .filter(h -> Replicator.data(player).isUnlocked(h.value().milestone()))
                    // Cheapest per wanted part, so a missing hull queues the hull, not a whole kit.
                    .min(Comparator.comparingDouble(h -> h.value().cost().total() / h.value().count(entry.getKey())));
            if (recipe.isEmpty()) {
                continue;
            }
            int perJob = recipe.get().value().count(entry.getKey());
            for (int made = 0; made < entry.getValue(); made += perJob) {
                if (Replicator.queue(player, recipe.get().id()) != ReplicatorLogic.QueueResult.QUEUED) {
                    break;
                }
                queued++;
            }
        }
        player.displayClientMessage(Component.translatable("message.void_reclaimer.blueprint.queued", queued).withStyle(ChatFormatting.AQUA), true);
        return queued;
    }

    /** Parts the design needs that neither the body nor the inventory has. */
    public static Map<Item, Integer> missing(ServerPlayer player, Blueprint blueprint) {
        BodyData body = Bodies.data(player);
        Map<Item, Integer> needed = new LinkedHashMap<>();
        blueprint.sections().forEach((socket, item) -> {
            boolean have = socket.equals(blueprint.coreSocket()) ? Blueprint.wants(item, body.core()) : Blueprint.wants(item, body.section(socket));
            if (!have) {
                needed.merge(item, 1, Integer::sum);
            }
        });
        blueprint.modules().forEach((mount, item) -> {
            if (!Blueprint.wants(item, body.module(mount))) {
                needed.merge(item, 1, Integer::sum);
            }
        });
        Inventory inventory = player.getInventory();
        Map<Item, Integer> missing = new LinkedHashMap<>();
        needed.forEach((item, count) -> {
            int short_ = count - inventory.countItem(item);
            if (short_ > 0) {
                missing.put(item, short_);
            }
        });
        return missing;
    }

    private interface Installer {
        BodyRules.Check install(int slot);
    }

    private static void install(ServerPlayer player, net.minecraft.world.item.Item item, Installer installer, Map<Item, Integer> missing) {
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(item));
        if (slot < 0 || slot >= player.getInventory().items.size()) {
            missing.merge(item, 1, Integer::sum);
            return;
        }
        installer.install(slot);
    }

    private static Component report(Map<Item, Integer> missing) {
        if (missing.isEmpty()) {
            return Component.translatable("message.void_reclaimer.blueprint.applied").withStyle(ChatFormatting.AQUA);
        }
        MutableComponent list = Component.empty();
        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(missing.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(Component.translatable("message.void_reclaimer.blueprint.count", entries.get(i).getValue(),
                    entries.get(i).getKey().getDescription()));
        }
        return Component.translatable("message.void_reclaimer.blueprint.missing", list).withStyle(ChatFormatting.GOLD);
    }
}
