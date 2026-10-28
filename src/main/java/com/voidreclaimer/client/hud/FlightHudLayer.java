package com.voidreclaimer.client.hud;

import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Speed and dampener readout to the right of the hotbar while thrusters are active. */
public final class FlightHudLayer {
    private static final int COLOR_NORMAL = 0x9FE8FF;
    private static final int COLOR_DIM = 0x6F8A99;
    private static final int COLOR_WARNING = 0xFF5050;

    private FlightHudLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player) || !ThrusterFlight.isActive(player)) {
            return;
        }
        Font font = mc.font;
        int x = graphics.guiWidth() / 2 + 96;
        int y = graphics.guiHeight() - 20;

        double speed = player.getDeltaMovement().length() * FlightStats.TICKS_PER_SECOND;
        graphics.drawString(font, Component.translatable("hud.void_reclaimer.speed", String.format("%.1f", speed)), x, y, COLOR_NORMAL);

        Component status;
        int color;
        if (!Energy.canPowerSystems(player)) {
            status = Component.translatable("hud.void_reclaimer.thrusters_offline");
            color = COLOR_WARNING;
        } else if (player.getData(ModAttachments.FLIGHT_SETTINGS).dampener()) {
            status = Component.translatable("hud.void_reclaimer.dampener_on");
            color = COLOR_NORMAL;
        } else {
            status = Component.translatable("hud.void_reclaimer.dampener_off");
            color = COLOR_DIM;
        }
        graphics.drawString(font, status, x, y + 10, color);
    }
}
