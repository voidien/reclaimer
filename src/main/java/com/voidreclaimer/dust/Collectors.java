package com.voidreclaimer.dust;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.replicator.Replicator;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.replicator.ReplicatorLogic;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Raw-item collectors: modules whose {@link ModuleStats.Collector} spec turns dust gathered in a cloud into raw items
 * as well. {@link DustTicker} calls {@link #collect} with the dust a tick gained, so the yield follows cloud density,
 * distance flown and the body's dust intake exactly like dust does.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class Collectors {
    /** Fractions of an item not yet produced, per player and item. Lost on logout; under one item each. */
    private static final Map<UUID, Map<ResourceLocation, Float>> CARRY = new HashMap<>();

    private Collectors() {}

    /** Whole items now due, and the fraction left over. */
    public record Step(int whole, float carry) {}

    /** Adds {@code produced} to {@code carry} and takes out the whole items. */
    public static Step accumulate(float carry, float produced) {
        float total = carry + Math.max(0f, produced);
        int whole = (int) Math.floor(total);
        return new Step(whole, total - whole);
    }

    /** Items produced from {@code dustGain} dust by a module with {@code yield}, under the config multiplier. */
    public static float produced(float dustGain, float yield, double multiplier) {
        return (float) (dustGain * yield * multiplier);
    }

    /**
     * Runs every installed collector for dust of {@code kind} just gathered. Does nothing in low power, with the
     * collectors switched off in config, or when neither the inventory nor the output buffer can take more.
     */
    public static void collect(ServerPlayer player, ResourceLocation kind, float dustGain) {
        if (!Config.COLLECTOR_ENABLED.get() || dustGain <= 0f || !Energy.canPowerSystems(player)) {
            return;
        }
        for (ItemStack module : Bodies.data(player).modules().values()) {
            ModuleStats stats = Parts.module(module);
            ResourceLocation output = stats == null || stats.collector().isEmpty() ? null : stats.collector().get().outputs().get(kind);
            if (output == null || !hasRoom(player)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(output);
            if (item == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            ModuleStats.Collector collector = stats.collector().get();
            Energy.consume(player, collector.energyPerSecond() / 20f);
            Map<ResourceLocation, Float> carries = CARRY.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
            Step step = accumulate(carries.getOrDefault(output, 0f), produced(dustGain, collector.yield(), Config.COLLECTOR_YIELD.get()));
            carries.put(output, step.carry());
            if (step.whole() > 0) {
                give(player, new ItemStack(item, step.whole()));
            }
        }
    }

    private static boolean hasRoom(ServerPlayer player) {
        return player.getInventory().getFreeSlot() >= 0 || Replicator.data(player).buffer().size() < ReplicatorData.BUFFER_SLOTS;
    }

    /** To the inventory; what doesn't fit goes to the Replicator's output buffer. */
    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            Replicator.store(player, ReplicatorLogic.toBuffer(Replicator.data(player), stack));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CARRY.remove(event.getEntity().getUUID());
    }
}
