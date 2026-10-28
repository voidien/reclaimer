package com.voidreclaimer.client.hud;

import com.voidreclaimer.Config;
import com.voidreclaimer.client.input.ClientTetherController;
import com.voidreclaimer.tether.Tether;
import com.voidreclaimer.tether.TetherData;
import com.voidreclaimer.tether.TetherPhysics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Small text readout above the flight readout: tether distance, recall hold progress, or the active recall. */
public final class TetherHudLayer {
    private static final int COLOR_NORMAL = 0x9FE8FF;
    private static final int COLOR_TAUT = 0xFFAA40;
    /** The leash counts as taut from this fraction of its length. */
    private static final double TAUT_FRACTION = 0.9;

    private TetherHudLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        Vec3 anchor = Tether.anchorPoint(player);
        if (anchor == null) {
            return;
        }
        TetherData data = Tether.data(player);
        double distance = player.getBoundingBox().getCenter().distanceTo(anchor);
        Component text;
        int color = COLOR_NORMAL;
        int hold = ClientTetherController.holdTicks();
        if (data.recalling()) {
            text = Component.translatable("hud.void_reclaimer.tether.recalling", Math.round(distance));
        } else if (hold > 0) {
            boolean teleport = TetherPhysics.usesTeleport(distance, Config.RECALL_PULL_RANGE.get());
            int needed = teleport ? Tether.TELEPORT_HOLD_TICKS : Tether.PULL_HOLD_TICKS;
            int percent = Math.min(100, hold * 100 / needed);
            text = Component.translatable(teleport ? "hud.void_reclaimer.tether.hold_teleport" : "hud.void_reclaimer.tether.hold_recall", percent);
        } else if (data.clipped()) {
            double length = Config.TETHER_LENGTH.get();
            text = Component.translatable("hud.void_reclaimer.tether.length", Math.round(distance), Math.round(length));
            if (distance >= length * TAUT_FRACTION) {
                color = COLOR_TAUT;
            }
        } else {
            return;
        }
        graphics.drawString(mc.font, text, graphics.guiWidth() / 2 + 96, graphics.guiHeight() - 30, color);
    }
}
