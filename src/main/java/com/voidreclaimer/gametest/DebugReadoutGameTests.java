package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.debug.DebugReadout;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Photo mode's debug facts, on real blocks and entities in deep space. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DebugReadoutGameTests {
    private DebugReadoutGameTests() {}

    private static final Vec3 ALONG_X = new Vec3(1, 0, 0);

    private static void clear(ServerLevel level, BlockPos from, int length) {
        for (int i = 0; i <= length; i++) {
            level.setBlockAndUpdate(from.offset(i, 0, 0), Blocks.AIR.defaultBlockState());
        }
        level.getEntitiesOfClass(Entity.class, new AABB(from).inflate(length + 2.0), e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .forEach(Entity::discard);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theReadoutNamesTheFirstBlockAlongTheViewWithItsContents(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_000, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 12, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            clear(space, origin, 10);
            BlockPos chestPos = origin.offset(5, 0, 0);
            space.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) space.getBlockEntity(chestPos)).setItem(0, new ItemStack(Items.STONE, 3));
            space.setBlockAndUpdate(origin.offset(8, 0, 0), Blocks.STONE.defaultBlockState());

            Vec3 eye = Vec3.atCenterOf(origin);
            DebugReadout.Target target = DebugReadout.target(space, eye, ALONG_X);
            helper.assertTrue(chestPos.equals(target.pos()), "the first block along the view, not the one behind it: " + target.pos());
            List<String> lines = DebugReadout.lines(space, eye, ALONG_X, null);
            helper.assertTrue(lines.get(0).startsWith("Block minecraft:chest at "), lines.get(0));
            helper.assertTrue(lines.contains("  items 1 of 27 slots used"), "the inventory: " + lines);
            helper.assertTrue(lines.stream().anyMatch(line -> line.startsWith("  data ") && line.contains("minecraft:stone")),
                    "the saved data, read from the server's block entity: " + lines);
            helper.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Place void_reclaimer:deep_space")), "the place: " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anEntityInFrontOfABlockIsWhatTheReadoutSees(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_100, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 12, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            clear(space, origin, 10);
            space.setBlockAndUpdate(origin.offset(8, 0, 0), Blocks.STONE.defaultBlockState());
            Entity stand = EntityType.ARMOR_STAND.create(space);
            stand.setNoGravity(true);
            stand.setPos(origin.getX() + 4.5, origin.getY() - 0.4, origin.getZ() + 0.5);
            space.addFreshEntity(stand);
            Vec3 eye = Vec3.atCenterOf(origin);
            DebugReadout.Target target = DebugReadout.target(space, eye, ALONG_X);
            helper.assertTrue(target.entity() == stand, "the armour stand is nearer than the stone: " + target);
            List<String> lines = DebugReadout.lines(space, eye, ALONG_X, null);
            helper.assertTrue(lines.get(0).startsWith("Entity minecraft:armor_stand"), lines.get(0));
            stand.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void emptySpaceSaysSoAndStillDescribesThePlace(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_200, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 12, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            Vec3 eye = Vec3.atCenterOf(origin);
            DebugReadout.Target target = DebugReadout.target(space, eye, new Vec3(0, 1, 0));
            helper.assertTrue(target.pos() == null && target.entity() == null, "nothing above: " + target);
            List<String> lines = DebugReadout.lines(space, eye, new Vec3(0, 1, 0), null);
            helper.assertTrue(lines.get(0).startsWith("Nothing within "), lines.get(0));
            helper.assertTrue(lines.size() > 2, "the place's lines still come: " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theLogisticsOverlayShowsADronesJobRouteAndHeading(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_300, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 12, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            clear(space, origin, 10);
            BlockPos from = origin.offset(2, 0, 0);
            BlockPos to = origin.offset(8, 0, 0);
            space.setBlockAndUpdate(from, Blocks.CHEST.defaultBlockState());
            space.setBlockAndUpdate(to, Blocks.CHEST.defaultBlockState());
            var drone = com.voidreclaimer.helper.HelperDrone.spawn(space, Vec3.atCenterOf(origin.above(3)),
                    new com.voidreclaimer.helper.HelperLinks(java.util.Optional.of(from), java.util.Optional.of(to)), null);
            helper.runAfterDelay(3, () -> {
                var shapes = new com.voidreclaimer.debug.DebugShapes();
                var facts = new java.util.ArrayList<String>();
                com.voidreclaimer.debug.LogisticsOverlay.add(space, Vec3.atCenterOf(origin), 32.0, shapes, facts);
                helper.assertTrue(facts.stream().anyMatch(fact -> fact.startsWith("Drone ferrying, ")), "the job: " + facts);
                helper.assertTrue(shapes.lines().stream().anyMatch(line -> line.colour() == com.voidreclaimer.debug.LogisticsOverlay.ROUTE
                        && line.from().equals(Vec3.atCenterOf(from)) && line.to().equals(Vec3.atCenterOf(to))), "the route between its chests");
                helper.assertTrue(shapes.lines().stream().anyMatch(line -> line.colour() == com.voidreclaimer.debug.LogisticsOverlay.HEADING), "where it is heading");
                drone.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theLogisticsOverlayListsAStoppedMachine(GameTestHelper helper) {
        BlockPos origin = new BlockPos(93_400, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 4, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlockAndUpdate(origin, com.simibubi.create.AllBlocks.SHAFT.get().defaultBlockState());
            var shapes = new com.voidreclaimer.debug.DebugShapes();
            var facts = new java.util.ArrayList<String>();
            com.voidreclaimer.debug.LogisticsOverlay.add(space, Vec3.atCenterOf(origin), 32.0, shapes, facts);
            helper.assertTrue(facts.contains("Machine at " + origin.toShortString() + ": 0 rpm"), "the stopped shaft: " + facts);
            helper.assertTrue(shapes.texts().stream().anyMatch(text -> text.colour() == com.voidreclaimer.debug.LogisticsOverlay.STOPPED), "no label");
            helper.succeed();
        });
    }
}
