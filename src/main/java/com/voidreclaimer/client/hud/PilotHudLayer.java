package com.voidreclaimer.client.hud;

import com.mojang.math.Axis;
import com.voidreclaimer.client.input.ClientPilotController;
import com.voidreclaimer.flight.FlightSettings;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.ship.Attitude;
import com.voidreclaimer.ship.HelmBlockEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * The pilot's HUD while sitting at a Helm: speed and the two holds, the thrusters' reach on each axis, fuel and energy
 * across the ship's thrusters, and an attitude indicator. Everything comes from the Helm's readout, sent each tick.
 * Drawn only through {@link GuiGraphics}, so it survives the performance mods.
 */
public final class PilotHudLayer {
    private static final int COLOR_NORMAL = 0x9FE8FF;
    private static final int COLOR_DIM = 0x6F8A99;
    private static final int COLOR_WARNING = 0xFF5050;
    private static final int COLOR_SKY = 0xA02C5A8C;
    private static final int COLOR_GROUND = 0xA06B4A2B;
    private static final int COLOR_LINE = 0xFFE8F4FF;
    /** Half the attitude box, in pixels, and how many pixels a degree of pitch moves the horizon. */
    static final int ATTITUDE_HALF = 22;
    static final double PIXELS_PER_DEGREE = 0.5;
    /** A thruster axis counts as covered from this fraction of a full push. */
    static final double COVERED = 0.5;
    private static final String[] MOVE_LABELS = {"F", "B", "R", "L", "U", "D"};
    private static final String[] TURN_LABELS = {"RR", "RL", "NU", "ND", "YL", "YR"};

    private PilotHudLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.options.hideGui || player == null || !ClientPilotController.isPiloting(player)) {
            return;
        }
        HelmBlockEntity.Readout readout = ClientPilotController.readout();
        FlightSettings settings = player.getData(ModAttachments.FLIGHT_SETTINGS);
        Font font = mc.font;
        int x = graphics.guiWidth() / 2 + 96;
        int bottom = graphics.guiHeight() - 10;

        // Speed, or why there is none.
        if (!readout.onShip()) {
            graphics.drawString(font, Component.translatable("hud.void_reclaimer.pilot.no_ship"), x, bottom - 40, COLOR_WARNING);
        } else if (readout.thrusters() == 0) {
            graphics.drawString(font, Component.translatable("hud.void_reclaimer.pilot.no_thrusters"), x, bottom - 40, COLOR_WARNING);
        } else {
            graphics.drawString(font, Component.translatable("hud.void_reclaimer.speed", String.format("%.1f", readout.speed())), x, bottom - 40, COLOR_NORMAL);
        }
        // The holds.
        Component dampener = Component.translatable(settings.dampener() ? "hud.void_reclaimer.dampener_on" : "hud.void_reclaimer.dampener_off");
        graphics.drawString(font, dampener, x, bottom - 30, settings.dampener() ? COLOR_NORMAL : COLOR_DIM);
        Component lock = Component.translatable(settings.rcsLock() ? "hud.void_reclaimer.pilot.rcs_on" : "hud.void_reclaimer.pilot.rcs_off");
        graphics.drawString(font, lock, x + font.width(dampener) + 8, bottom - 30, settings.rcsLock() ? COLOR_NORMAL : COLOR_DIM);
        // Supplies: only the kinds the ship carries.
        int sx = x;
        if (readout.fuelCapacity() > 0) {
            Component fuel = Component.translatable("hud.void_reclaimer.pilot.fuel", compact(readout.fuel()), compact(readout.fuelCapacity()));
            graphics.drawString(font, fuel, sx, bottom - 20, readout.fuel() > 0 ? COLOR_NORMAL : COLOR_WARNING);
            sx += font.width(fuel) + 8;
        }
        if (readout.energyCapacity() > 0) {
            Component energy = Component.translatable("hud.void_reclaimer.pilot.energy", compact(readout.energy()), compact(readout.energyCapacity()));
            graphics.drawString(font, energy, sx, bottom - 20, readout.energy() > 0 ? COLOR_NORMAL : COLOR_WARNING);
        }
        // Thrusters: how many, and which ways they can push and turn.
        Component count = Component.translatable("hud.void_reclaimer.pilot.thrusters", readout.thrusters());
        graphics.drawString(font, count, x, bottom - 10, COLOR_NORMAL);
        int ax = x + font.width(count) + 8;
        ax = axes(graphics, font, ax, bottom - 10, MOVE_LABELS, readout.move());
        axes(graphics, font, ax + 6, bottom - 10, TURN_LABELS, readout.turn());

        attitude(graphics, font, graphics.guiWidth() / 2 - 96 - ATTITUDE_HALF - 6, bottom - ATTITUDE_HALF - 4, readout.attitude(), readout.onShip());
    }

    /** Draws the axis labels, lit where the ship has a thruster for them; returns where the row ends. */
    private static int axes(GuiGraphics graphics, Font font, int x, int y, String[] labels, double[] coverage) {
        for (int i = 0; i < labels.length; i++) {
            int color = coverage[i] >= COVERED ? COLOR_NORMAL : coverage[i] > 0 ? 0x4FA0B8 : COLOR_DIM;
            graphics.drawString(font, labels[i], x, y, color);
            x += font.width(labels[i]) + 3;
        }
        return x;
    }

    /** An artificial horizon in a box centred on ({@code cx}, {@code cy}): the sky and ground roll and slide with the ship. */
    static void attitude(GuiGraphics graphics, Font font, int cx, int cy, Attitude attitude, boolean onShip) {
        int half = ATTITUDE_HALF;
        graphics.fill(cx - half - 1, cy - half - 1, cx + half + 1, cy + half + 1, 0xC0101820);
        graphics.enableScissor(cx - half, cy - half, cx + half, cy + half);
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        // The world turns the other way from the ship: the view rolls right, the horizon rolls left on screen.
        graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) -attitude.roll()));
        int horizon = (int) Math.round(attitude.pitch() * PIXELS_PER_DEGREE);
        int reach = half * 2;
        graphics.fill(-reach, -reach + horizon, reach, horizon, COLOR_SKY);
        graphics.fill(-reach, horizon, reach, reach + horizon, COLOR_GROUND);
        graphics.fill(-reach, horizon, reach, horizon + 1, COLOR_LINE);
        graphics.pose().popPose();
        graphics.disableScissor();
        // The ship's own symbol stays put: a wing each side and a dot in the middle.
        graphics.fill(cx - half + 3, cy, cx - 4, cy + 1, COLOR_LINE);
        graphics.fill(cx + 4, cy, cx + half - 3, cy + 1, COLOR_LINE);
        graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, COLOR_LINE);
        if (onShip) {
            Component text = Component.translatable("hud.void_reclaimer.pilot.attitude",
                    String.format("%03d", (int) Math.round(attitude.heading()) % 360),
                    String.format("%+d", (int) Math.round(attitude.pitch())),
                    String.format("%+d", (int) Math.round(attitude.roll())));
            graphics.drawString(font, text, cx + half + 1 - font.width(text), cy - half - 11, COLOR_NORMAL);
        }
    }

    /** 1234 -> "1.2k", 1234567 -> "1.2M"; small numbers stay whole. */
    static String compact(double value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000);
        }
        if (value >= 10_000) {
            return String.format("%.0fk", value / 1_000);
        }
        if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000);
        }
        return String.format("%.0f", value);
    }
}
