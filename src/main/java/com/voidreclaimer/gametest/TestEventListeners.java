package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.BodyChangedEvent;
import com.voidreclaimer.api.event.BodyStatsEvent;
import com.voidreclaimer.api.event.DustGainEvent;
import com.voidreclaimer.api.event.ModuleUseEvent;
import com.voidreclaimer.body.BodyStats;
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

    /** Body stats get +10 max health. */
    public static final String TOUGH = "vr_test_tough";
    /** Tool module use is cancelled. */
    public static final String NO_MODULES = "vr_test_no_modules";

    /** Body changes per player, for every player. */
    static final Map<UUID, Integer> BODY_CHANGES = new ConcurrentHashMap<>();

    /** Milestones reached per player, in order, for every player. */
    static final Map<UUID, List<ResourceKey<Milestone>>> REACHED = new ConcurrentHashMap<>();

    /** Mass Drivers whose base stands at this X turn every launch into five trusses instead of the payload's own count. */
    public static final int FIVE_TRUSS_DRIVER_X = 89_256;
    /** Mass Drivers whose base stands at this X launch nothing (the event is cancelled). */
    public static final int CANCELLED_DRIVER_X = 89_288;

    private TestEventListeners() {}

    @SubscribeEvent
    static void onMassDriverLaunch(com.voidreclaimer.api.event.MassDriverLaunchEvent event) {
        if (event.getDriverPos().getX() == FIVE_TRUSS_DRIVER_X) {
            event.setSatellites(0);
            event.setTrusses(5);
        } else if (event.getDriverPos().getX() == CANCELLED_DRIVER_X) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onDustGain(DustGainEvent event) {
        if (event.getEntity().getTags().contains(NO_DUST)) {
            event.setCanceled(true);
        } else if (event.getEntity().getTags().contains(FIXED_DUST)) {
            event.setAmount(1f);
        }
    }

    @SubscribeEvent
    static void onBodyStats(BodyStatsEvent event) {
        if (event.getEntity().getTags().contains(TOUGH)) {
            BodyStats s = event.getStats();
            event.setStats(new BodyStats(s.size(), s.scale(), s.sizeRank(), s.mass(), s.massBudget(), s.coreTier(), s.flight(),
                    s.energyUse(), s.energyCapacity(), s.solarRate(), s.maxHealth() + 10f, s.reach(), s.dustIntake(), s.rows(),
                    s.dustCapacity(), s.stability(), s.turnRate()));
        }
    }

    @SubscribeEvent
    static void onBodyChanged(BodyChangedEvent event) {
        BODY_CHANGES.merge(event.getEntity().getUUID(), 1, Integer::sum);
    }

    @SubscribeEvent
    static void onModuleUse(ModuleUseEvent event) {
        if (event.getEntity().getTags().contains(NO_MODULES)) {
            event.setCanceled(true);
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
