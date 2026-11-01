package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.network.UnlinkPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Leave Shell key. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientLinkController {
    private ClientLinkController() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyMappings.UNLINK.consumeClick()) {
            PacketDistributor.sendToServer(new UnlinkPayload());
        }
    }
}
