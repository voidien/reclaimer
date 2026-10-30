package com.voidreclaimer.client.render.body;

import java.util.HashMap;
import java.util.Map;

/**
 * Lean of a body into its motion, kept free of rendering classes so it can be unit-tested. A body that leans forward when
 * it flies forward, banks into a strafe and tips back when it flies backward needs only one good flying animation for
 * every direction. The target angles follow the step relative to the facing; each tick the current angles ease toward them.
 */
public final class BodyLean {
    /** Share of the remaining angle covered each tick. */
    public static final float EASE = 0.25f;

    private BodyLean() {}

    /** Pitch (positive leans forward) and roll (positive banks to the right), in degrees. */
    public record Angles(float pitch, float roll) {
        public static final Angles ZERO = new Angles(0f, 0f);
    }

    /**
     * The angles the lean eases toward.
     *
     * @param forward the step's part along the facing, blocks per tick (negative when flying backward)
     * @param lateral the step's part to the right, blocks per tick (negative when flying left)
     */
    public static Angles target(double forward, double lateral, BodyRigSpec.Lean lean) {
        if (lean.isNone()) {
            return Angles.ZERO;
        }
        return new Angles(lean.pitch() * share(forward, lean), lean.roll() * share(lateral, lean));
    }

    private static float share(double speed, BodyRigSpec.Lean lean) {
        return (float) Math.max(-1.0, Math.min(1.0, speed / lean.referenceSpeed()));
    }

    /** One tick of easing from {@code current} toward {@code target}. */
    public static Angles ease(Angles current, Angles target) {
        return new Angles(current.pitch() + (target.pitch() - current.pitch()) * EASE,
                current.roll() + (target.roll() - current.roll()) * EASE);
    }

    /** Per-player memory of the last two tick angles, so the renderer can blend between ticks. */
    public static final class State {
        private final Map<Integer, Entry> entries = new HashMap<>();

        private static final class Entry {
            int tick = Integer.MIN_VALUE;
            Angles previous = Angles.ZERO;
            Angles current = Angles.ZERO;
        }

        /** Advances a player's angles once per game tick (a repeat call in the same tick does nothing). */
        public void update(int playerId, int tick, double forward, double lateral, BodyRigSpec.Lean lean) {
            Entry entry = entries.computeIfAbsent(playerId, id -> new Entry());
            if (entry.tick != tick) {
                entry.tick = tick;
                entry.previous = entry.current;
                entry.current = ease(entry.current, target(forward, lateral, lean));
            }
        }

        /** The angles to draw, blended {@code partialTick} of the way from the last tick to this one. */
        public Angles at(int playerId, float partialTick) {
            Entry entry = entries.get(playerId);
            if (entry == null) {
                return Angles.ZERO;
            }
            return new Angles(entry.previous.pitch() + (entry.current.pitch() - entry.previous.pitch()) * partialTick,
                    entry.previous.roll() + (entry.current.roll() - entry.previous.roll()) * partialTick);
        }
    }
}
