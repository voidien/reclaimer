package com.voidreclaimer.link;

import com.voidreclaimer.Config;
import java.util.List;

/**
 * How far a link reaches and how clean it is there, decoupled from the config so it can be unit tested.
 *
 * <p>A node covers a sphere of its range. Each Dyson tier reached adds its own amount to every node. A seat at a station is a
 * hub: its range is multiplied and static starts later across its network. Static fills the outer {@code staticBand} of a range:
 * signal strength is 1 inside it and falls in a straight line to 0 at the edge.
 *
 * @param seatRange        blocks a seat reaches with no Dyson tier
 * @param relayRange       blocks a relay reaches with no Dyson tier
 * @param dysonRanges      blocks added by tier 1, tier 2 and so on; a tier adds its own and every tier below it
 * @param staticBand       the outer share of a range that carries static, 0 to 1
 * @param stationRange     a seat at a station multiplies its range by this
 * @param stationStaticBand {@code staticBand} for a seat at a station
 */
public record SignalMath(double seatRange, double relayRange, List<Double> dysonRanges, double staticBand,
                         double stationRange, double stationStaticBand) {
    public SignalMath {
        dysonRanges = List.copyOf(dysonRanges);
    }

    public static SignalMath fromConfig() {
        return new SignalMath(
                Config.LINK_SEAT_RANGE.get(),
                Config.LINK_RELAY_RANGE.get(),
                List.of(Config.LINK_DYSON_TIER_1_RANGE.get(), Config.LINK_DYSON_TIER_2_RANGE.get(),
                        Config.LINK_DYSON_TIER_3_RANGE.get(), Config.LINK_DYSON_TIER_4_RANGE.get()),
                Config.LINK_STATIC_BAND.get(),
                Config.LINK_STATION_RANGE.get(),
                Config.LINK_STATION_STATIC_BAND.get());
    }

    /** What the Dyson Sphere adds to every node at {@code tier}. */
    public double dysonBonus(int tier) {
        double bonus = 0;
        for (int i = 0; i < Math.min(tier, dysonRanges.size()); i++) {
            bonus += dysonRanges.get(i);
        }
        return bonus;
    }

    /** A seat's range at {@code dysonTier}; {@code atStation} is the hub perk. */
    public double seatRange(int dysonTier, boolean atStation) {
        double range = seatRange + dysonBonus(dysonTier);
        return atStation ? range * stationRange : range;
    }

    /** A relay's range at {@code dysonTier}. */
    public double relayRange(int dysonTier) {
        return relayRange + dysonBonus(dysonTier);
    }

    /** The share of every range in this seat's network that carries static. */
    public double staticBand(boolean atStation) {
        return atStation ? stationStaticBand : staticBand;
    }

    /** Signal strength, 0 to 1, at {@code distance} from a node of {@code range}; 0 at the edge and beyond, unless there is no band. */
    public static double strength(double distance, double range, double band) {
        if (range <= 0 || distance > range) {
            return 0;
        }
        double fade = range * Math.min(band, 1);
        return fade <= 0 ? 1 : Math.min(1, (range - distance) / fade);
    }

    /** How much static a link of this strength carries, 0 (clean) to 1 (about to drop). */
    public static double staticLevel(double strength) {
        return 1 - Math.max(0, Math.min(1, strength));
    }
}
