package com.voidreclaimer.body.assembly;

import com.voidreclaimer.body.BodyStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What would change between two bodies, pure: the rows the assembly view shows while a part is hovered. */
public final class StatDelta {
    /** Below this a stat counts as unchanged, so rounding never shows as a change. */
    private static final double SAME = 1.0E-4;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final int MAX_DECIMALS = 4;
    /** 0.03 - 0.029 comes out a hair under 0.001, which would ask for a decimal too many. */
    private static final double ROUNDING = 1.0E-9;

    /**
     * One changed stat.
     *
     * @param key    the last part of its lang key, {@code assembly.void_reclaimer.stat.<key>}
     * @param better whether the change is an improvement: more mass, for instance, is not
     */
    public record Row(String key, double before, double after, boolean better) {}

    private StatDelta() {}

    /** "before > after", with enough decimals to show the change: a small one must not read 5.0 > 5.0. */
    public static String text(Row row) {
        double change = Math.abs(row.after() - row.before());
        int decimals = change >= 1.0 ? 0 : Math.min(MAX_DECIMALS, (int) Math.ceil(-Math.log10(change) - ROUNDING));
        String format = "%." + decimals + "f";
        return String.format(Locale.ROOT, format + " > " + format, row.before(), row.after());
    }

    /** The stats that differ, in a fixed order; none when the part changes nothing. */
    public static List<Row> between(BodyStats before, BodyStats after) {
        List<Row> rows = new ArrayList<>();
        add(rows, "mass", before.mass(), after.mass(), false);
        add(rows, "budget", before.massBudget(), after.massBudget(), true);
        add(rows, "acceleration", before.flight().acceleration(), after.flight().acceleration(), true);
        add(rows, "top_speed", before.flight().maxSpeed() * TICKS_PER_SECOND, after.flight().maxSpeed() * TICKS_PER_SECOND, true);
        add(rows, "energy", before.energyCapacity(), after.energyCapacity(), true);
        add(rows, "solar", before.solarRate(), after.solarRate(), true);
        add(rows, "health", before.maxHealth() / 2.0, after.maxHealth() / 2.0, true);
        add(rows, "reach", before.reach(), after.reach(), true);
        add(rows, "intake", before.dustIntake(), after.dustIntake(), true);
        add(rows, "dust", before.dustCapacity(), after.dustCapacity(), true);
        add(rows, "rows", before.rows(), after.rows(), true);
        add(rows, "stability", before.stability(), after.stability(), true);
        add(rows, "turn_rate", before.turnRate(), after.turnRate(), true);
        return rows;
    }

    private static void add(List<Row> rows, String key, double before, double after, boolean moreIsBetter) {
        if (Math.abs(after - before) > SAME) {
            rows.add(new Row(key, before, after, (after > before) == moreIsBetter));
        }
    }
}
