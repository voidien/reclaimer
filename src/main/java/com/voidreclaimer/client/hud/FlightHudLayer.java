package com.voidreclaimer.client.hud;

import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.flight.AfterburnerState;
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
    /** Degrees between view and nose below which the marker would only sit on the crosshair. */
    private static final double NOSE_SHOWN_FROM = 1.5;
    private static final int NOSE_SIZE = 4;
    private static final int NOSE_MARGIN = 12;

    private FlightHudLayer() {}

    /** Where a big body's nose points while it catches up with the view: that is where thrust goes, and the crosshair is not. */
    private static void renderNose(GuiGraphics graphics, Minecraft mc, Player player, float partialTick) {
        com.voidreclaimer.body.BodyHeading heading = com.voidreclaimer.body.BodyTurning.at(player, partialTick);
        if (heading == null || Math.hypot(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - heading.yaw()), player.getXRot() - heading.pitch()) < NOSE_SHOWN_FROM) {
            return;
        }
        var nose = com.voidreclaimer.body.BodyBoxes.turn(heading.yaw(), heading.pitch()).transform(new org.joml.Vector3d(0.0, 0.0, -1.0));
        var point = com.voidreclaimer.client.hud.ScreenProjection.project(mc.gameRenderer.getMainCamera(), new net.minecraft.world.phys.Vec3(nose.x, nose.y, nose.z),
                mc.options.fov().get(), graphics.guiWidth(), graphics.guiHeight(), NOSE_MARGIN, NOSE_MARGIN);
        int color = 0xFF000000 | COLOR_NORMAL;
        graphics.fill(point.x() - NOSE_SIZE, point.y() - NOSE_SIZE, point.x() + NOSE_SIZE + 1, point.y() - NOSE_SIZE + 1, color);
        graphics.fill(point.x() - NOSE_SIZE, point.y() + NOSE_SIZE, point.x() + NOSE_SIZE + 1, point.y() + NOSE_SIZE + 1, color);
        graphics.fill(point.x() - NOSE_SIZE, point.y() - NOSE_SIZE, point.x() - NOSE_SIZE + 1, point.y() + NOSE_SIZE, color);
        graphics.fill(point.x() + NOSE_SIZE, point.y() - NOSE_SIZE, point.x() + NOSE_SIZE + 1, point.y() + NOSE_SIZE, color);
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player) || !ThrusterFlight.isActive(player)) {
            return;
        }
        renderNose(graphics, mc, player, deltaTracker.getGameTimeDeltaPartialTick(true));
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
        } else if (ThrusterFlight.weight(player) > 0.0 && !player.getData(ModAttachments.FLIGHT_SETTINGS).dampener()) {
            status = Component.translatable("hud.void_reclaimer.falling");
            color = COLOR_WARNING;
        } else if (ThrusterFlight.weight(player) > 0.0 && FlightStats.of(player).acceleration() < ThrusterFlight.weight(player)) {
            status = Component.translatable("hud.void_reclaimer.too_heavy");
            color = COLOR_WARNING;
        } else if (ThrusterFlight.weight(player) > 0.0) {
            status = Component.translatable("hud.void_reclaimer.hover");
            color = COLOR_NORMAL;
        } else if (player.getData(ModAttachments.FLIGHT_SETTINGS).dampener()) {
            status = Component.translatable("hud.void_reclaimer.dampener_on");
            color = COLOR_NORMAL;
        } else {
            status = Component.translatable("hud.void_reclaimer.dampener_off");
            color = COLOR_DIM;
        }
        graphics.drawString(font, status, x, y + 10, color);

        // Above the speed: how much burn is left, or how far it has to cool.
        AfterburnerState burner = player.getData(ModAttachments.AFTERBURNER);
        if (burner.heat() > 0f) {
            int percent = Math.round(burner.heat() * 100f);
            Component line = burner.overheated() ? Component.translatable("hud.void_reclaimer.afterburner_cooling", percent)
                    : Component.translatable("hud.void_reclaimer.afterburner", 100 - percent);
            graphics.drawString(font, line, x, y - 10, burner.overheated() ? COLOR_WARNING : COLOR_NORMAL);
        }
    }
}
