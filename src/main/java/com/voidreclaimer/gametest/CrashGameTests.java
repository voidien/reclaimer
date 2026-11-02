package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyCrash;
import com.voidreclaimer.body.BoxCollision;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.flight.ThrusterFlight;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.registry.ModAttachments;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A body flying into something, as the server hears it. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrashGameTests {
    private CrashGameTests() {}

    private static void inTheOpen(GameTestHelper helper, BlockPos origin, ResourceKey<Chassis> chassis, Consumer<ServerPlayer> test) {
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(chassis));
            player.setHealth(player.getMaxHealth());
            TestPlayers.endSpawnProtection(player);
            player.setYRot(0f);
            player.setXRot(0f);
            test.accept(player);
            TestEventListeners.WRECKS.remove(player.getUUID());
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    private static double top(ServerPlayer player) {
        return FlightStats.of(player).maxSpeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aHeadOnHitAtTopSpeedDestroysABody(GameTestHelper helper) {
        inTheOpen(helper, new BlockPos(99_400, 300, 0), BuiltinBodies.SHIP, player -> {
            BodyCrash.heard(player, new Vec3(0.0, 0.0, top(player)));
            helper.assertTrue(player.isDeadOrDying(), "it survived with " + player.getHealth());
            UUID wreck = TestEventListeners.WRECKS.get(player.getUUID());
            helper.assertTrue(wreck != null, "it left no wreck");
            player.serverLevel().getEntity(wreck).discard();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void underTheSafeSpeedAHitDoesNothing(GameTestHelper helper) {
        inTheOpen(helper, new BlockPos(99_800, 300, 0), BuiltinBodies.SHIP, player -> {
            float health = player.getHealth();
            BodyCrash.heard(player, new Vec3(0.0, 0.0, 0.24));
            helper.assertTrue(player.getHealth() == health, "it lost health at 4.8 blocks a second");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aHitAtHalfSpeedHurtsAndDoesNotKill(GameTestHelper helper) {
        inTheOpen(helper, new BlockPos(100_200, 300, 0), BuiltinBodies.SHIP, player -> {
            float health = player.getHealth();
            BodyCrash.heard(player, new Vec3(0.0, 0.0, top(player) * 0.6));
            helper.assertTrue(player.getHealth() < health && player.isAlive(), "health went from " + health + " to " + player.getHealth());
        });
    }

    /** A client's word goes no further than the body could have been going. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aWildReportIsHeldToTheBodysSpeed(GameTestHelper helper) {
        inTheOpen(helper, new BlockPos(100_600, 300, 0), BuiltinBodies.SHIP, player -> {
            float health = player.getHealth();
            BodyCrash.heard(player, new Vec3(Double.NaN, 0.0, 1.0));
            helper.assertTrue(player.getHealth() == health, "a report of NaN hurt it");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anAddonCanCancelACrash(GameTestHelper helper) {
        inTheOpen(helper, new BlockPos(101_000, 300, 0), BuiltinBodies.SHIP, player -> {
            player.addTag(TestEventListeners.NO_CRASH);
            float health = player.getHealth();
            BodyCrash.heard(player, new Vec3(0.0, 0.0, top(player)));
            helper.assertTrue(player.getHealth() == health, "a cancelled crash hurt it");
        });
    }

    /** Flies the body along +Z until it touches, and hits at {@code speed} blocks a tick. */
    private static void hit(ServerPlayer player, double speed) {
        Vec3 ahead = new Vec3(0.0, 0.0, 0.5);
        for (int step = 0; step < 200 && BoxCollision.resolve(player, ahead).z >= 0.5; step++) {
            player.setPos(player.position().add(ahead));
        }
        BodyCrash.heard(player, new Vec3(0.0, 0.0, speed));
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aBigBodyBreaksTheWeakBlocksItHitsAndNotTheStrongOnes(GameTestHelper helper) {
        BlockPos origin = new BlockPos(101_400, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            BlockPos glass = origin.offset(0, 0, 20);
            for (int x = -6; x <= 6; x++) {
                for (int y = -6; y <= 12; y++) {
                    Block block = x == 0 && y == 2 ? Blocks.SPONGE : (x + y & 1) == 0 ? Blocks.GLASS : Blocks.OBSIDIAN;
                    space.setBlock(glass.offset(x, y, 0), block.defaultBlockState(), 3);
                }
            }
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.SHIP));
            player.setHealth(player.getMaxHealth());
            TestPlayers.endSpawnProtection(player);
            player.setYRot(0f);
            player.setXRot(0f);
            hit(player, top(player) * 0.7);
            int glassLeft = 0;
            int obsidianLeft = 0;
            for (int x = -6; x <= 6; x++) {
                for (int y = -6; y <= 12; y++) {
                    Block block = space.getBlockState(glass.offset(x, y, 0)).getBlock();
                    glassLeft += block == Blocks.GLASS ? 1 : 0;
                    obsidianLeft += block == Blocks.OBSIDIAN ? 1 : 0;
                }
            }
            int glassAtStart = 0;
            int obsidianAtStart = 0;
            for (int x = -6; x <= 6; x++) {
                for (int y = -6; y <= 12; y++) {
                    if (!(x == 0 && y == 2)) {
                        if ((x + y & 1) == 0) {
                            glassAtStart++;
                        } else {
                            obsidianAtStart++;
                        }
                    }
                }
            }
            helper.assertTrue(glassLeft < glassAtStart, "no glass broke");
            helper.assertTrue(obsidianLeft == obsidianAtStart, obsidianAtStart - obsidianLeft + " obsidian broke");
            helper.assertTrue(space.getBlockState(glass.offset(0, 2, 0)).is(Blocks.SPONGE), "a block in the crash_proof tag broke");
            BlockPos.betweenClosed(glass.offset(-6, -6, 0), glass.offset(6, 12, 0)).forEach(p -> space.setBlock(p, Blocks.AIR.defaultBlockState(), 3));
            space.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(glass).inflate(16)).forEach(net.minecraft.world.entity.Entity::discard);
            TestEventListeners.WRECKS.remove(player.getUUID());
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSmallBodyBreaksNothing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(101_800, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            BlockPos wall = origin.offset(0, 0, 2);
            space.setBlock(wall, Blocks.GLASS.defaultBlockState(), 3);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.DRONE));
            player.setHealth(player.getMaxHealth());
            TestPlayers.endSpawnProtection(player);
            BodyCrash.heard(player, new Vec3(0.0, 0.0, top(player) * 0.9));
            helper.assertTrue(space.getBlockState(wall).is(Blocks.GLASS), "a Drone broke a block");
            helper.assertTrue(player.getHealth() < player.getMaxHealth(), "the hit did not hurt it");
            space.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
            TestEventListeners.WRECKS.remove(player.getUUID());
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** What flight reports: the speed lost to the wall, whoever stopped the body, and nothing for a body that only slows. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void flightReportsTheSpeedAWallTook(GameTestHelper helper) {
        BlockPos origin = new BlockPos(102_200, 300, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 3, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            space.setBlock(origin.offset(0, 0, 2), Blocks.STONE.defaultBlockState(), 3);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.DRONE));
            AtomicReference<Vec3> heard = new AtomicReference<>();
            Consumer<Vec3> before = ThrusterFlight.crashed;
            ThrusterFlight.crashed = heard::set;
            try {
                player.setDeltaMovement(0.0, 0.0, 0.1);
                ThrusterFlight.travel(player);
                helper.assertTrue(heard.get() == null, "a free move was reported: " + heard.get());
                for (int tick = 0; tick < 12 && heard.get() == null; tick++) {
                    player.setDeltaMovement(0.0, 0.0, 1.0);
                    ThrusterFlight.travel(player);
                }
                helper.assertTrue(heard.get() != null, "the wall took the speed and nothing was said, at " + player.position());
                helper.assertTrue(heard.get().z > 0.3, "the report says " + heard.get());
            } finally {
                ThrusterFlight.crashed = before;
            }
            space.setBlock(origin.offset(0, 0, 2), Blocks.AIR.defaultBlockState(), 3);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }
}
