package com.voidreclaimer.dust;

import javax.annotation.Nullable;

/** Server-side, per player: when the next pulse is allowed and the echo still travelling back. */
public final class SensorState {
    long nextPulseTick;
    @Nullable
    SensorPulse.Echo pendingEcho;
    long echoDueTick;

    public boolean hasPendingEcho() {
        return pendingEcho != null;
    }
}
