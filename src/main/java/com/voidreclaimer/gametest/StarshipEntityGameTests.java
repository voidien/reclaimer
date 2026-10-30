package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.starship.StarshipDrift;
import com.voidreclaimer.starship.TestStarship;
import com.voidreclaimer.starship.TestStarshipItem;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The placeholder Test Starship entity: drift and turn, right-click ignition, breaking, saving, the spawn command. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StarshipEntityGameTests {
    private StarshipEntityGameTests() {}

    /** Force-loads the chunks round {@code pos} (so the ship ticks) and removes ships left there by an earlier local run. */
    private static ServerLevel prepare(GameTestHelper helper, Vec3 pos) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        forceChunks(space, pos, true);
        ships(space, pos, 32).forEach(TestStarship::discard);
        return space;
    }

    /** The drift takes the ship up to StarshipDrift.RADIUS blocks into a neighbouring chunk, so force all nine. */
    private static void forceChunks(ServerLevel space, Vec3 pos, boolean forced) {
        int cx = (int) Math.floor(pos.x) >> 4;
        int cz = (int) Math.floor(pos.z) >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                space.setChunkForced(cx + dx, cz + dz, forced);
            }
        }
    }

    /** A sequence that waits until the force-loaded chunks tick entities; a fixed delay can come too early. */
    private static GameTestSequence whenTicking(GameTestHelper helper, ServerLevel space, Vec3 pos) {
        return helper.startSequence()
                .thenWaitUntil(() -> {
                    for (int dx = -16; dx <= 16; dx += 16) {
                        for (int dz = -16; dz <= 16; dz += 16) {
                            helper.assertTrue(space.isPositionEntityTicking(BlockPos.containing(pos.add(dx, 0, dz))), "chunks not ticking entities yet");
                        }
                    }
                })
                // The GameTest world persists: a ship an earlier run left here is only loaded once its chunk ticks, so
                // clear the area now, not in prepare() (where its entities are not loaded yet).
                .thenExecute(() -> ships(space, pos, 32).forEach(TestStarship::discard));
    }

    /** Ships anchored at {@code anchor}. A ship sits at its drift position, up to the drift radius away from its anchor. */
    private static List<TestStarship> anchoredAt(ServerLevel space, Vec3 anchor) {
        return ships(space, anchor, Math.hypot(StarshipDrift.RADIUS, StarshipDrift.BOB) + 1).stream()
                .filter(ship -> ship.anchor().distanceTo(anchor) < 1e-3).toList();
    }

    private static List<TestStarship> ships(ServerLevel space, Vec3 pos, double range) {
        return space.getEntities(ModEntities.TEST_STARSHIP.get(), new AABB(pos, pos).inflate(range), e -> true);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void starshipDriftsAndTurnsRoundItsAnchorWithoutFalling(GameTestHelper helper) {
        Vec3 anchor = new Vec3(93_000.5, 100, 0.5);
        ServerLevel space = prepare(helper, anchor);
        whenTicking(helper, space, anchor).thenExecute(() -> {
            TestStarship ship = TestStarship.spawn(space, anchor);
            Vec3 start = ship.position();
            float startYaw = ship.getYRot();
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(ship.isAlive(), "the ship is gone");
                helper.assertTrue(ship.position().distanceTo(start) > 0.2, "the ship did not drift");
                helper.assertTrue(ship.position().distanceTo(anchor) <= Math.hypot(StarshipDrift.RADIUS, StarshipDrift.BOB) + 1e-6,
                        "the ship drifted away from its anchor: " + ship.position());
                helper.assertTrue(Math.abs(ship.getYRot() - startYaw) > 5, "the ship did not turn");
                helper.assertTrue(ship.anchor().distanceTo(anchor) < 1e-3, "the anchor moved (gravity?)");

                CompoundTag saved = ship.saveWithoutId(new CompoundTag());
                TestStarship loaded = new TestStarship(ModEntities.TEST_STARSHIP.get(), space);
                loaded.load(saved);
                helper.assertTrue(loaded.anchor().distanceTo(anchor) < 1e-3, "the anchor was not saved");
                ship.discard();
                forceChunks(space, anchor, false);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void rightClickPlaysIgnitionOnceAndAHitBreaksTheShip(GameTestHelper helper) {
        Vec3 anchor = new Vec3(93_200.5, 100, 0.5);
        ServerLevel space = prepare(helper, anchor);
        AtomicReference<TestStarship> ship = new AtomicReference<>();
        AtomicReference<ServerPlayer> player = new AtomicReference<>();
        long[] clicked = {0};
        whenTicking(helper, space, anchor)
                .thenExecute(() -> {
                    ship.set(TestStarship.spawn(space, anchor));
                    player.set(TestPlayers.survivalInDeepSpace(helper, anchor.x + 8, anchor.y, anchor.z));
                    helper.assertFalse(ship.get().isIgniting(), "igniting before any click");
                    helper.assertTrue(ship.get().interact(player.get(), InteractionHand.MAIN_HAND).consumesAction(), "the click was not taken");
                    helper.assertTrue(ship.get().isIgniting(), "a right-click did not start ignite_prep");
                    clicked[0] = space.getGameTime();
                })
                .thenWaitUntil(() -> helper.assertFalse(ship.get().isIgniting(), "ignite_prep did not end; the ship should go back to gear_cycle"))
                .thenExecute(() -> {
                    try {
                        long ticks = space.getGameTime() - clicked[0];
                        helper.assertTrue(ticks >= TestStarship.IGNITE_TICKS - 1, "ignite_prep ended after " + ticks + " ticks");
                        ship.get().hurt(space.damageSources().playerAttack(player.get()), 1f);
                        helper.assertTrue(ship.get().isRemoved(), "one hit did not break the ship");
                        forceChunks(space, anchor, false);
                    } finally {
                        TestPlayers.remove(player.get());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void spawnCommandPutsOneShipAheadOfTheSource(GameTestHelper helper) {
        Vec3 from = new Vec3(93_400.5, 100, 0.5);
        Vec3 expected = from.add(0, 0, TestStarshipItem.AHEAD);  // yaw 0 looks south (+Z)
        ServerLevel space = prepare(helper, expected);
        whenTicking(helper, space, expected).thenExecute(() -> {
            CommandSourceStack source = space.getServer().createCommandSourceStack().withLevel(space).withPosition(from)
                    .withRotation(Vec2.ZERO).withPermission(4).withSuppressedOutput();
            space.getServer().getCommands().performPrefixedCommand(source, "void_reclaimer starship spawn");
            List<TestStarship> found = anchoredAt(space, expected);
            helper.assertTrue(found.size() == 1, "expected one ship anchored ahead, found " + found.size()
                    + " (ships nearby: " + ships(space, expected, 32).stream().map(TestStarship::anchor).toList() + ")");
            found.forEach(TestStarship::discard);
            forceChunks(space, expected, false);
            helper.succeed();
        });
    }
}
