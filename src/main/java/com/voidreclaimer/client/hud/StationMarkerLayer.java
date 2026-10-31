package com.voidreclaimer.client.hud;

import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.station.StationList;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Edge-pinned markers for the nearest stations of this level, name and distance, like the death marker. Stations within
 * {@value #ARRIVED} blocks are not marked (you are there). Drawn through {@link GuiGraphics} only.
 */
public final class StationMarkerLayer {
    static final double ARRIVED = 24.0;
    static final int MOST = 3;
    private static final int COLOR = 0x9FE8FF;
    private static final int MARGIN = 24;
    private static final int BOTTOM_MARGIN = 72;

    private StationMarkerLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null || !(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        StationList stations = mc.level.getData(ModAttachments.STATIONS);
        Vec3 eyes = player.getEyePosition();
        Font font = mc.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (StationList.Entry station : stations.farthestFirstNearest(eyes, ARRIVED, MOST)) {
            Vec3 target = station.position();
            double distance = eyes.distanceTo(target);
            ScreenProjection.Point point = ScreenProjection.project(mc.gameRenderer.getMainCamera(), target.subtract(eyes).normalize(),
                    mc.options.fov().get(), width, height, MARGIN, BOTTOM_MARGIN);
            graphics.fill(point.x() - 2, point.y() - 2, point.x() + 2, point.y() + 2, COLOR);
            Component label = Component.translatable("hud.void_reclaimer.station_marker", station.name(), Math.round(distance));
            int textWidth = font.width(label);
            int textX = Mth.clamp(point.x() - textWidth / 2, 2, width - textWidth - 2);
            int textY = point.y() + (point.y() > height / 2 ? -12 : 5);
            graphics.drawString(font, label, textX, textY, COLOR);
        }
    }
}
