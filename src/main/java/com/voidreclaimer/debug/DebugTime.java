package com.voidreclaimer.debug;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;

/**
 * Time controls for checks, through the server's tick manager (what {@code /tick} uses): freeze the world, step it, or slow it,
 * so animations and timing can be looked at. Players keep moving while the world is frozen. Call on the server thread.
 */
public final class DebugTime {
    public static final float NORMAL_RATE = 20f;
    /** Quarter speed. */
    public static final float SLOW_RATE = 5f;

    private DebugTime() {}

    /** Freezes the world, or lets it run again. */
    public static void setFrozen(MinecraftServer server, boolean frozen) {
        server.tickRateManager().setFrozen(frozen);
    }

    /** Runs a frozen world on for this many ticks. */
    public static void step(MinecraftServer server, int ticks) {
        ServerTickRateManager manager = server.tickRateManager();
        if (!manager.isFrozen()) {
            manager.setFrozen(true);
        }
        manager.stepGameIfPaused(ticks);
    }

    public static void setSlow(MinecraftServer server, boolean slow) {
        server.tickRateManager().setTickRate(slow ? SLOW_RATE : NORMAL_RATE);
    }

    /** Normal time again. */
    public static void reset(MinecraftServer server) {
        ServerTickRateManager manager = server.tickRateManager();
        manager.stopStepping();
        manager.setFrozen(false);
        manager.setTickRate(NORMAL_RATE);
    }

    /** One line for the debug panel, or empty at normal time. */
    public static String describe(MinecraftServer server) {
        ServerTickRateManager manager = server.tickRateManager();
        String rate = manager.tickrate() == NORMAL_RATE ? "" : manager.tickrate() + " ticks a second";
        if (manager.isFrozen()) {
            return manager.isSteppingForward() ? "Time stepping " + rate : "Time frozen " + rate;
        }
        return rate.isEmpty() ? "" : "Time at " + rate;
    }
}
