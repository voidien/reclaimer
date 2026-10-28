package com.voidreclaimer.client.render;

import com.voidreclaimer.ClientConfig;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.FlightPhysics;
import com.voidreclaimer.flight.ThrusterFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Optional camera sway (off by default): the view rolls slightly against sideways acceleration and dips or lifts
 * with forward acceleration and braking, then settles. Purely visual; the player's real rotation never changes.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class CameraSway {
    private static final float MAX_ROLL_DEGREES = 4f;
    private static final float MAX_PITCH_DEGREES = 2.5f;
    /** Degrees per (blocks/tick²) of acceleration. */
    private static final float ROLL_GAIN = 120f;
    private static final float PITCH_GAIN = 70f;
    private static final float SMOOTHING = 0.15f;

    private static Vec3 lastVelocity = Vec3.ZERO;
    private static float roll;
    private static float pitch;
    private static float prevRoll;
    private static float prevPitch;

    private CameraSway() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        prevRoll = roll;
        prevPitch = pitch;
        float targetRoll = 0f;
        float targetPitch = 0f;
        if (player != null && ClientConfig.SWAY_ENABLED.get() && ThrusterFlight.isActive(player)) {
            Vec3 velocity = player.getDeltaMovement();
            Vec3 acceleration = velocity.subtract(lastVelocity);
            lastVelocity = velocity;
            Vec3 forward = FlightPhysics.viewVector(player.getXRot(), player.getYRot());
            Vec3 right = forward.cross(FlightPhysics.viewVector(player.getXRot() - 90f, player.getYRot()));
            float strength = ClientConfig.SWAY_STRENGTH.get().floatValue();
            targetRoll = Mth.clamp((float) acceleration.dot(right) * ROLL_GAIN * strength, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
            targetPitch = Mth.clamp((float) -acceleration.dot(forward) * PITCH_GAIN * strength, -MAX_PITCH_DEGREES, MAX_PITCH_DEGREES);
        } else if (player != null) {
            lastVelocity = player.getDeltaMovement();
        }
        roll += (targetRoll - roll) * SMOOTHING;
        pitch += (targetPitch - pitch) * SMOOTHING;
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (roll == 0f && pitch == 0f && prevRoll == 0f && prevPitch == 0f) {
            return;
        }
        float partial = (float) event.getPartialTick();
        event.setRoll(event.getRoll() + Mth.lerp(partial, prevRoll, roll));
        event.setPitch(event.getPitch() + Mth.lerp(partial, prevPitch, pitch));
    }
}
