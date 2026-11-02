package com.voidreclaimer.client.hud;

import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.weather.WeatherState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** A line at the top of the screen while weather is coming or here. Every level has its own, so the sky over deep space says nothing about Earth. */
public final class WeatherLayer {
    private static final int TOP = 4;
    private static final int OPAQUE = 0xFF000000;

    private WeatherLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null) {
            return;
        }
        WeatherState state = mc.level.getData(ModAttachments.WEATHER);
        if (state.weather().isEmpty()) {
            return;
        }
        var weather = mc.level.registryAccess().registryOrThrow(VoidReclaimerRegistries.SPACE_WEATHER).getOptional(state.weather().get());
        if (weather.isEmpty()) {
            return;
        }
        Component name = weather.get().name().orElse(Component.translatable("hud.void_reclaimer.weather.unnamed"));
        Component line = state.warning() ? Component.translatable("hud.void_reclaimer.weather.warning", name, state.warningLeft())
                : Component.translatable("hud.void_reclaimer.weather.active", name, state.activeLeft());
        graphics.drawCenteredString(mc.font, line, graphics.guiWidth() / 2, TOP, OPAQUE | weather.get().color());
    }
}
