package com.voidreclaimer.tether;

/** Server-only, transient: whether the recall key is held and for how long. */
public final class RecallState {
    boolean holding;
    int holdTicks;
}
