package com.voidreclaimer.client.camera;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.input.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Photo mode: the photo-mode key detaches the camera from the body and hides the HUD. {@link PhotoModeScreen} reads the input
 * (mouse drag to look, WASD, Space and Shift to move, scroll for the field of view, Q and E to roll), the game keeps running,
 * and {@code CameraPhotoMixin} puts the camera where {@link PhotoState} says; the roll and field of view go through NeoForge's
 * viewport events.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class PhotoMode {
    private static final PhotoState STATE = new PhotoState();
    private static boolean active;
    private static boolean hudWasHidden;

    private PhotoMode() {}

    public static boolean active() {
        return active;
    }

    public static PhotoState state() {
        return STATE;
    }

    public static Vec3 position() {
        return STATE.position();
    }

    /** Starts at the current camera and opens the control screen. */
    public static void enter(Minecraft mc) {
        if (active || mc.player == null) {
            return;
        }
        var camera = mc.gameRenderer.getMainCamera();
        STATE.start(camera.getPosition(), camera.getYRot(), camera.getXRot(), mc.options.fov().get());
        hudWasHidden = mc.options.hideGui;
        mc.options.hideGui = true;
        active = true;
        mc.setScreen(new PhotoModeScreen());
    }

    /** Called by the screen when it closes. */
    public static void exit(Minecraft mc) {
        if (!active) {
            return;
        }
        active = false;
        mc.options.hideGui = hudWasHidden;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            if (active) {
                exit(mc);
            }
            return;
        }
        while (ModKeyMappings.PHOTO_MODE.consumeClick()) {
            if (!active && mc.screen == null) {
                enter(mc);
            }
        }
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (active) {
            event.setRoll(STATE.roll());
        }
    }

    @SubscribeEvent
    static void onFov(ViewportEvent.ComputeFov event) {
        if (active) {
            event.setFOV(STATE.fov());
        }
    }
}
