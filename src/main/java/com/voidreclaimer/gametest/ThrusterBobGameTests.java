package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.flight.ThrusterFlight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThrusterBobGameTests {
    private ThrusterBobGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void restingOnTheFloorDoesNotBobTheView(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 96_000.5, 80, 0.5);
        try {
            // What vanilla's per-tick bob update leaves behind after the body touched something while moving.
            player.bob = 0.1f;
            player.oBob = 0.1f;
            ThrusterFlight.calmWalkBob(player);
            helper.assertTrue(player.bob == 0f && player.oBob == 0f, "walk bob stayed at " + player.bob + " / " + player.oBob);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
