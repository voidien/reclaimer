package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.debug.DebugTime;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The debug time controls reach the server and put everything back. Each check undoes itself in the same tick: other tests are running. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DebugTimeGameTests {
    private DebugTimeGameTests() {}

    @GameTest(template = "empty")
    public static void timeFreezesSlowsAndComesBackToNormal(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            DebugTime.setFrozen(server, true);
            helper.assertTrue(server.tickRateManager().isFrozen(), "not frozen");
            helper.assertTrue(DebugTime.describe(server).startsWith("Time frozen"), DebugTime.describe(server));
            DebugTime.setFrozen(server, false);
            DebugTime.setSlow(server, true);
            helper.assertTrue(server.tickRateManager().tickrate() == DebugTime.SLOW_RATE, "not slow");
            helper.assertTrue(DebugTime.describe(server).startsWith("Time at "), DebugTime.describe(server));
            DebugTime.step(server, 1);
            helper.assertTrue(server.tickRateManager().isFrozen(), "a step freezes a running world first");
        } finally {
            DebugTime.reset(server);
        }
        helper.assertFalse(server.tickRateManager().isFrozen(), "still frozen");
        helper.assertTrue(server.tickRateManager().tickrate() == DebugTime.NORMAL_RATE, "still slow");
        helper.assertTrue(DebugTime.describe(server).isEmpty(), "normal time has no line: " + DebugTime.describe(server));
        helper.succeed();
    }
}
