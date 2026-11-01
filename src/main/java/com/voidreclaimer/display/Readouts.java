package com.voidreclaimer.display;

import com.voidreclaimer.api.holo.HoloReadout;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The formatters of the built-in readouts: snapshots to lines, nothing else, so every number and every cap is tested
 * without a world. The samplers that fill the snapshots are in {@link ReadoutKinds}.
 */
public final class Readouts {
    /** Ticks in a Minecraft day. */
    private static final long DAY = 24_000;

    private Readouts() {}

    /** What a kind shows now; an unknown kind shows nothing. */
    public static List<String> lines(@Nullable HoloReadout<?> kind, Level level, BlockPos pos) {
        return kind == null ? List.of() : kind.lines(level, pos);
    }

    /** The source after {@code current} in id order, wrapping round; an unknown current starts from the first. */
    public static ResourceLocation next(List<ResourceLocation> ids, ResourceLocation current) {
        List<ResourceLocation> sorted = ids.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
        int index = sorted.indexOf(current);
        return sorted.get((index + 1) % sorted.size());
    }

    /** A number with one decimal, the same in every locale. */
    public static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** A share from 0 to 1 as a whole percentage. */
    public static String percent(double share) {
        return Math.round(Math.max(0, Math.min(1, share)) * 100) + "%";
    }

    /** Game time as the clock readout shows it. */
    public record Clock(long gameTime) {}

    public static List<String> clock(Clock clock) {
        long day = clock.gameTime / DAY;
        long ofDay = clock.gameTime % DAY;
        long hours = ofDay / 1000;
        long minutes = ofDay % 1000 * 60 / 1000;
        return List.of("MISSION CLOCK", String.format(Locale.ROOT, "Day %d  %02d:%02d", day, hours, minutes));
    }
}
