package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.earth.AcidDrizzle;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Acid Drizzle wears a body under the open sky on ruined Earth, through the ordinary repairable wear. Bodies stand high over sector 15. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AcidDrizzleGameTests {
    /** Enough seconds for the slowest case here to pass a 1% step on every section. */
    private static final int SECONDS = 2_000;

    private AcidDrizzleGameTests() {}

    private interface Test {
        void run(ServerLevel earth, ServerPlayer player, BlockPos pos);
    }

    private static void onEarth(GameTestHelper helper, int x, Test test) {
        ServerLevel earth = SpireGameTests.earth(helper);
        BlockPos pos = new BlockPos(x, 220, 440);
        SpireGameTests.forceChunks(earth, pos, 2, true);
        helper.runAfterDelay(5, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 0.5, 100, 0.5);
            try {
                player.teleportTo(earth, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                test.run(earth, player, pos);
            } finally {
                TestPlayers.remove(player);
                SpireGameTests.forceChunks(earth, pos, 2, false);
            }
        });
    }

    private static void seconds(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            AcidDrizzle.second(player);
        }
    }

    private static boolean worn(ServerPlayer player) {
        return Bodies.data(player).sections().values().stream().anyMatch(section -> Wear.condition(section) < 1f);
    }

    private static boolean allWorn(ServerPlayer player) {
        return Bodies.data(player).sections().values().stream().allMatch(section -> Wear.condition(section) < 1f);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void drizzleWearsEverySectionUnderTheOpenSkyAndNeverBreaksOne(GameTestHelper helper) {
        onEarth(helper, 300, (earth, player, pos) -> {
            helper.assertTrue(EarthRestoration.map(earth.getServer()).count() < Sectors.COUNT, "the test needs an unrestored planet");
            helper.assertTrue(!Bodies.data(player).sections().isEmpty(), "a test body has sections");
            seconds(player, SECONDS);
            helper.assertTrue(allWorn(player), "not every section wore");
            seconds(player, 3_000_000 / 100);
            helper.assertTrue(Bodies.data(player).sections().values().stream().allMatch(section -> !section.isEmpty()), "a section broke");
            helper.assertTrue(player.isAlive() && player.getHealth() == player.getMaxHealth(), "the drizzle hurt the body");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aRoofKeepsTheDrizzleOff(GameTestHelper helper) {
        onEarth(helper, 332, (earth, player, pos) -> {
            BlockPos roof = pos.above(4);
            earth.setBlock(roof, Blocks.STONE.defaultBlockState(), 3);
            try {
                helper.assertFalse(AcidDrizzle.underOpenSky(player), "a roof overhead reads as open sky");
                seconds(player, SECONDS);
                helper.assertFalse(worn(player), "wore under a roof");
            } finally {
                earth.setBlock(roof, Blocks.AIR.defaultBlockState(), 3);
            }
            helper.assertTrue(AcidDrizzle.underOpenSky(player), "the sky did not open again");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restoredGroundHasNoDrizzle(GameTestHelper helper) {
        onEarth(helper, 364, (earth, player, pos) -> {
            int sector = Sectors.at(pos.getX(), pos.getZ());
            EarthRestoration.set(earth, SectorMap.of(earth).with(sector));
            try {
                seconds(player, SECONDS);
                helper.assertFalse(worn(player), "wore over a restored sector");
                helper.succeed();
            } finally {
                EarthRestoration.set(earth, SectorMap.of(earth).without(sector));
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theConfigSwitchesItOff(GameTestHelper helper) {
        onEarth(helper, 396, (earth, player, pos) -> {
            Config.EARTH_ACID_DRIZZLE.set(false);
            try {
                seconds(player, SECONDS);
                helper.assertFalse(worn(player), "wore with the drizzle switched off");
                helper.succeed();
            } finally {
                Config.EARTH_ACID_DRIZZLE.set(true);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void thereIsNoDrizzleInDeepSpace(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 133_000.5, 100, 0.5);
        try {
            seconds(player, SECONDS);
            helper.assertFalse(worn(player), "wore in deep space");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
