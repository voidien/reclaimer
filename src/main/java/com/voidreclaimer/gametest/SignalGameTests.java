package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.link.SignalMath;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Signal on a real server. The maths itself is in the unit tests. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SignalGameTests {
    private SignalGameTests() {}

    /** Every config number lands in its own place: each gets a different value, within one tick, and is put back. */
    @GameTest(template = "empty")
    public static void theSignalRulesReadTheConfig(GameTestHelper helper) {
        List<ModConfigSpec.DoubleValue> values = List.of(Config.LINK_SEAT_RANGE, Config.LINK_RELAY_RANGE, Config.LINK_DYSON_TIER_1_RANGE,
                Config.LINK_DYSON_TIER_2_RANGE, Config.LINK_DYSON_TIER_3_RANGE, Config.LINK_DYSON_TIER_4_RANGE,
                Config.LINK_STATIC_BAND, Config.LINK_STATION_RANGE, Config.LINK_STATION_STATIC_BAND);
        List<Double> trial = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 0.07, 8.0, 0.09);
        List<Double> kept = values.stream().map(ModConfigSpec.DoubleValue::get).toList();
        SignalMath read;
        try {
            for (int i = 0; i < values.size(); i++) {
                values.get(i).set(trial.get(i));
            }
            read = SignalMath.fromConfig();
        } finally {
            for (int i = 0; i < values.size(); i++) {
                values.get(i).set(kept.get(i));
            }
        }
        helper.assertTrue(new SignalMath(1, 2, List.of(3.0, 4.0, 5.0, 6.0), 0.07, 8, 0.09).equals(read), "read wrongly: " + read);
        helper.assertTrue(Config.LINK_STATION_RANGE.getDefault() == 1.5, "the owner's station range is 1.5 times");
        helper.succeed();
    }
}
