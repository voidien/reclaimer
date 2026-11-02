package com.voidreclaimer.flight;

import com.voidreclaimer.api.body.ModuleStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How an Afterburner stands. A burn heats it and rest cools it, so a short burn only needs a short rest, and one held to the end locks it until
 * it is cold. Pure, so it can be unit tested.
 *
 * @param heat        0 cold, 1 a full burn
 * @param burning     burning this tick
 * @param overheated  burned to the end, and locked until cold
 * @param speedFactor what top speed is multiplied by; after a burn it falls back to 1 over {@link #FADE_TICKS} rather than stopping a body dead
 */
public record AfterburnerState(float heat, boolean burning, boolean overheated, float speedFactor) {
    public static final AfterburnerState COLD = new AfterburnerState(0f, false, false, 1f);
    private static final float TICKS_PER_SECOND = (float) FlightStats.TICKS_PER_SECOND;
    /** Ticks a full +50% takes to fade. */
    private static final float FADE_TICKS = 20f;
    private static final float FADE_PER_TICK = 0.5f / FADE_TICKS;

    public static final StreamCodec<ByteBuf, AfterburnerState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, AfterburnerState::heat,
            ByteBufCodecs.BOOL, AfterburnerState::burning,
            ByteBufCodecs.BOOL, AfterburnerState::overheated,
            ByteBufCodecs.FLOAT, AfterburnerState::speedFactor,
            AfterburnerState::new);

    /** The next tick. {@code spec} is null for a body with no Afterburner, which only lets the last one fade and cool. */
    public AfterburnerState tick(boolean wanted, ModuleStats.Afterburner spec) {
        if (spec == null) {
            float factor = fadedFactor();
            return factor == 1f ? COLD : new AfterburnerState(0f, false, false, factor);
        }
        boolean burn = wanted && !overheated;
        float next = burn ? Math.min(1f, heat + 1f / (spec.burnSeconds() * TICKS_PER_SECOND))
                : Math.max(0f, heat - 1f / (spec.cooldownSeconds() * TICKS_PER_SECOND));
        boolean locked = next <= 0f ? false : overheated || next >= 1f;
        return new AfterburnerState(next, burn, locked, burn ? spec.speedFactor() : fadedFactor());
    }

    /** A step down, ending on exactly 1: a sum of small floats would stop just over it and read as still fading. */
    private float fadedFactor() {
        float factor = speedFactor - FADE_PER_TICK;
        return factor < 1f + FADE_PER_TICK / 2f ? 1f : factor;
    }

    /** Over the body's own top speed but not burning: the server's speed check waits this out. */
    public boolean fading() {
        return !burning && speedFactor > 1f;
    }
}
