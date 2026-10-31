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
    /** Height of a text row. */
    private static final int ROW = 10;
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
        // A column of short rows against the right edge, so it fits beside the hotbar at any GUI scale.
        int right = graphics.guiWidth() - 4;
        int y = graphics.guiHeight() - 10 - ROW;
        // Thrusters, bottom up: which ways they can turn, which ways they can push, how many.
        y = axes(graphics, font, right, y, TURN_LABELS, readout.turn());
        y = axes(graphics, font, right, y, MOVE_LABELS, readout.move());
        y = row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.pilot.thrusters", readout.thrusters()), COLOR_NORMAL);
        // Supplies: only the kinds the ship carries.
        if (readout.energyCapacity() > 0) {
            y = row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.pilot.energy", compact(readout.energy()), compact(readout.energyCapacity())),
                    readout.energy() > 0 ? COLOR_NORMAL : COLOR_WARNING);
        }
        if (readout.fuelCapacity() > 0) {
            y = row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.pilot.fuel", compact(readout.fuel()), compact(readout.fuelCapacity())),
                    readout.fuel() > 0 ? COLOR_NORMAL : COLOR_WARNING);
        }
        // The holds.
        y = row(graphics, font, right, y, Component.translatable(settings.rcsLock() ? "hud.void_reclaimer.pilot.rcs_on" : "hud.void_reclaimer.pilot.rcs_off"),
                settings.rcsLock() ? COLOR_NORMAL : COLOR_DIM);
        y = row(graphics, font, right, y, Component.translatable(settings.dampener() ? "hud.void_reclaimer.dampener_on" : "hud.void_reclaimer.dampener_off"),
                settings.dampener() ? COLOR_NORMAL : COLOR_DIM);
        // Speed, or why there is none.
        if (!readout.onShip()) {
            row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.pilot.no_ship"), COLOR_WARNING);
        } else if (readout.thrusters() == 0) {
            row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.pilot.no_thrusters"), COLOR_WARNING);
        } else {
            row(graphics, font, right, y, Component.translatable("hud.void_reclaimer.speed", String.format("%.1f", readout.speed())), COLOR_NORMAL);
        }

        attitude(graphics, font, graphics.guiWidth() / 2 - 96 - ATTITUDE_HALF - 6, graphics.guiHeight() - 10 - ATTITUDE_HALF - 4, readout.attitude(), readout.onShip());
    }

    /** One right-aligned row; returns the y of the row above. */
    private static int row(GuiGraphics graphics, Font font, int right, int y, Component text, int color) {
        graphics.drawString(font, text, right - font.width(text), y, color);
        return y - ROW;
    }

    /** Draws the axis labels right-aligned, lit where the ship has a thruster for them; returns the y of the row above. */
    private static int axes(GuiGraphics graphics, Font font, int right, int y, String[] labels, double[] coverage) {
        int x = right;
        for (int i = labels.length - 1; i >= 0; i--) {
            x -= font.width(labels[i]);
            int color = coverage[i] >= COVERED ? COLOR_NORMAL : coverage[i] > 0 ? 0x4FA0B8 : COLOR_DIM;
            graphics.drawString(font, labels[i], x, y, color);
            x -= 3;
        }
        return y - ROW;
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
            // Two short rows above the box, right-aligned to it: the heading, then pitch and roll.
            Component heading = Component.translatable("hud.void_reclaimer.pilot.heading", String.format("%03d", (int) Math.round(attitude.heading()) % 360));
            Component tilt = Component.translatable("hud.void_reclaimer.pilot.tilt",
                    String.format("%+d", (int) Math.round(attitude.pitch())), String.format("%+d", (int) Math.round(attitude.roll())));
            graphics.drawString(font, heading, cx + half + 1 - font.width(heading), cy - half - 2 - 2 * ROW, COLOR_NORMAL);
            graphics.drawString(font, tilt, cx + half + 1 - font.width(tilt), cy - half - 2 - ROW, COLOR_NORMAL);
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
