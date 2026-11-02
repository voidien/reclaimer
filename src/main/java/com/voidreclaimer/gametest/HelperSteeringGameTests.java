package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.helper.Ferry;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Helper drones steering round blocks. Each test sits in the middle of its own chunk, which is the only one sure to tick
 * entities, and keeps its drone inside that chunk.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperSteeringGameTests {
    private HelperSteeringGameTests() {}

    /** A Titan shell's saucer is as solid as a wall to a drone, far from where the shell stands, and the cell beyond it is free. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDroneKeepsOutOfABigBody(GameTestHelper helper) {
        net.minecraft.core.BlockPos origin = new net.minecraft.core.BlockPos(97_200, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            net.minecraft.server.level.ServerLevel space = TestPlayers.deepSpace(helper);
            var owner = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            var drone = HelperDrone.spawn(space, net.minecraft.world.phys.Vec3.atCenterOf(origin.offset(0, 13, 24)), HelperLinks.EMPTY, owner.getUUID());
            int x = origin.getX(), y = origin.getY() + 13, z = origin.getZ() + 25;
            helper.assertTrue(drone.canFlyThrough(x, y, z), "the empty cell is not free");
            var shell = com.voidreclaimer.link.BodyHost.spawn(space, origin.getBottomCenter(), com.voidreclaimer.link.HostRole.SHELL,
                    com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN), owner.getUUID());
            shell.setYRot(0f);
            shell.setXRot(0f);
            helper.runAfterDelay(12, () -> {
                helper.assertFalse(drone.canFlyThrough(x, y, z), "a drone would fly into the saucer");
                helper.assertTrue(drone.canFlyThrough(x, y + 20, z), "a cell far above the saucer counts as the body");
                shell.discard();
                drone.discard();
                TestPlayers.remove(owner);
                helper.succeed();
            });
        });
    }

    private static final int Y = 80, Z = 4_008;

    private static Container chest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        return (Container) level.getBlockEntity(pos);
    }

    private static int stones(ServerLevel level, BlockPos pos) {
        return ((Container) level.getBlockEntity(pos)).countItem(Items.STONE);
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block) {
        BlockPos.betweenClosed(from, to).forEach(pos -> level.setBlock(pos, block.defaultBlockState(), 3));
    }

    private static void clear(ServerLevel level, BlockPos around) {
        level.getEntitiesOfClass(HelperDrone.class, new AABB(around).inflate(24)).forEach(HelperDrone::discard);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(24)).forEach(ItemEntity::discard);
        fill(level, around.offset(-7, -7, -7), around.offset(7, 7, 7), Blocks.AIR);
    }

    private static boolean inBlock(ServerLevel level, HelperDrone drone) {
        return !level.getBlockState(drone.blockPosition()).getCollisionShape(level, drone.blockPosition()).isEmpty();
    }

    /** Runs {@code check} every tick for {@code ticks} ticks. From inside a running step: {@code onEachTick} loses entries there. */
    private static void eachTick(GameTestHelper helper, int ticks, Runnable check) {
        if (ticks > 0) {
            helper.runAfterDelay(1, () -> {
                check.run();
                eachTick(helper, ticks - 1, check);
            });
        }
    }

    private static String statusKey(HelperDrone drone) {
        return ((TranslatableContents) drone.status().getContents()).getKey();
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aDroneFliesRoundAWallInsteadOfThroughIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_008, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            fill(level, pos.offset(0, -4, -6), pos.offset(0, 4, 6), Blocks.STONE);
            BlockPos a = pos.west(4), b = pos.east(4);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 5));
            chest(level, b);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(a.above()), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            boolean[] hit = new boolean[1];
            boolean[] detoured = new boolean[1];
            eachTick(helper, 199, () -> {
                hit[0] |= inBlock(level, drone);
                detoured[0] |= !drone.waypoints().isEmpty();
            });
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(stones(level, b) == 5, "stones delivered: " + stones(level, b) + ", drone at " + drone.position() + " " + drone.phase());
                helper.assertTrue(!hit[0], "the drone flew into the wall");
                helper.assertTrue(detoured[0], "the drone never took a detour");
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aDroneWithNoWayHoldsStillSaysSoAndGoesOnWhenOneOpens(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_104, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            // A sealed stone box round the destination, with room inside.
            BlockPos b = pos.east(3);
            fill(level, b.offset(-3, -3, -3), b.offset(3, 3, 3), Blocks.STONE);
            fill(level, b.offset(-2, -2, -2), b.offset(2, 2, 2), Blocks.AIR);
            chest(level, b);
            BlockPos a = pos.west(6);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 5));
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(a.above()), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            helper.runAfterDelay(100, () -> {
                helper.assertTrue(drone.phase() == Ferry.Phase.TO_DESTINATION, "phase: " + drone.phase());
                helper.assertTrue(drone.blocked(), "the drone should have given up, at " + drone.position());
                helper.assertTrue(statusKey(drone).endsWith(".status.blocked"), "status: " + statusKey(drone));
                helper.assertTrue(!drone.moving(), "a blocked drone still shows thrust");
                helper.assertTrue(drone.carried().getCount() == 5, "cargo: " + drone.carried());
                Vec3 held = drone.position();
                // Past a retry: it looks again and holds again, without moving.
                helper.runAfterDelay(130, () -> {
                    helper.assertTrue(drone.position().equals(held), "a blocked drone moved from " + held + " to " + drone.position());
                    helper.assertTrue(drone.blocked(), "it should still say so");
                    fill(level, b.offset(-3, 0, -1), b.offset(-3, 1, 1), Blocks.AIR);
                    fill(level, b.offset(-3, 0, 2), b.offset(-3, 1, 3), Blocks.AIR);
                    helper.runAfterDelay(250, () -> {
                        helper.assertTrue(stones(level, b) == 5, "stones delivered once the box opened: " + stones(level, b) + ", drone at " + drone.position());
                        clear(level, pos);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aDroneWithABlockSetOnItFindsItsWayOut(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_200, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            BlockPos a = pos.west(5), b = pos.east(5);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 5));
            chest(level, b);
            fill(level, pos.offset(-1, -1, -1), pos.offset(1, 1, 1), Blocks.STONE);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(!inBlock(level, drone), "still inside the stone at " + drone.position());
                helper.assertTrue(!drone.blocked(), "it should be on its way");
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aChestWithABlockOnTopIsServedFromItsSide(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_312, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            BlockPos a = pos.west(3), b = pos.east(3);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 5));
            chest(level, b);
            // The stone roof is three thick, out of reach from above.
            fill(level, b.above(), b.above(3), Blocks.STONE);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(a.above()), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(stones(level, b) == 5, "stones delivered: " + stones(level, b) + ", drone at " + drone.position());
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aDroneCountsBlocksAndTheWorldsEdgesAndNeverLoadsAChunkToLook(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_504, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above(2)), HelperLinks.EMPTY, null);
            helper.assertTrue(!drone.canFlyThrough(pos.getX(), pos.getY(), pos.getZ()), "stone is free");
            helper.assertTrue(drone.canFlyThrough(pos.getX(), pos.getY() + 1, pos.getZ()), "air is not free");
            helper.assertTrue(!drone.canFlyThrough(pos.getX(), level.getMaxBuildHeight() + 5, pos.getZ()), "above the build limit is free");
            int far = -777_776;
            helper.assertTrue(!level.hasChunk(far >> 4, far >> 4), "the far chunk is already loaded");
            helper.assertTrue(!drone.canFlyThrough(far, Y, far), "a chunk that is not loaded counts as free");
            helper.assertTrue(!level.hasChunk(far >> 4, far >> 4), "looking at a cell loaded its chunk");
            clear(level, pos);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aMinerSkipsTheBuriedRockAtItsMinePoint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_408, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            fill(level, pos.offset(-1, -1, -1), pos.offset(1, 1, 1), ModBlocks.ASTEROID_SILICATE.get());
            BlockPos chestPos = pos.west(6);
            chest(level, chestPos);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above(4)),
                    new HelperLinks(Optional.empty(), Optional.of(chestPos), Optional.of(pos)), null);
            helper.runAfterDelay(30, () -> {
                helper.assertTrue(drone.rock() != null, "no rock chosen");
                helper.assertTrue(!drone.rock().equals(pos), "the drone chose the rock with no open face");
                helper.assertTrue(!drone.blocked(), "a rock with an open face is reachable, at " + drone.position());
                clear(level, pos);
                helper.succeed();
            });
        });
    }
}
