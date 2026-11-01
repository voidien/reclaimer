package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.helper.Mining;
import com.voidreclaimer.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Helper drones mining asteroid rock. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperMiningGameTests {
    private HelperMiningGameTests() {}

    private static final int Y = 80;

    private static ChestBlockEntity chest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        return (ChestBlockEntity) level.getBlockEntity(pos);
    }

    private static int count(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        int total = 0;
        for (int i = 0; handler != null && i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeDrones(ServerLevel level, BlockPos around) {
        level.getEntitiesOfClass(HelperDrone.class, new AABB(around).inflate(40)).forEach(HelperDrone::discard);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(40)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void aDroneMinesTheRocksNearItsMinePointIntoItsChest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_400, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 8, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, pos);
            BlockPos mine = pos.east(4);
            BlockPos[] rocks = {mine, mine.east(), mine.east(2).above()};
            for (BlockPos rock : rocks) {
                level.setBlock(rock, ModBlocks.ASTEROID_SILICATE.get().defaultBlockState(), 3);
            }
            BlockPos stone = mine.east(3);
            level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            BlockPos chestPos = pos.west(3);
            chest(level, chestPos);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos).add(0, 2, 0),
                    new HelperLinks(Optional.empty(), Optional.of(chestPos), Optional.of(mine)), null);
            helper.assertTrue(drone.links().mining(), "a drone with a mine point does not mine");
            // Three rocks at 40 dig ticks each plus short flights: well under 600 ticks. A batch lasts as long as its slowest
            // test, so this goes on as soon as they are in. The wait after that is two of the drone's scans and a dig: a drone
            // that wrongly wanted the stone would have started on it by then.
            helper.startSequence()
                    .thenWaitUntil(() -> {
                        int mined = count(level, chestPos, ModBlocks.ASTEROID_SILICATE.get().asItem());
                        helper.assertTrue(mined == 3, "rocks delivered: " + mined + ", drone " + drone.minePhase() + " carrying " + drone.carried());
                        for (BlockPos rock : rocks) {
                            helper.assertTrue(level.getBlockState(rock).isAir(), "rock left at " + rock);
                        }
                    })
                    .thenExecuteAfter(120, () -> {
                        helper.assertTrue(level.getBlockState(stone).is(Blocks.STONE), "the drone took a stone that is not asteroid rock");
                        helper.assertTrue(drone.minePhase() == Mining.Phase.WAITING, "with no rock left the drone should wait: " + drone.minePhase());
                        level.setBlock(stone, Blocks.AIR.defaultBlockState(), 3);
                        removeDrones(level, pos);
                    })
                    .thenSucceed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aMinerWithNoRockInReachWaitsWhereItIs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_500, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 8, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, pos);
            BlockPos chestPos = pos.west(3);
            chest(level, chestPos);
            Vec3 where = Vec3.atCenterOf(pos).add(0, 2, 0);
            HelperDrone drone = HelperDrone.spawn(level, where, new HelperLinks(Optional.empty(), Optional.of(chestPos), Optional.of(pos.east(4))), null);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(drone.minePhase() == Mining.Phase.WAITING, "no rock: " + drone.minePhase());
                helper.assertTrue(drone.position().distanceTo(where) < 0.01, "a waiting miner moved to " + drone.position());
                helper.assertTrue(drone.rock() == null, "found a rock in empty space: " + drone.rock());
                removeDrones(level, pos);
                helper.succeed();
            });
        });
    }
}
