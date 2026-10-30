package com.voidreclaimer.client.camera;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.input.ModKeyMappings;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Orbit camera: in third person, hold the orbit key (middle mouse by default) and move the mouse to look around your body, scroll
 * to zoom. The body does not turn; releasing the key eases the camera back. The angles are applied by
 * {@code CameraOrbitMixin} where the camera is positioned.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class OrbitCamera {
    private static final OrbitState STATE = new OrbitState();
    private static boolean held;
    private static double lastX;
    private static double lastY;
    private static boolean haveLast;

    private OrbitCamera() {}

    public static float yawOffset(float partialTick) {
        return STATE.yaw(partialTick);
    }

    public static float pitchOffset(float partialTick) {
        return STATE.pitch(partialTick);
    }

    public static float zoom() {
        return STATE.zoom();
    }

    /** Whether mouse movement currently orbits instead of turning the body. */
    public static boolean orbiting() {
        return held;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean thirdPerson = mc.player != null && mc.screen == null && mc.options.getCameraType() != CameraType.FIRST_PERSON;
        held = thirdPerson && ModKeyMappings.ORBIT_CAMERA.isDown();
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        double dx = haveLast ? x - lastX : 0;
        double dy = haveLast ? y - lastY : 0;
        lastX = x;
        lastY = y;
        haveLast = true;
        if (mc.player == null || !thirdPerson) {
            STATE.reset();
            return;
        }
        STATE.tick(held, dx, dy);
    }

    /** While orbiting the mouse must not turn the body. */
    @SubscribeEvent
    static void onTurn(CalculatePlayerTurnEvent event) {
        if (held) {
            event.setMouseSensitivity(0.0);
        }
    }

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (held) {
            STATE.scroll(event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }
}
