package com.voidreclaimer.client.hud;

import com.voidreclaimer.core.DeathMarker;
import com.voidreclaimer.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Screen-edge marker pointing to where the AI last died, and to the wreck it left there. */
public final class DeathMarkerLayer {
    private static final int COLOR = 0xFFFF8A80;
    private static final int MARGIN = 24;
    private static final int BOTTOM_MARGIN = 72;

    private DeathMarkerLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player)) {
            return;
        }
        Optional<GlobalPos> pos = player.getData(ModAttachments.DEATH_MARKER).pos();
        if (pos.isEmpty() || pos.get().dimension() != player.level().dimension()) {
            return;
        }
        Vec3 eyes = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(pos.get().pos());
        double distance = eyes.distanceTo(target);
        if (distance <= DeathMarker.ARRIVE_DISTANCE) {
            return;
        }
        Font font = mc.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        ScreenProjection.Point point = ScreenProjection.project(mc.gameRenderer.getMainCamera(), target.subtract(eyes).normalize(),
                mc.options.fov().get(), width, height, MARGIN, BOTTOM_MARGIN);
        graphics.fill(point.x() - 2, point.y() - 2, point.x() + 2, point.y() + 2, COLOR);
        Component label = Component.translatable("hud.void_reclaimer.death_marker", Math.round(distance));
        int textWidth = font.width(label);
        int textX = Mth.clamp(point.x() - textWidth / 2, 2, width - textWidth - 2);
        int textY = point.y() + (point.y() > height / 2 ? -12 : 5);
        graphics.drawString(font, label, textX, textY, COLOR);
    }
}
