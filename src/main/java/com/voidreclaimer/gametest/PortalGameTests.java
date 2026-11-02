package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.AtmosphericPortalBlock;
import com.voidreclaimer.earth.AtmosphericPortals;
import com.voidreclaimer.earth.PortalLinks;
import com.voidreclaimer.earth.PortalTravel;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 11 slice: building, lighting and breaking an Atmospheric Portal frame. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortalGameTests {
    private static final int WIDTH = 12;
    private static final int HEIGHT = 6;

    private PortalGameTests() {}

    /** A ring of frame blocks around a WIDTH x HEIGHT interior whose bottom-left block is {@code origin}, width along X. */
    private static void buildFrame(ServerLevel level, BlockPos origin) {
        for (int across = -1; across <= WIDTH; across++) {
            for (int up = -1; up <= HEIGHT; up++) {
                boolean interior = across >= 0 && across < WIDTH && up >= 0 && up < HEIGHT;
                boolean corner = (across == -1 || across == WIDTH) && (up == -1 || up == HEIGHT);
                if (!interior && !corner) {
                    level.setBlock(origin.east(across).above(up), ModBlocks.PORTAL_FRAME.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static void clear(ServerLevel level, BlockPos origin) {
        for (int across = -2; across <= WIDTH + 1; across++) {
            for (int up = -2; up <= HEIGHT + 1; up++) {
                level.setBlock(origin.east(across).above(up), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static int panes(ServerLevel level, BlockPos origin) {
        int count = 0;
        for (int across = 0; across < WIDTH; across++) {
            for (int up = 0; up < HEIGHT; up++) {
                if (level.getBlockState(origin.east(across).above(up)).is(ModBlocks.ATMOSPHERIC_PORTAL.get())) {
                    count++;
                }
            }
        }
        return count;
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aShipSizedFrameLightsAndBreaksTogether(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_000, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                helper.assertTrue(AtmosphericPortals.findFrame(space, origin.below()).isPresent(), "frame not found from its own block");
                helper.assertTrue(AtmosphericPortals.tryLight(space, origin.below()), "frame did not light");
                helper.assertTrue(panes(space, origin) == WIDTH * HEIGHT, "wrong pane count: " + panes(space, origin));
                helper.assertTrue(space.getBlockState(origin).getValue(AtmosphericPortalBlock.AXIS) == Direction.Axis.X, "wrong axis");

                // Breaking any frame block takes the whole portal with it.
                space.setBlock(origin.above(HEIGHT).east(4), Blocks.AIR.defaultBlockState(), 3);
                helper.assertTrue(panes(space, origin) == 0, "panes left after the frame broke: " + panes(space, origin));
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aBrokenFrameDoesNotLight(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_100, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                space.setBlock(origin.west().above(2), Blocks.AIR.defaultBlockState(), 3);
                helper.assertFalse(AtmosphericPortals.tryLight(space, origin.below()), "a frame with a gap lit");
                helper.assertTrue(panes(space, origin) == 0, "panes appeared");
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void theIgniterLightsTheFrameAndIsUsedUp(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(13_200, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            try {
                clear(space, origin);
                buildFrame(space, origin);
                ServerPlayer player = TestPlayers.join(helper);
                ItemStack igniter = new ItemStack(ModItems.PORTAL_IGNITER.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, igniter);
                BlockPos clicked = origin.below().east(3);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0, 0.5, 0), Direction.UP, clicked, false);
                // Survival players use the item up; creative ones keep it.
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                InteractionResult result = igniter.useOn(new UseOnContext(space, player, InteractionHand.MAIN_HAND, igniter, hit));
                helper.assertTrue(result.consumesAction(), "igniter did nothing: " + result);
                helper.assertTrue(panes(space, origin) == WIDTH * HEIGHT, "igniter did not fill the frame");
                helper.assertTrue(igniter.getCount() == 1, "igniter not used up: " + igniter.getCount());
                helper.succeed();
            } finally {
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            }
        });
    }

    /** Calls {@code each} once per tick until {@code done}, then {@code then}; fails the test when the budget runs out. */
    private static void until(GameTestHelper helper, String leg, ServerPlayer player, int budget, BooleanSupplier done, Runnable each, Runnable then) {
        if (done.getAsBoolean()) {
            then.run();
            return;
        }
        if (budget <= 0) {
            helper.fail("timed out waiting for the " + leg + ": player in " + player.level().dimension().location() + " at " + player.position()
                    + ", portalProcess=" + player.portalProcess + ", cooldown=" + player.getPortalCooldown() + ", alive=" + player.isAlive());
            return;
        }
        each.run();
        helper.runAfterDelay(1, () -> until(helper, leg, player, budget - 1, done, each, then));
    }

    /** Puts the player in the pane at {@code pos} each tick, as walking through it would. */
    private static void standIn(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        level.getBlockState(pos).entityInside(level, pos, player);
        player.doTick();
    }

    /** A Ship or Titan stays in orbit; a Shuttle fits. On Earth nothing is held back, so a body can't be stranded there. */
    @GameTest(template = "empty")
    public static void bigBodiesStayInOrbitAndAShuttleGoesDown(GameTestHelper helper) {
        var player = TestPlayers.survivalInDeepSpace(helper, 0.5, 80, 0.5);
        var pane = com.voidreclaimer.registry.ModBlocks.ATMOSPHERIC_PORTAL.get().defaultBlockState();
        var pos = player.blockPosition();
        var space = player.serverLevel();
        for (var key : java.util.List.of(com.voidreclaimer.body.BuiltinBodies.SHIP, com.voidreclaimer.body.BuiltinBodies.TITAN)) {
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(key));
            pane.entityInside(space, pos, player);
            helper.assertTrue(player.portalProcess == null, key.location().getPath() + " started through the portal");
        }
        helper.assertFalse(com.voidreclaimer.earth.PortalTravel.staysInOrbit(helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION), player), "a Titan is stuck on Earth");
        for (var key : java.util.List.of(com.voidreclaimer.body.BuiltinBodies.SHUTTLE, com.voidreclaimer.body.BuiltinBodies.MECH, com.voidreclaimer.body.BuiltinBodies.DRONE)) {
            com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(key));
            player.portalProcess = null;
            pane.entityInside(space, pos, player);
            helper.assertTrue(player.portalProcess != null, key.location().getPath() + " was held back");
        }
        player.portalProcess = null;
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aPlayerTravelsToTheReturnGateAndBack(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerLevel earth = helper.getLevel().getServer().getLevel(RuinedEarth.DIMENSION);
        BlockPos origin = new BlockPos(13_300, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            clear(space, origin);
            buildFrame(space, origin);
            helper.assertTrue(AtmosphericPortals.tryLight(space, origin.below()), "frame did not light");
            BlockPos pane = origin.east(WIDTH / 2).above(1);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, pane.getX() + 0.5, pane.getY(), pane.getZ() + 0.5);
            Runnable cleanup = () -> {
                TestPlayers.remove(player);
                clear(space, origin);
                space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                space.setChunkForced((origin.getX() + WIDTH) >> 4, origin.getZ() >> 4, false);
            };
            try {
                until(helper, "trip to Earth", player, 100, () -> player.level().dimension() == RuinedEarth.DIMENSION, () -> standIn(player, pane), () -> {
                    try {
                        PortalLinks.Gate gate = earth.getData(ModAttachments.PORTAL_LINKS).gate().orElseThrow();
                        helper.assertTrue(gate.width() >= PortalTravel.MIN_GATE_WIDTH && gate.height() >= PortalTravel.MIN_GATE_HEIGHT,
                                "return gate too small for the Ship: " + gate);
                        BlockPos gatePane = gate.rect().at(gate.width() / 2, 1);
                        helper.assertTrue(earth.getBlockState(gatePane).is(ModBlocks.ATMOSPHERIC_PORTAL.get()), "the return gate is not lit");
                        // A body comes out over the far side of the planet, above the debris belt, not at the gate.
                        helper.assertTrue(player.getY() >= com.voidreclaimer.Config.EARTH_BELT_FLOOR.get() && Math.abs(player.getX() - gate.rect().centerBottom().x) < 1
                                        && player.getZ() > com.voidreclaimer.earth.RuinedTerrain.HALF / 2,
                                "arrived in the wrong place: " + player.position());
                        helper.assertTrue(player.getAttributeValue(Attributes.GRAVITY) > 0, "no gravity on ruined Earth");
                        BlockPos back = gatePane;
                        // A real client acknowledges the dimension change; until then the server holds the player's ticking.
                        player.hasChangedDimension();
                        // Standing in a portal during its cooldown keeps the cooldown going, so wait it out where we landed.
                        until(helper, "portal cooldown", player, 100, () -> player.getPortalCooldown() == 0, player::doTick, () -> {
                            // The body came out on the far side of the planet, so nothing holds the gate's chunk any more.
                            earth.setChunkForced(back.getX() >> 4, back.getZ() >> 4, true);
                            player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
                            until(helper, "trip back", player, 100, () -> player.level().dimension() == ModDimensions.DEEP_SPACE, () -> standIn(player, back), () -> {
                                try {
                                    earth.setChunkForced(back.getX() >> 4, back.getZ() >> 4, false);
                                    helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(pane).add(0, -1, 0)) < 6,
                                            "came back far from the portal: " + player.position());
                                    helper.assertTrue(player.getAttributeValue(Attributes.GRAVITY) == 0.0, "gravity on in deep space");
                                    helper.succeed();
                                } finally {
                                    cleanup.run();
                                }
                            });
                        });
                    } catch (RuntimeException e) {
                        cleanup.run();
                        throw e;
                    }
                });
            } catch (RuntimeException e) {
                cleanup.run();
                throw e;
            }
        });
    }
}
