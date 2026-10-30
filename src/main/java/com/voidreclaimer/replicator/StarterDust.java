package com.voidreclaimer.replicator;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.event.MilestoneReachedEvent;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * A new body starts with a little dust (config {@code starter}): enough for the first few Replicator recipes, so the
 * first thing to do is use the Replicator, not hunt for a cloud. It is granted when the Awakening milestone is
 * reached in play, which happens once per player; worlds that already passed it get nothing.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class StarterDust {
    private StarterDust() {}

    @SubscribeEvent
    static void onMilestone(MilestoneReachedEvent event) {
        if (event.getMilestone().equals(Milestones.AWAKENING) && event.getEntity() instanceof ServerPlayer player) {
            grant(player);
        }
    }

    /** Adds the configured starter dust, up to the body's capacity. Returns whether anything was granted. */
    public static boolean grant(ServerPlayer player) {
        if (!Config.STARTER_DUST_ENABLED.get()) {
            return false;
        }
        float capacity = DustData.capacity(player);
        DustData dust = player.getData(ModAttachments.DUST);
        dust = dust.add(DustKinds.METALLIC, Config.STARTER_METALLIC.get().floatValue(), capacity);
        dust = dust.add(DustKinds.SILICATE, Config.STARTER_SILICATE.get().floatValue(), capacity);
        dust = dust.add(DustKinds.ICE, Config.STARTER_ICE.get().floatValue(), capacity);
        dust = dust.add(DustKinds.CARBON, Config.STARTER_CARBON.get().floatValue(), capacity);
        player.setData(ModAttachments.DUST, dust);
        player.syncData(ModAttachments.DUST);
        return true;
    }
}
