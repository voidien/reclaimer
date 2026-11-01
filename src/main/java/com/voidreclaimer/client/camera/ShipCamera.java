package com.voidreclaimer.client.camera;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.input.ClientPilotController;
import com.voidreclaimer.ship.HelmBlock;
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
import org.joml.Vector3d;

/**
 * The camera rides the ship: while the player sits at a Helm, the view rolls with the ship so its deck stays level on
 * screen (Sable carries the pilot along, but nothing turns a seated pilot's view, and vanilla's camera only knows yaw and pitch).
 * When the mouse steers the ship the view follows the nose, or the nose would swing off screen. Free Look and aim steering leave the
 * view to the mouse, the orbit camera still swings round the body, and photo mode, which owns the roll, is left alone.
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
        if (ClientPilotController.steersWithMouse(player) && mc.level.getBlockState(seat.helmPos()).hasProperty(HelmBlock.FACING)) {
            Vector3d nose = client.renderPose((float) event.getPartialTick()).orientation()
                    .transform(Ships.unit(mc.level.getBlockState(seat.helmPos()).getValue(HelmBlock.FACING)));
            float yaw = ShipCameraMath.yaw(nose);
            float pitch = ShipCameraMath.pitch(nose);
            player.setYRot(yaw);
            player.yRotO = yaw;
            player.setXRot(pitch);
            player.xRotO = pitch;
            event.setYaw(yaw);
            event.setPitch(pitch);
        }
        lastRoll = ShipCameraMath.roll(client.renderPose((float) event.getPartialTick()).orientation(), event.getYaw(), event.getPitch(), lastRoll);
        event.setRoll(event.getRoll() + lastRoll);
    }
}
