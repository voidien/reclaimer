package com.voidreclaimer.client.camera;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.input.ClientPilotController;
import com.voidreclaimer.ship.HelmSeat;
import com.voidreclaimer.ship.ShipCameraMath;
import com.voidreclaimer.ship.Ships;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The camera rides the ship: while the player sits at a Helm, the view rolls with the ship so its deck stays level on
 * screen (Sable already carries the pilot along; vanilla's camera only ever knows yaw and pitch). The mouse still looks
 * where it is pointed, the orbit camera still swings round the body, and photo mode, which owns the roll, is left alone.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class ShipCamera {
    private static float lastRoll;

    private ShipCamera() {}

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        HelmSeat seat = player == null ? null : ClientPilotController.seat(player);
        if (seat == null || mc.level == null || PhotoMode.active()) {
            lastRoll = 0f;
            return;
        }
        SubLevel ship = Ships.shipAt(mc.level, seat.helmPos());
        if (!(ship instanceof ClientSubLevel client)) {
            lastRoll = 0f;
            return;
        }
        lastRoll = ShipCameraMath.roll(client.renderPose((float) event.getPartialTick()).orientation(), event.getYaw(), event.getPitch(), lastRoll);
        event.setRoll(event.getRoll() + lastRoll);
    }
}
