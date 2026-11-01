package com.voidreclaimer.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The last two seconds of a Helm's steering, to tune it from: what the pilot asked for, what the ship did, and how long the ship took
 * to answer. Pure, so the lag can be tested. {@code /void_reclaimer debug helm} prints it.
 */
public final class HelmTrace {
    /** One tick. Axes are pitch, yaw and roll, as the pilot names them; spin in degrees a second. */
    public record Sample(float mouseRight, float mouseUp, double[] wanted, double[] spin) {
    }

    private static final int TICKS = 40;
    /** The longest lag looked for, in ticks. */
    static final int MOST_LAG = 6;
    /** Below this much movement in spin there is nothing to measure a lag from, in degrees a second. */
    private static final double STILL = 0.5;
    private static final String[] AXES = {"pitch", "yaw", "roll"};

    private final List<Sample> samples = new ArrayList<>();
    private double[] authority = {0, 0, 0};
    private String mode = "";

    public void record(Sample sample) {
        samples.add(sample);
        if (samples.size() > TICKS) {
            samples.remove(0);
        }
    }

    /** What the thrusters can do, in degrees a second squared, and how the Helm is steering. */
    public void setup(double[] authority, String mode) {
        this.authority = authority;
        this.mode = mode;
    }

    /**
     * Ticks from a wanted turn to the ship's spin changing, on the axis that moved most: the offset where the two line up best.
     * Negative when the ship has hardly moved.
     */
    public int lag() {
        int axis = busiestAxis();
        if (axis < 0) {
            return -1;
        }
        int best = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int lag = 0; lag <= MOST_LAG; lag++) {
            double score = 0;
            for (int i = lag + 1; i < samples.size(); i++) {
                score += samples.get(i - lag).wanted()[axis] * (samples.get(i).spin()[axis] - samples.get(i - 1).spin()[axis]);
            }
            if (score > bestScore) {
                bestScore = score;
                best = lag;
            }
        }
        return best;
    }

    private int busiestAxis() {
        int busiest = -1;
        double most = STILL;
        for (int axis = 0; axis < AXES.length; axis++) {
            double low = Double.MAX_VALUE;
            double high = -Double.MAX_VALUE;
            for (Sample sample : samples) {
                low = Math.min(low, sample.spin()[axis]);
                high = Math.max(high, sample.spin()[axis]);
            }
            if (high - low > most) {
                most = high - low;
                busiest = axis;
            }
        }
        return busiest;
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        if (samples.isEmpty()) {
            lines.add("Nothing steered yet");
            return lines;
        }
        Sample last = samples.get(samples.size() - 1);
        float right = 0f;
        float up = 0f;
        for (Sample sample : samples) {
            right += sample.mouseRight();
            up += sample.mouseUp();
        }
        lines.add("Steering: " + mode + ", " + samples.size() + " ticks kept");
        lines.add(String.format(Locale.ROOT, "Mouse in those ticks: %.1f right, %.1f up, degrees", right, up));
        lines.add("Wanted turn now, -1 to 1: " + three(last.wanted(), "%.2f"));
        lines.add("Real spin now, degrees a second: " + three(last.spin(), "%.1f"));
        lines.add("Authority, degrees a second squared: " + three(authority, "%.0f"));
        int lag = lag();
        lines.add(lag < 0 ? "Lag: the ship has hardly moved, turn it and ask again" : "Lag: about " + lag + " ticks from asking to turning");
        return lines;
    }

    private static String three(double[] values, String format) {
        List<String> parts = new ArrayList<>();
        for (int axis = 0; axis < AXES.length; axis++) {
            parts.add(AXES[axis] + " " + String.format(Locale.ROOT, format, values[axis]));
        }
        return String.join(", ", parts);
    }
}
