package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.flight.FlightStats;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProbeGameTests {
    @GameTest(template = "empty")
    public static void probePresetThrust(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 26_000.5, 80, 0.5);
        try {
            for (var key : java.util.List.of(BuiltinBodies.DRONE, BuiltinBodies.MECH, BuiltinBodies.SHIP, BuiltinBodies.SHUTTLE, BuiltinBodies.TITAN)) {
                for (boolean full : new boolean[] {false, true}) {
                    Bodies.store(player, Bodies.preset(key, full));
                    FlightStats f = FlightStats.of(player);
                    VoidReclaimer.LOGGER.info("PROBE {} full={} mass={} thrust={} accel={} brake={} maxSpeed={}", key.location().getPath(), full,
                            f.mass(), f.thrust(), f.acceleration(), f.brakeDeceleration(), f.maxSpeed());
                }
            }
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
