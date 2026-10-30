package com.voidreclaimer.terraform;

import java.util.function.IntConsumer;

/**
 * How the Genesis Pulse wave spreads: square rings (by distance in the larger of x and z) around the Spire, one after
 * another, so that 16 Spires with radius 128 tile the 4 x 4 sectors exactly. Pure so it can be tested.
 *
 * @param radius        the last ring, in blocks from the Spire's column
 * @param durationTicks how long the wave takes to reach it
 */
public record PulseSchedule(int radius, int durationTicks) {
    public PulseSchedule {
        radius = Math.max(0, radius);
        durationTicks = Math.max(1, durationTicks);
    }

    /** The outermost ring the wave has reached after {@code elapsed} ticks; -1 before it starts. */
    public int reached(int elapsed) {
        if (elapsed <= 0) {
            return -1;
        }
        return Math.min(radius, (int) ((long) (radius + 1) * elapsed / durationTicks) - 1);
    }

    /** Calls {@code ring} for every ring the wave newly covers during tick number {@code elapsed} (counted from 1). */
    public void forNewRings(int elapsed, IntConsumer ring) {
        for (int d = reached(elapsed - 1) + 1; d <= reached(elapsed); d++) {
            ring.accept(d);
        }
    }

    public boolean isDone(int elapsed) {
        return elapsed >= durationTicks;
    }

    /** Calls {@code column} with the offsets (dx, dz) of every column at distance exactly {@code d}. */
    public static void forRing(int d, ColumnVisitor column) {
        if (d == 0) {
            column.visit(0, 0);
            return;
        }
        for (int i = -d; i <= d; i++) {
            column.visit(i, -d);
            column.visit(i, d);
        }
        for (int j = -d + 1; j <= d - 1; j++) {
            column.visit(-d, j);
            column.visit(d, j);
        }
    }

    @FunctionalInterface
    public interface ColumnVisitor {
        void visit(int dx, int dz);
    }
}
