package com.voidreclaimer.debug;

import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.flight.FlightStats;
import java.util.List;
import java.util.Locale;

/**
 * A body's handling in numbers, to tune flight from: speeds in blocks a second, and top speed in its own lengths a second, which is
 * what makes a body feel fast or slow. Pure; {@code /vrphoto handling} prints it, on the client because only the model knows a body's
 * length.
 */
public final class HandlingReadout {
    /** Mass in the body's units, speed in blocks a second, acceleration and braking in blocks a second squared, length in blocks. */
    public record Row(String name, double mass, double budget, double topSpeed, double acceleration, double braking, double length) {
        public double lengthsPerSecond() {
            return length > 0 ? topSpeed / length : 0;
        }

        public double secondsToTop() {
            return acceleration > 0 ? topSpeed / acceleration : 0;
        }

        /** How far the dampeners take to stop the body from top speed. */
        public double stoppingDistance() {
            return braking > 0 ? topSpeed * topSpeed / (2 * braking) : 0;
        }
    }

    private HandlingReadout() {}

    /** {@code length} is the body's longest side in blocks at scale 1; the stats scale it. */
    public static Row row(String name, BodyStats stats, double length) {
        FlightStats flight = stats.flight();
        double perSecond = FlightStats.TICKS_PER_SECOND;
        return new Row(name, stats.mass(), stats.massBudget(), flight.maxSpeed() * perSecond, flight.acceleration() * perSecond * perSecond,
                flight.brakeDeceleration() * perSecond * perSecond, length * stats.scale());
    }

    public static String line(Row row) {
        return String.format(Locale.ROOT, "%s: mass %.1f of %.1f%s, top %.1f m/s, %.1f lengths/s, accel %.1f, brake %.1f, length %.1f, %.1f s to top, %.1f to stop",
                row.name(), row.mass(), row.budget(), row.mass() > row.budget() ? " OVER" : "", row.topSpeed(), row.lengthsPerSecond(),
                row.acceleration(), row.braking(), row.length(), row.secondsToTop(), row.stoppingDistance());
    }

    public static List<String> lines(List<Row> rows) {
        return rows.stream().map(HandlingReadout::line).toList();
    }
}
