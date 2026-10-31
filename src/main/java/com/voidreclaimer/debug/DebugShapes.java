package com.voidreclaimer.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Lines and labels in the world, built on the server for photo mode's overlay and drawn by the client, pure. */
public final class DebugShapes {
    /** The bits of a corner's index pick its low or high side on each axis; these pairs differ in one bit, so they are the edges. */
    private static final int[][] EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final double HEAD = 0.25;

    public record Line(Vec3 from, Vec3 to, int colour) {}

    public record Text(Vec3 at, String text, int colour) {}

    private final List<Line> lines = new ArrayList<>();
    private final List<Text> texts = new ArrayList<>();

    public void line(Vec3 from, Vec3 to, int colour) {
        lines.add(new Line(from, to, colour));
    }

    public void text(Vec3 at, String text, int colour) {
        texts.add(new Text(at, text, colour));
    }

    /** A shaft along {@code vector} with a small head. Nothing for a zero vector: it has no direction to draw. */
    public void arrow(Vec3 from, Vec3 vector, int colour) {
        double length = vector.length();
        if (length < 1.0E-6) {
            return;
        }
        Vec3 tip = from.add(vector);
        Vec3 along = vector.scale(1.0 / length);
        Vec3 side = along.cross(Math.abs(along.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        double head = length * HEAD;
        line(from, tip, colour);
        line(tip, tip.subtract(along.scale(head)).add(side.scale(head / 2.0)), colour);
        line(tip, tip.subtract(along.scale(head)).subtract(side.scale(head / 2.0)), colour);
    }

    /** A box from eight corners, in the order the bits of the index give: x, then y, then z. The corners may be turned, as on a ship. */
    public void box(List<Vec3> corners, int colour) {
        for (int[] edge : EDGES) {
            line(corners.get(edge[0]), corners.get(edge[1]), colour);
        }
    }

    public List<Line> lines() {
        return lines;
    }

    public List<Text> texts() {
        return texts;
    }
}
