package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.compat.cc.StationPeripheral;
import com.voidreclaimer.energy.EnergyTicker;
import com.voidreclaimer.helper.HelperDrone;
import com.voidreclaimer.helper.HelperDroneItem;
import com.voidreclaimer.helper.HelperLinks;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.station.StationCoreBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
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
 * Helper drone batteries. Each test sits in the middle of its own chunk, the only one sure to tick entities. None changes the
 * config: other drone tests share it. They set a drone's battery and compare it with a drone in the same place.
 */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HelperBatteryGameTests {
    private HelperBatteryGameTests() {}

    private static final int Y = 80, Z = 4_104;

    private static Container chest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        return (Container) level.getBlockEntity(pos);
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block) {
        BlockPos.betweenClosed(from, to).forEach(pos -> level.setBlock(pos, block.defaultBlockState(), 3));
    }

    private static void clear(ServerLevel level, BlockPos around) {
        level.getEntitiesOfClass(HelperDrone.class, new AABB(around).inflate(24)).forEach(HelperDrone::discard);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(24)).forEach(ItemEntity::discard);
        fill(level, around.offset(-7, -7, -7), around.offset(7, 7, 7), Blocks.AIR);
    }

    /** A sealed stone box, hollow two blocks to each side of {@code pos}. */
    private static void darkBox(ServerLevel level, BlockPos pos) {
        fill(level, pos.offset(-3, -3, -3), pos.offset(3, 3, 3), Blocks.STONE);
        fill(level, pos.offset(-2, -2, -2), pos.offset(2, 2, 2), Blocks.AIR);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aFlatDroneFliesSlowlyAndAFullOneDoesNot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_008, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            BlockPos a = pos.west(6), b = pos.east(6), c = pos.west(6).south(3), d = pos.east(6).south(3);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 1));
            chest(level, b);
            chest(level, c).setItem(0, new ItemStack(Items.STONE, 1));
            chest(level, d);
            Vec3 flatFrom = Vec3.atCenterOf(a.above()), fullFrom = Vec3.atCenterOf(c.above());
            HelperDrone flat = HelperDrone.spawn(level, flatFrom, new HelperLinks(Optional.of(a), Optional.of(b)), null);
            HelperDrone full = HelperDrone.spawn(level, fullFrom, new HelperLinks(Optional.of(c), Optional.of(d)), null);
            flat.setBattery(0f);
            helper.assertTrue(flat.power() == 0.25f, "a flat drone's power: " + flat.power());
            helper.assertTrue(full.power() == 1f, "a full drone's power: " + full.power());
            helper.runAfterDelay(20, () -> {
                double flatMoved = flat.position().distanceTo(flatFrom), fullMoved = full.position().distanceTo(fullFrom);
                helper.assertTrue(fullMoved > 5.5, "the full drone moved " + fullMoved);
                helper.assertTrue(flatMoved > 1.0 && flatMoved < 2.5, "the flat drone moved " + flatMoved + ", the full one " + fullMoved);
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void flightSpendsTheBatteryAndAnIdleDroneInTheSunFillsUp(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_104, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            helper.assertTrue(EnergyTicker.sunExposure(level, pos) > 0.5f, "no sun here: " + EnergyTicker.sunExposure(level, pos));
            BlockPos a = pos.west(6), b = pos.east(6);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 16));
            chest(level, b);
            HelperDrone flying = HelperDrone.spawn(level, Vec3.atCenterOf(a.above()), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            HelperDrone idle = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above(3)), HelperLinks.EMPTY, null);
            flying.setBattery(0.5f);
            idle.setBattery(0.5f);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(flying.battery() < 0.5f, "flying did not cost anything: " + flying.battery());
                helper.assertTrue(idle.battery() > 0.51f, "an idle drone in the sun: " + idle.battery());
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aDroneInTheDarkGainsNothingUntilAStationCoreIsNear(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_200, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            darkBox(level, pos);
            // The light engine works in its own time.
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(EnergyTicker.sunExposure(level, pos) == 0f, "the box is not dark: " + EnergyTicker.sunExposure(level, pos));
                HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above()), HelperLinks.EMPTY, null);
                drone.setBattery(0.5f);
                helper.runAfterDelay(50, () -> {
                    helper.assertTrue(drone.battery() == 0.5f, "a drone gained charge in the dark: " + drone.battery());
                    level.setBlock(pos, ModBlocks.STATION_CORE.get().defaultBlockState(), 3);
                    StationCoreBlockEntity core = (StationCoreBlockEntity) level.getBlockEntity(pos);
                    helper.assertTrue(new StationPeripheral().getDronesCharging(core) == 1, "the core does not see the drone");
                    // A second for the station list, a second for the drone's next look, and a second of charging.
                    helper.runAfterDelay(60, () -> {
                        helper.assertTrue(drone.battery() > 0.53f, "a drone at a core: " + drone.battery());
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        clear(level, pos);
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aFlatMinerDigsSlowly(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_296, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            level.setBlock(pos, ModBlocks.ASTEROID_SILICATE.get().defaultBlockState(), 3);
            BlockPos chestPos = pos.west(5);
            chest(level, chestPos);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above()),
                    new HelperLinks(Optional.empty(), Optional.of(chestPos), Optional.of(pos)), null);
            drone.setBattery(0f);
            // 40 dig ticks at full power, 160 flat: at 80 ticks a full battery would have long finished.
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(!level.getBlockState(pos).isAir(), "a flat miner dug at full speed");
                helper.startSequence()
                        .thenWaitUntil(() -> helper.assertTrue(level.getBlockState(pos).isAir(), "the flat miner never finished: " + drone.minePhase()))
                        .thenExecute(() -> clear(level, pos))
                        .thenSucceed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aDroneFromASaveWithNoBatteryLoadsFullAndWorks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_408, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            BlockPos a = pos.west(4), b = pos.east(4);
            chest(level, a).setItem(0, new ItemStack(Items.STONE, 5));
            chest(level, b);
            HelperDrone written = HelperDrone.spawn(level, Vec3.atCenterOf(a.above()), new HelperLinks(Optional.of(a), Optional.of(b)), null);
            CompoundTag tag = written.saveWithoutId(new CompoundTag());
            written.discard();
            helper.assertTrue(tag.contains("battery"), "the battery is not saved");
            // What this version's save looked like before: no battery, and the dig count as a whole number.
            tag.remove("battery");
            tag.remove("UUID");
            tag.putInt("dug", 7);
            HelperDrone old = new HelperDrone(ModEntities.HELPER_DRONE.get(), level);
            old.load(tag);
            level.addFreshEntity(old);
            helper.assertTrue(old.battery() == 1f, "an old drone's battery: " + old.battery());
            helper.assertTrue(old.saveWithoutId(new CompoundTag()).getFloat("dug") == 7f, "the old dig count is lost");
            helper.runAfterDelay(80, () -> {
                int delivered = ((Container) level.getBlockEntity(b)).countItem(Items.STONE);
                helper.assertTrue(delivered == 5, "an old drone delivered " + delivered);
                clear(level, pos);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void theItemKeepsTheBatteryAndAFullDroneKeepsPlainItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_504, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos.east(1)), HelperLinks.EMPTY, player.getUUID());
            helper.assertTrue(!drone.toItem().has(ModDataComponents.HELPER_BATTERY.get()), "a full drone's item carries a battery component");
            drone.setBattery(0.3f);
            ItemStack item = drone.toItem();
            drone.discard();
            helper.assertTrue(HelperDroneItem.battery(item) == 0.3f, "the item's battery: " + HelperDroneItem.battery(item));
            helper.assertTrue(HelperDroneItem.battery(new ItemStack(ModItems.HELPER_DRONE.get())) == 1f, "a new item is not full");
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            item.use(level, player, InteractionHand.MAIN_HAND);
            var released = level.getEntitiesOfClass(HelperDrone.class, new AABB(pos).inflate(8));
            helper.assertTrue(released.size() == 1, "released drones: " + released.size());
            helper.assertTrue(released.get(0).battery() == 0.3f, "the released drone's battery: " + released.get(0).battery());
            player.discard();
            clear(level, pos);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void anAddonCanChargeADroneThroughTheEvent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_600, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            darkBox(level, pos);
            helper.runAfterDelay(20, () -> {
                HelperDrone charged = HelperDrone.spawn(level, Vec3.atCenterOf(pos.above()), HelperLinks.EMPTY, null);
                HelperDrone plain = HelperDrone.spawn(level, Vec3.atCenterOf(pos.below()), HelperLinks.EMPTY, null);
                charged.addTag(TestEventListeners.CHARGED_DRONE);
                charged.setBattery(0f);
                plain.setBattery(0f);
                helper.runAfterDelay(50, () -> {
                    helper.assertTrue(charged.battery() > 0.5f, "the listener's charge: " + charged.battery());
                    helper.assertTrue(plain.battery() == 0f, "a drone without the tag gained " + plain.battery());
                    clear(level, pos);
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theStatusKeepsItsPhaseAndAddsTheBattery(GameTestHelper helper) {
        BlockPos pos = new BlockPos(92_696, Y, Z);
        NaniteGameTests.inEntityTickingArea(helper, pos, 7, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            clear(level, pos);
            HelperDrone drone = HelperDrone.spawn(level, Vec3.atCenterOf(pos), HelperLinks.EMPTY, null);
            drone.setBattery(0.42f);
            var status = drone.status();
            helper.assertTrue(((TranslatableContents) status.getContents()).getKey().endsWith(".status.idle"), "status: " + status.getContents());
            boolean battery = status.getSiblings().stream().anyMatch(part -> part.getContents() instanceof TranslatableContents key
                    && key.getKey().endsWith(".helper.battery") && "42".equals(String.valueOf(key.getArgs()[0])));
            helper.assertTrue(battery, "no battery in the status: " + status.getSiblings());
            clear(level, pos);
            helper.succeed();
        });
    }
}
