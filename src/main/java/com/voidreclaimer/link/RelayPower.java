package com.voidreclaimer.link;

/** When a relay runs on the Forge Energy it holds. Pure, so it can be unit tested. */
public final class RelayPower {
    /**
     * A dark relay waits for this many ticks of energy, so a supply just short of what it uses gives a few seconds on and a few off, and
     * not a block that flips every other tick.
     */
    public static final int RESTART_TICKS = 20;

    private RelayPower() {}

    /** Whether a relay holding {@code stored} runs this tick. No buffer can wait for more than it holds. */
    public static boolean runs(int stored, boolean lit, int perTick, int buffer) {
        return stored >= Math.min(lit ? perTick : perTick * RESTART_TICKS, buffer);
    }

    /** What a relay that ran this tick has left. */
    public static int after(int stored, int perTick) {
        return Math.max(0, stored - perTick);
    }
}
