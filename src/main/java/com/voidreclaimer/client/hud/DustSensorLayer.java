package com.voidreclaimer.client.hud;

import com.voidreclaimer.client.input.ClientSensorController;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.dust.DustSensor;
import com.voidreclaimer.dust.SensorPulse;
import com.voidreclaimer.network.SensorEchoPayload;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Dust Sensor HUD (only with a sensor):
 * <ul>
 *   <li>inside a cloud: kind, local density, and screen-edge markers for the core and the nearest edge;</li>
 *   <li>outside: the hum's signal bar;</li>
 *   <li>after a pulse: the fuzzy echo marker, fading out.</li>
 * </ul>
 * Nothing is drawn in the world; clouds stay invisible.
 */
public final class DustSensorLayer {
    private static final int PANEL_Y = 6;
    private static final int BAR_WIDTH = 100;
    private static final int MARKER_MARGIN = 24;
    /** Keeps pinned markers above the hotbar, health/energy bars and the flight readout. */
    private static final int MARKER_BOTTOM_MARGIN = 72;
    private static final int TEXT = 0xCFE8FF;

    private DustSensorLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player) || !DustSensor.has(player)) {
            return;
        }
        DustField field = DustField.of(player.level());
        if (field == null) {
            return;
        }
        Font font = mc.font;
        Camera camera = mc.gameRenderer.getMainCamera();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        double fov = mc.options.fov().get();
        Vec3 eyes = player.getEyePosition();
        DustField.Sample sample = field.sample(eyes);

        if (sample.inCloud()) {
            DustCloud cloud = sample.cloud();
            int kindColor = DustKinds.color(player.registryAccess(), cloud.kind());
            Component title = Component.translatable("hud.void_reclaimer.sensor.cloud", DustKinds.name(player.registryAccess(), cloud.kind()),
                    Math.round(sample.density() * 100));
            graphics.drawCenteredString(font, title, width / 2, PANEL_Y, kindColor);
            bar(graphics, width / 2 - BAR_WIDTH / 2, PANEL_Y + 11, (float) sample.density(), 0xFF000000 | kindColor);

            double toCenter = eyes.distanceTo(cloud.center());
            if (toCenter > 2.0) {
                Vec3 inward = cloud.center().subtract(eyes).normalize();
                marker(graphics, font, camera, inward, fov, width, height,
                        Component.translatable("hud.void_reclaimer.sensor.core", Math.round(toCenter)), kindColor);
                marker(graphics, font, camera, inward.scale(-1), fov, width, height,
                        Component.translatable("hud.void_reclaimer.sensor.edge", Math.round(cloud.radius() - toCenter)), 0x8899AA);
            }
        } else {
            graphics.drawCenteredString(font, Component.translatable("hud.void_reclaimer.sensor.clear"), width / 2, PANEL_Y, 0x8899AA);
            float signal = ClientSensorController.signal();
            if (signal > 0f) {
                graphics.drawCenteredString(font, Component.translatable("hud.void_reclaimer.sensor.signal"), width / 2, PANEL_Y + 11, TEXT);
                bar(graphics, width / 2 - BAR_WIDTH / 2, PANEL_Y + 22, signal, 0xFF7FD4FF);
            }
        }

        SensorEchoPayload echo = ClientSensorController.echo();
        if (echo != null) {
            int alpha = Mth.clamp((int) (ClientSensorController.echoFade() * 255), 8, 255);
            if (echo.strength() == SensorPulse.Strength.NONE) {
                graphics.drawCenteredString(font, Component.translatable("hud.void_reclaimer.sensor.echo.none"),
                        width / 2, height / 2 + 20, (alpha << 24) | 0x8899AA);
            } else {
                Component label = Component.translatable("hud.void_reclaimer.sensor.echo." + echo.strength().name().toLowerCase(java.util.Locale.ROOT));
                marker(graphics, font, camera, echo.direction(), fov, width, height, label, (alpha << 24) | 0xFFE08A);
            }
        }
    }

    private static void bar(GuiGraphics graphics, int x, int y, float fraction, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + 3, 0x90202028);
        int filled = Mth.clamp(Math.round(fraction * BAR_WIDTH), 0, BAR_WIDTH);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 3, color);
        }
    }

    private static void marker(GuiGraphics graphics, Font font, Camera camera, Vec3 direction, double fov,
                               int width, int height, Component label, int color) {
        ScreenProjection.Point point = ScreenProjection.project(camera, direction, fov, width, height, MARKER_MARGIN, MARKER_BOTTOM_MARGIN);
        int argb = (color & 0xFF000000) == 0 ? 0xFF000000 | color : color;
        graphics.fill(point.x() - 2, point.y() - 2, point.x() + 2, point.y() + 2, argb);
        int textWidth = font.width(label);
        int textX = Mth.clamp(point.x() - textWidth / 2, 2, width - textWidth - 2);
        int textY = point.y() + (point.y() > height / 2 ? -12 : 5);
        graphics.drawString(font, label, textX, textY, argb);
    }
}
