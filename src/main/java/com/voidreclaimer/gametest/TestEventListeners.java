package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.DustGainEvent;
import com.voidreclaimer.api.event.MilestoneReachedEvent;
import com.voidreclaimer.api.event.ReplicatorJobFinishedEvent;
import com.voidreclaimer.api.milestone.Milestone;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Listeners standing in for an addon's. They only touch test players that carry one of the tags below, so other
 * tests see the mod's normal behaviour.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class TestEventListeners {
    /** Dust gain is replaced by exactly 1 per tick. */
    public static final String FIXED_DUST = "vr_test_fixed_dust";
    /** Dust gain is cancelled. */
    public static final String NO_DUST = "vr_test_no_dust";
    /** Finished Replicator jobs also deliver a diamond. */
    public static final String BONUS_OUTPUT = "vr_test_bonus_output";

    /** Milestones reached per player, in order, for every player. */
    static final Map<UUID, List<ResourceKey<Milestone>>> REACHED = new ConcurrentHashMap<>();

    private TestEventListeners() {}

    @SubscribeEvent
    static void onDustGain(DustGainEvent event) {
        if (event.getEntity().getTags().contains(NO_DUST)) {
            event.setCanceled(true);
        } else if (event.getEntity().getTags().contains(FIXED_DUST)) {
            event.setAmount(1f);
        }
    }

    @SubscribeEvent
    static void onMilestone(MilestoneReachedEvent event) {
        REACHED.computeIfAbsent(event.getEntity().getUUID(), id -> new CopyOnWriteArrayList<>()).add(event.getMilestone());
    }

    @SubscribeEvent
    static void onJobFinished(ReplicatorJobFinishedEvent event) {
        if (event.getEntity().getTags().contains(BONUS_OUTPUT)) {
            event.getOutputs().add(new ItemStack(Items.DIAMOND));
        }
    }
}
