package com.voidreclaimer.flight;

import com.voidreclaimer.api.body.Chassis;
import java.util.ArrayDeque;
import net.minecraft.world.phys.Vec3;

/** Server-side ring of a player's recent positions, one per tick, used by the speed check. */
public final class FlightTracker {
    private final ArrayDeque<Vec3> positions = new ArrayDeque<>();
    private double lastSpeed;
    /** Where the body's boxes were last clear of everything, and what body that was: a different one proves nothing about this place. */
    private Vec3 clearAt;
    private Chassis clearChassis;
    private int deepTicks;

    /** Records this tick's position, dropping the oldest beyond {@code window} entries. */
    public void record(Vec3 position, int window) {
        positions.addLast(position);
        while (positions.size() > window) {
            positions.removeFirst();
        }
    }

    public int size() {
        return positions.size();
    }

    Vec3 oldest() {
        return positions.peekFirst();
    }

    /** Position recorded on the previous tick, or null. */
    Vec3 latest() {
        return positions.peekLast();
    }

    /** Blocks moved on the last tracked tick (0 when not under thruster flight); used for dust scooping. */
    public double lastSpeed() {
        return lastSpeed;
    }

    void setLastSpeed(double speed) {
        lastSpeed = speed;
    }

    /** The body's boxes are clear of everything here. */
    void clearOfBlocks(Vec3 position, Chassis chassis) {
        clearAt = position;
        clearChassis = chassis;
        deepTicks = 0;
    }

    /** Counts {@code ticks} more of a body deep in something. Returns how long that has lasted, in ticks. */
    int deepFor(int ticks) {
        deepTicks += ticks;
        return deepTicks;
    }

    /** Where the body was last clear, or null if this {@code chassis} hasn't been: nothing says a body fitted in a tunnel got in by cheating. */
    Vec3 lastClear(Chassis chassis) {
        return chassis.equals(clearChassis) ? clearAt : null;
    }

    public void clear() {
        positions.clear();
        lastSpeed = 0.0;
        clearAt = null;
        clearChassis = null;
        deepTicks = 0;
    }
}
