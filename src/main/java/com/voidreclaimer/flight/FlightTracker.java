package com.voidreclaimer.flight;

import java.util.ArrayDeque;
import net.minecraft.world.phys.Vec3;

/** Server-side ring of a player's recent positions, one per tick, used by the speed check. */
public final class FlightTracker {
    private final ArrayDeque<Vec3> positions = new ArrayDeque<>();
    private double lastSpeed;

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

    public void clear() {
        positions.clear();
        lastSpeed = 0.0;
    }
}
