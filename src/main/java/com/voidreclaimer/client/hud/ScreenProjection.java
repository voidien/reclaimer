package com.voidreclaimer.client.hud;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Projects a world-space direction onto the screen. Directions in view land at their screen position; anything
 * off-screen or behind the camera is pinned to the screen border in its direction, like a compass marker.
 */
public final class ScreenProjection {
    private ScreenProjection() {}

    /**
     * @param x        GUI x
     * @param y        GUI y
     * @param onScreen whether the direction is within view (not pinned to the border)
     */
    public record Point(int x, int y, boolean onScreen) {}

    /**
     * @param margin       distance kept from the top and side edges
     * @param bottomMargin distance kept from the bottom edge (larger, to stay clear of the hotbar and status bars)
     */
    public static Point project(Camera camera, Vec3 direction, double fovDegrees, int width, int height, int margin, int bottomMargin) {
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double vx = -(direction.x * left.x + direction.y * left.y + direction.z * left.z);
        double vy = direction.x * up.x + direction.y * up.y + direction.z * up.z;
        double vz = direction.x * forward.x + direction.y * forward.y + direction.z * forward.z;

        double cx = width / 2.0;
        double cy = height / 2.0;
        double focal = (height / 2.0) / Math.tan(Math.toRadians(fovDegrees) / 2.0);
        if (vz > 1.0E-3) {
            double sx = cx + vx / vz * focal;
            double sy = cy - vy / vz * focal;
            if (sx >= margin && sx <= width - margin && sy >= margin && sy <= height - bottomMargin) {
                return new Point((int) sx, (int) sy, true);
            }
        }
        // Pin to the border along the direction's on-screen heading.
        double dx = vx;
        double dy = -vy;
        if (Math.abs(dx) < 1.0E-6 && Math.abs(dy) < 1.0E-6) {
            dy = 1.0;
        }
        double halfW = cx - margin;
        double halfH = dy > 0 ? (height - bottomMargin) - cy : cy - margin;
        double scale = Math.min(halfW / Math.max(Math.abs(dx), 1.0E-6), halfH / Math.max(Math.abs(dy), 1.0E-6));
        int px = (int) Mth.clamp(cx + dx * scale, margin, width - margin);
        int py = (int) Mth.clamp(cy + dy * scale, margin, height - bottomMargin);
        return new Point(px, py, false);
    }
}
