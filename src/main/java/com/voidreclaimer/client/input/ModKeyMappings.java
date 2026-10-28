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

    private ModKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_DAMPENER);
        event.register(TOGGLE_CREATIVE_THRUSTERS);
        event.register(SENSOR_PULSE);
    }
}
