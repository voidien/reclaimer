package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.helper.Ferry;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperDroneItem;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.launch.LaunchComplex;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import com.voidreclaimer.launch.LaunchPad;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.registry.ModMaterials;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Helper drones ferrying items. Each test has its own chunk; drones tick on their own once the chunk ticks entities. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperDroneGameTests {
    private HelperDroneGameTests() {}

    private static final int Y = 80;

    /** A fresh, empty chest: the world persists between runs, and setting a chest over a chest keeps the old contents. */
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

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aDroneFerriesItemsBetweenTwoChests(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_000, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 4, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, pos);
            BlockPos a = pos, b = pos.east(6);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 20));
            chest(level, b);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.east(3)).add(0, 2, 0), HelperLinks.EMPTY.withSource(a).withDestination(b), null);
            helper.assertTrue(drone.phase() == Ferry.Phase.TO_SOURCE, "a linked drone starts for its source: " + drone.phase());
            // 6 blocks each way at 0.35 blocks a tick, 16 then 4 stones: two round trips take well under 200 ticks.
            helper.runAfterDelay(200, () -> {
                helper.assertTrue(count(level, b, Items.STONE) == 20, "stones delivered: " + count(level, b, Items.STONE));
                helper.assertTrue(count(level, a, Items.STONE) == 0, "stones left behind: " + count(level, a, Items.STONE));
                helper.assertTrue(drone.carried().isEmpty(), "the drone kept " + drone.carried());
                helper.assertTrue(drone.phase() == Ferry.Phase.WAITING || drone.phase() == Ferry.Phase.TO_SOURCE, "after the job: " + drone.phase());
                removeDrones(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aDroneWithoutBothLinksIdlesWhereItIs(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_100, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 4, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, pos);
            chest(level, pos).setItem(0, new ItemStack(Items.STONE, 5));
            Vec3 where = Vec3.atCenterOf(pos.east(3)).add(0, 2, 0);
            HelperDrone drone = HelperDrone.spawn(level, where, HelperLinks.EMPTY.withSource(pos), null);
            helper.assertTrue(drone.phase() == Ferry.Phase.IDLE, "half-linked drone: " + drone.phase());
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(drone.position().distanceTo(where) < 0.01, "an idle drone moved to " + drone.position());
                helper.assertTrue(count(level, pos, Items.STONE) == 5, "an idle drone took stones");
                removeDrones(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void sneakUseTakesTheDroneBackAsAnItemWithItsLinks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(90_200, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, pos, 4, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, pos);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            HelperLinks links = HelperLinks.EMPTY.withSource(pos.north(2)).withDestination(pos.south(2));
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.east(1)), links, player.getUUID());
            drone.setCarried(new ItemStack(Items.STONE, 3));
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            drone.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(drone.isRemoved(), "the drone stayed");
            ItemStack item = ItemStack.EMPTY;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.HELPER_DRONE.get())) {
                    item = stack;
                }
            }
            helper.assertTrue(!item.isEmpty(), "no drone item in the inventory");
            helper.assertTrue(links.equals(HelperDroneItem.links(item)), "links lost: " + item.get(ModDataComponents.HELPER_LINKS.get()));
            boolean dropped = !level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), e -> e.getItem().is(Items.STONE)).isEmpty();
            helper.assertTrue(dropped, "the carried stones were not dropped");
            removeDrones(level, pos);
            player.discard();
            helper.succeed();
        });
    }

    /** The launch complex takes payloads from a drone like any inventory: the fourth helper job, carrying to the launcher. */
    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aDroneCarriesPayloadsToTheLaunchController(GameTestHelper helper) {
        // Chunk-local x 8 facing south keeps the 15 x 15 pad in one chunk.
        BlockPos controllerPos = new BlockPos(90_312, Y, 0);
        NaniteGameTests.inEntityTickingArea(helper, controllerPos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            removeDrones(level, controllerPos);
            Direction facing = Direction.SOUTH;
            for (int across = -LaunchPad.HALF; across <= LaunchPad.HALF; across++) {
                for (int forward = 0; forward < LaunchPad.SIZE; forward++) {
                    level.setBlock(LaunchPad.cell(controllerPos, facing, across, forward), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            level.setBlock(controllerPos, ModBlocks.LAUNCH_CONTROLLER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 3);
            for (BlockPos cell : LaunchPad.ring(controllerPos, facing)) {
                level.setBlock(cell, ModBlocks.SCAFFOLD.get().defaultBlockState(), 3);
            }
            LaunchControllerBlockEntity controller = (LaunchControllerBlockEntity) level.getBlockEntity(controllerPos);
            helper.assertTrue(LaunchComplex.assemble(controller) == LaunchComplex.Result.ASSEMBLED, "complex not assembled");
            // The chest stays in the pad's (entity-ticking) chunk, just west of the ring: a drone in a chunk that does not
            // tick entities stops.
            BlockPos source = controllerPos.west(8).south(2);
            chest(level, source).setItem(0, new ItemStack(ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get(), 2));
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(controllerPos).add(0, 2, 0),
                    new HelperLinks(Optional.of(source), Optional.of(controllerPos)), null);
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(controller.inventory().getStackInSlot(0).getCount() + controller.inventory().getStackInSlot(1).getCount() == 2,
                        "payloads in the controller: " + controller.inventory().getStackInSlot(0) + " " + controller.inventory().getStackInSlot(1));
                helper.assertTrue(count(level, source, ModMaterials.DYSON_SWARM_SATELLITE_PAYLOAD.get()) == 0, "payloads left in the chest");
                drone.discard();
                controller.inventory().setStackInSlot(0, ItemStack.EMPTY);
                controller.inventory().setStackInSlot(1, ItemStack.EMPTY);
                LaunchComplex.disassemble(controller);
                removeDrones(level, controllerPos);
                helper.succeed();
            });
        });
    }
}
