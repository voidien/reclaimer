package com.voidreclaimer.client.screen;

import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.assembly.AssemblyCamera;
import com.voidreclaimer.client.render.body.BodyBounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * The body beside a screen's panel, in the empty margin a smaller GUI scale leaves: drag to turn it, scroll to zoom, and it stays
 * where it is left. With no room it is simply not there.
 */
final class SidePreview {
    /** Below this a body is too small to be worth the space, in GUI pixels. */
    static final int MIN_MARGIN = 96;
    private static final int MAX_WIDTH = 160;
    private static final int GUTTER = 4;
    private static final int PANEL = 0xE0101820;
    private static final int BORDER = 0xFF3A6A80;
    private static final float FIT = 0.42f;

    private final AssemblyCamera camera = new AssemblyCamera(AssemblyScreen.START_YAW, AssemblyScreen.START_PITCH);
    private boolean dragging;
    private long lastFrame;

    /** Whether a margin of {@code margin} GUI pixels beside the panel has room. */
    static boolean fits(int margin) {
        return margin >= MIN_MARGIN;
    }

    /** The preview's left edge, in a margin that runs from 0 to {@code margin}. */
    static int leftEdge(int margin) {
        return Math.max(GUTTER, margin - GUTTER - MAX_WIDTH);
    }

    /** Whether a point is in the box left of a panel at {@code panelLeft}, between {@code top} and {@code bottom}. */
    static boolean over(int panelLeft, int top, int bottom, double x, double y) {
        return fits(panelLeft) && x >= leftEdge(panelLeft) && x < panelLeft - GUTTER && y >= top && y < bottom;
    }

    /** Draws the body in the margin left of a panel at {@code panelLeft}, as tall as the panel. */
    void render(GuiGraphics graphics, LocalPlayer player, int panelLeft, int top, int bottom) {
        if (!fits(panelLeft)) {
            return;
        }
        long now = Util.getMillis();
        camera.advance(lastFrame == 0L ? 0f : (now - lastFrame) / 1000f);
        lastFrame = now;
        int left = leftEdge(panelLeft);
        int right = panelLeft - GUTTER;
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.renderOutline(left, top, right - left, bottom - top, BORDER);
        float radius = Math.max(0.1f, BodyBounds.radius(Bodies.view(player)) * player.getScale());
        BodyStage.draw(graphics, player, camera, left + 1, top + 1, right - 1, bottom - 1, Math.min(right - left, bottom - top) * FIT / radius * camera.zoom());
    }

    /** Starts a drag on the box; whether the press was this one's. */
    boolean press(int panelLeft, int top, int bottom, double x, double y) {
        dragging = over(panelLeft, top, bottom, x, y);
        return dragging;
    }

    boolean drag(double dx, double dy) {
        if (dragging) {
            camera.drag(dx, dy);
        }
        return dragging;
    }

    void release() {
        dragging = false;
    }

    /** Zooms if the mouse is on the box; whether it was. */
    boolean scroll(int panelLeft, int top, int bottom, double x, double y, double notches) {
        boolean on = over(panelLeft, top, bottom, x, y);
        if (on) {
            camera.scroll(notches);
        }
        return on;
    }
}
