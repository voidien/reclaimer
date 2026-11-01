package com.voidreclaimer.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.void_reclaimer";

    public static final KeyMapping TOGGLE_DAMPENER = new KeyMapping("key.void_reclaimer.toggle_dampener",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping TOGGLE_CREATIVE_THRUSTERS = new KeyMapping("key.void_reclaimer.toggle_creative_thrusters",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping SENSOR_PULSE = new KeyMapping("key.void_reclaimer.sensor_pulse",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);

    public static final KeyMapping REPLICATOR = new KeyMapping("key.void_reclaimer.replicator",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping RECALL = new KeyMapping("key.void_reclaimer.recall",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    /** Leaves a shell and goes home (H for home). */
    public static final KeyMapping UNLINK = new KeyMapping("key.void_reclaimer.unlink",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping CLOUD_SCANNER = new KeyMapping("key.void_reclaimer.cloud_scanner",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);

    /** Hold to open the module wheel (Left Alt: C is vanilla's save-hotbar key). */
    public static final KeyMapping MODULE_WHEEL = new KeyMapping("key.void_reclaimer.module_wheel",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    /** Hold, in third person, and move the mouse to orbit the camera round the body (middle mouse by default). */
    public static final KeyMapping ORBIT_CAMERA = new KeyMapping("key.void_reclaimer.orbit_camera",
            KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY);

    /** Opens the full-window body assembly view. Unbound by default: the Body tab has a button for it. */
    public static final KeyMapping ASSEMBLY = new KeyMapping("key.void_reclaimer.assembly",
            KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);

    /** Photo mode: a free camera with the HUD hidden. Inside: drag to look, WASD/Space/Shift to move (Ctrl fast), scroll for FOV, Q/E to roll, R resets roll, G debug layer, Enter for a screenshot. */
    public static final KeyMapping PHOTO_MODE = new KeyMapping("key.void_reclaimer.photo_mode",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY);

    public static final KeyMapping TOGGLE_RCS_LOCK = new KeyMapping("key.void_reclaimer.toggle_rcs_lock",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    /** The Helm's rotation axes; its translation axes are the movement keys. */
    public static final KeyMapping ROLL_LEFT = new KeyMapping("key.void_reclaimer.roll_left",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, CATEGORY);
    public static final KeyMapping ROLL_RIGHT = new KeyMapping("key.void_reclaimer.roll_right",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);
    public static final KeyMapping PITCH_UP = new KeyMapping("key.void_reclaimer.pitch_up",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, CATEGORY);
    public static final KeyMapping PITCH_DOWN = new KeyMapping("key.void_reclaimer.pitch_down",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, CATEGORY);
    public static final KeyMapping YAW_LEFT = new KeyMapping("key.void_reclaimer.yaw_left",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, CATEGORY);
    public static final KeyMapping YAW_RIGHT = new KeyMapping("key.void_reclaimer.yaw_right",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, CATEGORY);

    private ModKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DAMPENER);
        event.register(TOGGLE_CREATIVE_THRUSTERS);
        event.register(SENSOR_PULSE);
        event.register(REPLICATOR);
        event.register(RECALL);
        event.register(CLOUD_SCANNER);
        event.register(UNLINK);
        event.register(MODULE_WHEEL);
        event.register(ORBIT_CAMERA);
        event.register(PHOTO_MODE);
        event.register(ASSEMBLY);
        event.register(TOGGLE_RCS_LOCK);
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
        event.register(PITCH_UP);
        event.register(PITCH_DOWN);
        event.register(YAW_LEFT);
        event.register(YAW_RIGHT);
    }
}
