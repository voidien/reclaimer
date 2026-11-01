package com.voidreclaimer.client.input;

/** Leaving a shell takes two presses of the key, close together, so a stray press does not end a link. */
final class LeaveConfirm {
    /** Ticks a first press waits for the second. */
    static final int WINDOW_TICKS = 60;
    private static final long NEVER = Long.MIN_VALUE / 2;

    private long armedAt = NEVER;

    /** A press at {@code tick}: true when it is the second, so the shell should be left now. */
    boolean press(long tick) {
        if (tick - armedAt <= WINDOW_TICKS) {
            armedAt = NEVER;
            return true;
        }
        armedAt = tick;
        return false;
    }
}
