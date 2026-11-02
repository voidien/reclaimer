package com.voidreclaimer.weather;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.resources.ResourceLocation;

/** How weather comes and goes, a second at a time. Pure, so it can be unit tested. */
public final class WeatherClock {
    /** A kind of weather that could come now. */
    public record Candidate(ResourceLocation id, int weight, int warningSeconds, int minSeconds, int maxSeconds) {}

    /** The dice: a number from 0 up to but not including the bound. */
    public interface Dice {
        int roll(int bound);
    }

    private WeatherClock() {}

    /** The state a second after {@code state}. {@code gap} says how long until the next one, and only what could come now is in {@code candidates}. */
    public static WeatherState second(WeatherState state, List<Candidate> candidates, IntSupplier gap, Dice dice) {
        if (state.warning()) {
            return new WeatherState(state.weather(), state.warningLeft() - 1, state.activeLeft(), state.strength(), state.nextIn());
        }
        if (state.active()) {
            return state.activeLeft() <= 1 ? new WeatherState(Optional.empty(), 0, 0, 0f, gap.getAsInt())
                    : new WeatherState(state.weather(), 0, state.activeLeft() - 1, state.strength(), state.nextIn());
        }
        if (state.nextIn() < 0) {
            return new WeatherState(Optional.empty(), 0, 0, 0f, gap.getAsInt());
        }
        if (state.nextIn() > 0) {
            return new WeatherState(Optional.empty(), 0, 0, 0f, state.nextIn() - 1);
        }
        return pick(candidates, dice).map(next -> start(next, dice)).orElseGet(() -> new WeatherState(Optional.empty(), 0, 0, 0f, gap.getAsInt()));
    }

    /** This weather, at a strength from one half up, with its warning to run. */
    public static WeatherState start(Candidate next, Dice dice) {
        int length = next.minSeconds() + dice.roll(Math.max(next.maxSeconds() - next.minSeconds(), 0) + 1);
        return new WeatherState(Optional.of(next.id()), next.warningSeconds(), Math.max(length, 1), 0.5f + dice.roll(51) / 100f, -1);
    }

    private static Optional<Candidate> pick(List<Candidate> candidates, Dice dice) {
        int total = candidates.stream().mapToInt(candidate -> Math.max(candidate.weight(), 0)).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = dice.roll(total);
        for (Candidate candidate : candidates) {
            roll -= Math.max(candidate.weight(), 0);
            if (roll < 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
