package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import com.voidreclaimer.earth.SkyEra;
import com.voidreclaimer.earth.SkyLook;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.terraform.SpireBlockEntity;
import com.voidreclaimer.terraform.SpireTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The planet's sectors: a tower's pulse stays inside its sector and restores it, and the sky follows. The sector map is one
 * value for the whole test world, so each test leaves it as it found it, apart from its own sector. Sectors in use here:
 * 12 and 13. Sector 3 belongs to the CC test, which needs it unrestored.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SectorGameTests {
    private SectorGameTests() {}

    private static boolean ruinedAt(ServerLevel earth, int x, int y, int z) {
        return earth.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).is(RuinedEarth.BIOME);
    }

    /** A loaded spot on Earth, with a ticket for the whole test. */
    private static void onEarth(GameTestHelper helper, ServerLevel earth, BlockPos pos, Runnable body) {
        SpireGameTests.forceChunks(earth, pos, 8, true);
        helper.runAfterDelay(5, () -> {
            try {
                SpireTowerGameTests.clear(earth, pos);
                body.run();
            } finally {
                SpireTowerGameTests.clear(earth, pos);
                SpireGameTests.forceChunks(earth, pos, 8, false);
            }
        });
    }

    private static void forget(ServerLevel earth, int sector) {
        EarthRestoration.set(earth, SectorMap.of(earth).without(sector));
    }

    /** Three blocks from the east edge of sector 12, with a radius of eight: the wave meets the edge on that side only. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aPulseRestoresItsSectorAndStopsAtItsEdge(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        BlockPos center = new BlockPos(-260, 120, 300);
        int sector = Sectors.at(center.getX(), center.getZ());
        helper.assertTrue(sector == 12, "the test's sector moved: " + sector);
        BlockPos spirePos = center.above(3).north(SpireTower.HALF);
        SpireGameTests.forceChunks(earth, center, SpireGameTests.PLATFORM + 4, true);
        Runnable cleanup = () -> {
            SpireTowerGameTests.clear(earth, spirePos);
            SpireGameTests.clearPlatform(earth, center);
            SpireGameTests.forceChunks(earth, center, SpireGameTests.PLATFORM + 4, false);
            forget(earth, sector);
        };
        helper.runAfterDelay(5, () -> {
            try {
                forget(earth, sector);
                SpireGameTests.buildPlatform(earth, center);
                var ruined = earth.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(RuinedEarth.BIOME);
                SpireGameTests.resetBiomes(earth, center, ruined);
                SpireTowerGameTests.clear(earth, spirePos);
                SpireBlockEntity spire = SpireTowerGameTests.build(helper, earth, spirePos);
                helper.assertTrue(spire.sector() == sector, "the tower is in sector " + spire.sector());
                TestEventListeners.SECTORS_RESTORED.remove(Integer.valueOf(sector));
                spire.start(8, 20, sector);
                helper.assertFalse(SectorMap.of(earth).isRestored(sector), "restored before the pulse ended");
                SpireGameTests.until(helper, 400, () -> !spire.running(), () -> {
                    try {
                        int edge = Sectors.minX(sector) + Sectors.SIZE - 1;
                        helper.assertTrue(earth.getBlockState(new BlockPos(edge, 122, center.getZ())).is(Blocks.GRASS_BLOCK), "the sector's last column was not rewritten");
                        helper.assertTrue(earth.getBlockState(new BlockPos(edge + 1, 122, center.getZ())).is(ModBlocks.ASHEN_SLAG.get()), "the wave crossed into the next sector");
                        helper.assertTrue(earth.getBlockState(center.offset(-8, 2, 0)).is(Blocks.GRASS_BLOCK), "the far side was not rewritten");
                        helper.assertFalse(ruinedAt(earth, edge - 1, 122, center.getZ()), "the biome inside the sector is still ruined");
                        helper.assertTrue(ruinedAt(earth, edge + 3, 122, center.getZ()), "the biome crossed into the next sector");
                        SectorMap map = SectorMap.of(earth);
                        helper.assertTrue(map.isRestored(sector), "the sector was not restored");
                        helper.assertTrue(SkyEra.of(earth) == SkyEra.forSectors(map.count()), "the era did not follow: " + SkyEra.of(earth));
                        helper.assertTrue(TestEventListeners.SECTORS_RESTORED.contains(sector), "no SectorRestoredEvent");
                        helper.succeed();
                    } finally {
                        cleanup.run();
                    }
                }, cleanup);
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
        });
    }

    /** A pulse that names no sector, as an old save's does, restores none. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aPulseWithNoSectorRestoresNone(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        BlockPos center = new BlockPos(-460, 200, 460);
        int sector = Sectors.at(center.getX(), center.getZ());
        BlockPos spirePos = center;
        SpireGameTests.forceChunks(earth, center, 8, true);
        Runnable cleanup = () -> {
            SpireTowerGameTests.clear(earth, spirePos);
            SpireGameTests.forceChunks(earth, center, 8, false);
        };
        helper.runAfterDelay(5, () -> {
            try {
                forget(earth, sector);
                SpireTowerGameTests.clear(earth, spirePos);
                SpireBlockEntity spire = SpireTowerGameTests.place(earth, spirePos);
                spire.start(1, 2);
                SpireGameTests.until(helper, 100, () -> !spire.running(), () -> {
                    try {
                        helper.assertFalse(SectorMap.of(earth).isRestored(sector), "a sectorless pulse restored a sector");
                        helper.succeed();
                    } finally {
                        cleanup.run();
                    }
                }, cleanup);
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
        });
    }

    /** One tower's real start: it covers its sector from where it stands, and only there. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aTowerStartsOnlyOverAnUnrestoredSector(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        // Sector 13 covers x -256 to -1, z 256 to 511.
        BlockPos spirePos = new BlockPos(-100, 200, 300);
        onEarth(helper, earth, spirePos, () -> {
            int sector = Sectors.at(spirePos.getX(), spirePos.getZ() + SpireTower.HALF);
            helper.assertTrue(sector == 13, "the test's sector moved: " + sector);
            try {
                SpireBlockEntity spire = SpireTowerGameTests.build(helper, earth, spirePos);
                EarthRestoration.set(earth, SectorMap.of(earth).with(sector));
                helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.SECTOR_RESTORED, "a pulse over a restored sector");
                helper.assertFalse(spire.running(), "running over a restored sector");

                forget(earth, sector);
                helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.NO_CATALYST, "over an unrestored sector it only wants its cost");
                int reach = Sectors.reach(sector, spirePos.getX(), spirePos.getZ() + SpireTower.HALF);
                helper.assertTrue(spire.radius() == reach, "radius " + spire.radius() + ", the sector's far edge is " + reach);
                helper.succeed();
            } finally {
                SpireTowerGameTests.clear(earth, spirePos);
                forget(earth, sector);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aTowerOffThePlanetRefuses(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        BlockPos spirePos = new BlockPos(700, 200, 700);
        onEarth(helper, earth, spirePos, () -> {
            SpireBlockEntity spire = SpireTowerGameTests.build(helper, earth, spirePos);
            helper.assertTrue(spire.sector() == Sectors.NONE && spire.radius() == 0, "a sector off the planet: " + spire.sector());
            helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.OFF_PLANET, "fired off the planet");
            helper.succeed();
        });
    }

    /** The sky blends with the sectors; an era a debug command sets stands until the next sector. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theSkyFollowsTheSectors(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        SectorMap before = SectorMap.of(earth);
        SkyEra eraBefore = SkyEra.of(earth);
        try {
            EarthRestoration.set(earth, new SectorMap(0xFF));
            helper.assertTrue(SkyEra.of(earth) == SkyEra.HEALING, "era at 8 sectors: " + SkyEra.of(earth));
            helper.assertTrue(SkyLook.in(earth).equals(SkyLook.of(SkyEra.HEALING)), "half the planet is the healing look");
            EarthRestoration.set(earth, new SectorMap(0xF));
            helper.assertTrue(SkyLook.in(earth).equals(SkyLook.blend(0.25f)), "a quarter of the planet: " + SkyLook.in(earth));
            var saved = earth.serializeAttachments(earth.registryAccess());
            helper.assertTrue(saved != null && saved.getInt("void_reclaimer:earth_sectors") == 0xF, "the map is not saved with the level: " + saved);

            earth.setData(com.voidreclaimer.registry.ModAttachments.EARTH_SKY_ERA, SkyEra.RESTORED);
            helper.assertTrue(SkyLook.in(earth).equals(SkyLook.of(SkyEra.RESTORED)), "the debug era was ignored");

            EarthRestoration.set(earth, new SectorMap(0xFFFF));
            helper.assertTrue(SkyEra.of(earth) == SkyEra.RESTORED, "era at 16 sectors: " + SkyEra.of(earth));
            EarthRestoration.set(earth, SectorMap.NONE);
            helper.assertTrue(SkyEra.of(earth) == SkyEra.RUINED && SkyLook.in(earth).equals(SkyLook.of(SkyEra.RUINED)), "era at none");
            helper.succeed();
        } finally {
            EarthRestoration.set(earth, before);
            earth.setData(com.voidreclaimer.registry.ModAttachments.EARTH_SKY_ERA, eraBefore);
        }
    }
}
