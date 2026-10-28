package com.voidreclaimer.flight;

import java.util.ArrayDeque;
import net.minecraft.world.phys.Vec3;

/** Server-side ring of a player's recent positions, one per tick, used by the speed check. */
public final class FlightTracker {
    private final ArrayDeque<Vec3> positions = new ArrayDeque<>();

    /** Records this tick's position, dropping the oldest beyond {@code window} entries. */
    void record(Vec3 position, int window) {
        positions.addLast(position);
        while (positions.size() > window) {
            positions.removeFirst();
        }
    }

    int size() {
        return positions.size();
    }

    Vec3 oldest() {
        return positions.peekFirst();
    }

    /** Position recorded on the previous tick, or null. */
    Vec3 latest() {
        return positions.peekLast();
    }

    public void clear() {
        positions.clear();
    }
}
