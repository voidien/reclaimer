package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.screen.MilestoneToast;
import com.voidreclaimer.client.screen.ReplicatorScreen;
import com.voidreclaimer.api.milestone.Milestone;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.replicator.ReplicatorData;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opens the Replicator screen (R) and toasts newly reached milestones. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientReplicatorController {
    private ClientReplicatorController() {}

    /** Installed as {@link ReplicatorData#clientSyncListener}. */
    public static void onSync(ReplicatorData previous, ReplicatorData current) {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceKey<Milestone> milestone : ReplicatorData.newlyUnlocked(previous, current)) {
            mc.execute(() -> mc.getToasts().addToast(new MilestoneToast(milestone)));
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (ModKeyMappings.ASSEMBLY.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new com.voidreclaimer.client.screen.AssemblyScreen());
            }
        }
        while (ModKeyMappings.REPLICATOR.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ReplicatorScreen());
            }
        }
    }
}
