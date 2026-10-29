package com.voidreclaimer.client.input;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.network.TetherRecallPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the recall key (G) to the server on press and release, and counts the hold for the HUD. */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ClientTetherController {
    private static boolean held;
    private static int holdTicks;

    private ClientTetherController() {}

    /** Ticks the recall key has been held (0 when released). */
    public static int holdTicks() {
        return holdTicks;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            // Disconnected while holding the key: start clean on the next connection.
            held = false;
            holdTicks = 0;
            return;
        }
        boolean down = mc.player != null && mc.screen == null && ModKeyMappings.RECALL.isDown();
        holdTicks = down ? holdTicks + 1 : 0;
        if (down != held && mc.getConnection() != null) {
            held = down;
            PacketDistributor.sendToServer(new TetherRecallPayload(down));
        }
    }
}
