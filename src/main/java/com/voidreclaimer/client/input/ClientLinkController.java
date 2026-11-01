package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.network.UnlinkPayload;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Leave Shell key: two presses, see {@link LeaveConfirm}. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientLinkController {
    private static final LeaveConfirm CONFIRM = new LeaveConfirm();

    private ClientLinkController() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        while (ModKeyMappings.UNLINK.consumeClick()) {
            // The home dust cap is set only while linked: out of a shell the key has nothing to do.
            if (player == null || player.getData(ModAttachments.HOME_DUST_CAPACITY) < 0f) {
                continue;
            }
            if (CONFIRM.press(player.tickCount)) {
                PacketDistributor.sendToServer(new UnlinkPayload());
            } else {
                player.displayClientMessage(Component.translatable("message.void_reclaimer.link.leave_confirm"), true);
            }
        }
    }
}
