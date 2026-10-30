package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.network.OpenInventoryPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** E opens the body inventory instead of the vanilla inventory (survival and adventure; creative keeps its own screen). */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientInventoryController {
    private ClientInventoryController() {}

    @SubscribeEvent
    static void onScreenOpening(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getNewScreen() instanceof InventoryScreen && mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator()) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(OpenInventoryPayload.INSTANCE);
        }
    }
}
