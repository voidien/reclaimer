package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.terraform.SpireBlockEntity;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 12 slice: the Terraforming Spire and its Genesis Pulse. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpireGameTests {
    private static final int RADIUS = 20;
    private static final int PULSE_TICKS = 40;
    /** The platform reaches this far, past the pulse, so the edge of the area can be told apart. */
    private static final int PLATFORM = 24;

    private SpireGameTests() {}

    private static ServerLevel earth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(RuinedEarth.DIMENSION);
        if (level == null) {
            throw new AssertionError("ruined_earth is not loaded");
        }
        return level;
    }

    private static void forceChunks(ServerLevel level, BlockPos center, int reach, boolean forced) {
        for (int x = (center.getX() - reach) >> 4; x <= (center.getX() + reach) >> 4; x++) {
            for (int z = (center.getZ() - reach) >> 4; z <= (center.getZ() + reach) >> 4; z++) {
                level.setChunkForced(x, z, forced);
            }
        }
    }

    /**
     * Puts the ruined biome back over the test area. Deliberately not {@code Terraforming.rewriteBiomes}: the persistent
     * GameTest world keeps the plains of earlier runs, and the reset must not depend on the code under test.
     */
    private static void resetBiomes(ServerLevel level, BlockPos center, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> ruined) {
        for (int x = (center.getX() - PLATFORM - 8) >> 4; x <= (center.getX() + PLATFORM + 8) >> 4; x++) {
            for (int z = (center.getZ() - PLATFORM - 8) >> 4; z <= (center.getZ() + PLATFORM + 8) >> 4; z++) {
                var chunk = level.getChunk(x, z);
                chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> ruined, level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
            }
        }
    }

    /** A floating patch of ruined ground: three layers of slag on three of stone, with the Spire on top. */
    private static void buildPlatform(ServerLevel level, BlockPos center) {
        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                for (int dy = -3; dy <= 2; dy++) {
                    Block block = dy < 0 ? Blocks.STONE : ModBlocks.ASHEN_SLAG.get();
                    level.setBlock(center.offset(dx, dy, dz), block.defaultBlockState(), 2);
                }
                level.setBlock(center.offset(dx, 3, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void clearPlatform(ServerLevel level, BlockPos center) {
        for (int dx = -PLATFORM; dx <= PLATFORM; dx++) {
            for (int dz = -PLATFORM; dz <= PLATFORM; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }


    /** Checks once per tick until {@code done}, then runs {@code then}; fails the test (after {@code onTimeout}) when the budget runs out. */
    private static void until(GameTestHelper helper, int budget, BooleanSupplier done, Runnable then, Runnable onTimeout) {
        if (done.getAsBoolean()) {
            then.run();
        } else if (budget <= 0) {
            onTimeout.run();
            helper.fail("timed out waiting for the pulse");
        } else {
            helper.runAfterDelay(1, () -> until(helper, budget - 1, done, then, onTimeout));
        }
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aPulseSweepsOutAndRewritesBlocksAndBiomes(GameTestHelper helper) {
        ServerLevel earth = earth(helper);
        BlockPos center = new BlockPos(300, 120, 300);
        forceChunks(earth, center, PLATFORM + 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                buildPlatform(earth, center);
                var ruined = earth.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(RuinedEarth.BIOME);
                resetBiomes(earth, center, ruined);
                BlockPos spirePos = center.above(3);
                earth.setBlock(spirePos, ModBlocks.TERRAFORMING_SPIRE.get().defaultBlockState(), 3);
                BlockEntity entity = earth.getBlockEntity(spirePos);
                helper.assertTrue(entity instanceof SpireBlockEntity, "no Spire block entity");
                SpireBlockEntity spire = (SpireBlockEntity) entity;
                int before = spire.pulses();
                helper.assertTrue(earth.getBlockState(center.offset(10, 2, 0)).is(ModBlocks.ASHEN_SLAG.get()), "platform is not slag");

                spire.start(RADIUS, PULSE_TICKS);
                Runnable cleanup = () -> {
                    earth.setBlock(spirePos, Blocks.AIR.defaultBlockState(), 2);
                    clearPlatform(earth, center);
                    forceChunks(earth, center, PLATFORM + 4, false);
                };
                // Wait on the pulse's own progress, not on fixed delays: a freshly forced chunk can take a while to start ticking.
                until(helper, 400, () -> spire.elapsed() >= 10 || !spire.running(), () -> {
                    try {
                        helper.assertTrue(spire.running() && spire.elapsed() < 16, "pulse was not caught part way: " + spire.elapsed());
                        // Part way: the wave has reached the near columns but not the far ones.
                        helper.assertTrue(earth.getBlockState(center.offset(2, 2, 0)).is(Blocks.GRASS_BLOCK), "near column not rewritten yet");
                        helper.assertTrue(earth.getBlockState(center.offset(RADIUS - 1, 2, 0)).is(ModBlocks.ASHEN_SLAG.get()), "far column rewritten too soon");
                    } catch (RuntimeException e) {
                        cleanup.run();
                        throw e;
                    }
                    until(helper, 400, () -> !spire.running(), () -> {
                        try {
                            helper.assertTrue(spire.pulses() == before + 1, "pulse not counted");
                            BlockPos probe = center.offset(10, 0, 0);
                            helper.assertTrue(earth.getBlockState(probe.above(2)).is(Blocks.GRASS_BLOCK), "no grass on the surface");
                            helper.assertTrue(earth.getBlockState(probe.above(1)).is(Blocks.DIRT), "no dirt under the grass");
                            helper.assertTrue(earth.getBlockState(probe).is(Blocks.DIRT), "slag layer 3 not rewritten");
                            helper.assertTrue(earth.getBlockState(probe.below()).is(Blocks.STONE), "stone below the slag was touched");
                            BlockPos edge = center.offset(RADIUS, 2, RADIUS);
                            helper.assertTrue(earth.getBlockState(edge).is(Blocks.GRASS_BLOCK), "the corner of the area was missed");
                            helper.assertTrue(earth.getBlockState(center.offset(RADIUS + 2, 2, 0)).is(ModBlocks.ASHEN_SLAG.get()), "slag outside the area changed");
                            helper.assertTrue(earth.getBiome(probe).is(Biomes.PLAINS), "biome inside not plains: " + earth.getBiome(probe));
                            helper.assertTrue(earth.getBiome(center.offset(RADIUS + 8, 0, 0)).is(RuinedEarth.BIOME), "biome outside changed");
                            helper.assertTrue(spire.lastChunksResent() > 0, "no chunks were resent");
                            helper.succeed();
                        } finally {
                            cleanup.run();
                        }
                    }, cleanup);
                }, cleanup);
            } catch (RuntimeException e) {
                clearPlatform(earth, center);
                forceChunks(earth, center, PLATFORM + 4, false);
                throw e;
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aSpireOutsideRuinedEarthRefuses(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos pos = new BlockPos(13_400, 64, 0);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                space.setBlock(pos, ModBlocks.TERRAFORMING_SPIRE.get().defaultBlockState(), 3);
                SpireBlockEntity spire = (SpireBlockEntity) space.getBlockEntity(pos);
                helper.assertTrue(spire.tryStart() == SpireBlockEntity.Start.WRONG_WORLD, "a Spire started in deep space");
                helper.assertFalse(spire.running(), "running in deep space");
                helper.succeed();
            } finally {
                space.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aPulseInProgressSurvivesASaveAndLoad(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos pos = new BlockPos(13_450, 64, 0);
        space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                space.setBlock(pos, ModBlocks.TERRAFORMING_SPIRE.get().defaultBlockState(), 3);
                SpireBlockEntity spire = (SpireBlockEntity) space.getBlockEntity(pos);
                spire.start(RADIUS, PULSE_TICKS);
                var tag = spire.saveWithFullMetadata(space.registryAccess());
                BlockEntity loaded = BlockEntity.loadStatic(pos, space.getBlockState(pos), tag, space.registryAccess());
                helper.assertTrue(loaded instanceof SpireBlockEntity restored && restored.running(), "the pulse did not survive a reload");
                helper.succeed();
            } finally {
                space.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                space.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            }
        });
    }
}
