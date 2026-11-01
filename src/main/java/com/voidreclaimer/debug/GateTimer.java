package com.voidreclaimer.debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;

/**
 * One body's flight through a line of gates, to judge flight tuning by the clock rather than by feel. Gates face along +z and are
 * flown through in that direction. The first gate starts the clock from any state, so a run can always be flown again. Pure.
 */
public final class GateTimer {
    /** A square hole in the plane of constant {@code z}; {@code half} is half its width. */
    public record Gate(double x, double y, double z, double half) {
        /** How far along the move the plane is crossed, 0 to 1, or negative when the move does not cross it. */
        double crossing(Vec3 from, Vec3 to) {
            return from.z < z && to.z >= z ? (z - from.z) / (to.z - from.z) : -1;
        }

        boolean holds(Vec3 point) {
            return Math.abs(point.x - x) <= half && Math.abs(point.y - y) <= half;
        }
    }

    public enum Kind {
        STARTED, GATE, FINISHED, MISSED
    }

    /** What just happened: {@code gate} counts from 1, {@code seconds} from the first gate, {@code speed} in blocks a second. */
    public record Event(Kind kind, int gate, int gates, double seconds, double speed, double best) {}

    private static final double TICKS_PER_SECOND = 20.0;
    /** Speed is read over this many ticks: a client's moves reach the server unevenly, so one tick can hold two of them. */
    private static final int SPEED_TICKS = 5;

    private final List<Gate> gates;
    /** The next gate to fly through; 0 before a run. */
    private int next;
    private double started;
    private double best = Double.POSITIVE_INFINITY;
    private final Deque<Vec3> recent = new ArrayDeque<>();

    public GateTimer(List<Gate> gates) {
        this.gates = List.copyOf(gates);
    }

    /** Whether a run is under way. */
    public boolean running() {
        return next > 0;
    }

    /** Takes the body's position this tick; returns what happened, or null. */
    @Nullable
    public Event update(long tick, Vec3 position) {
        Vec3 from = recent.peekLast();
        recent.addLast(position);
        if (recent.size() > SPEED_TICKS + 1) {
            recent.removeFirst();
        }
        if (from == null || gates.isEmpty()) {
            return null;
        }
        double speed = recent.peekFirst().distanceTo(position) * TICKS_PER_SECOND / (recent.size() - 1);
        double start = gates.get(0).crossing(from, position);
        if (start >= 0 && gates.get(0).holds(from.lerp(position, start))) {
            started = tick - 1 + start;
            next = 1;
            return new Event(Kind.STARTED, 1, gates.size(), 0, speed, best);
        }
        if (next == 0) {
            return null;
        }
        Gate gate = gates.get(next);
        double along = gate.crossing(from, position);
        if (along < 0) {
            return null;
        }
        double seconds = (tick - 1 + along - started) / TICKS_PER_SECOND;
        if (!gate.holds(from.lerp(position, along))) {
            int missed = next + 1;
            next = 0;
            return new Event(Kind.MISSED, missed, gates.size(), seconds, speed, best);
        }
        next++;
        if (next < gates.size()) {
            return new Event(Kind.GATE, next, gates.size(), seconds, speed, best);
        }
        next = 0;
        best = Math.min(best, seconds);
        return new Event(Kind.FINISHED, gates.size(), gates.size(), seconds, speed, best);
    }
}
