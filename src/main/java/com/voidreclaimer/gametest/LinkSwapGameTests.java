package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyInventoryMenu;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.debug.ItemCensus;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.BodySnapshot;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Remote control: the move between bodies. Uses ticking chunks, because a shell has to be found by id once it is back. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkSwapGameTests {
    /** Along z from a spot at z 0, so a shell stays in the chunk the test waited for: entity lookups see nothing in a chunk still waiting. */
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private LinkSwapGameTests() {}

    /** A player in a Mech and their Ship shell a few blocks off, each holding something of its own. */
    private static void withShell(GameTestHelper helper, BlockPos origin, BiConsumer<ServerPlayer, BodyHost> test) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel space = player.serverLevel();
            UUID id = null;
            try {
                LinkGameTests.kit(player, Bodies.preset(BuiltinBodies.SHIP, true), Items.EMERALD, 2, MobEffects.LUCK, origin);
                BodySnapshot inShell = BodySnapshot.take(player);
                BodyHost shell = BodyHost.spawn(space, player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
                id = shell.getUUID();
                inShell.applyTo(shell);
                LinkGameTests.kitOne(player, Bodies.preset(BuiltinBodies.MECH));
                test.accept(player, shell);
            } finally {
                // By id: a move leaves a new entity in the shell's place, and the world persists between runs.
                if (id != null && space.getEntity(id) != null) {
                    space.getEntity(id).discard();
                }
                TestPlayers.remove(helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()));
            }
        });
    }

    private static ItemCensus homeCensus(ServerPlayer player) {
        CompoundTag home = Links.state(player).link().orElseThrow().home();
        return ItemCensus.of(BodySnapshot.load(home, player.registryAccess()).orElseThrow(), player.registryAccess());
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aLinkAndAWakeSwapTheBodiesAndNothingIsLost(GameTestHelper helper) {
        withShell(helper, new BlockPos(77_000, 80, 0), (player, shell) -> {
            var registries = player.registryAccess();
            ServerLevel space = player.serverLevel();
            DustData dust = DustData.of(10f, 20f, 30f, 40f);
            player.setData(ModAttachments.DUST, dust);
            Vec3 from = player.position();
            Vec3 there = shell.position();
            UUID id = shell.getUUID();
            long epoch = shell.epoch();
            BodySnapshot home = BodySnapshot.take(player);
            BodySnapshot inShell = BodySnapshot.of(shell);
            ItemCensus everything = ItemCensus.of(home, registries).plus(ItemCensus.of(inShell, registries));

            var held = com.voidreclaimer.flight.FlightInput.of(true, false, false, false, false, false);
            player.setData(ModAttachments.FLIGHT_INPUT, held);
            player.getData(ModAttachments.FLIGHT_TRACKER).record(player.position(), 20);
            player.fallDistance = 7f;
            player.getData(ModAttachments.RECALL_STATE);
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            // The client only reports a change, so keys held through a move inside one dimension have to stay as they are.
            helper.assertTrue(player.getData(ModAttachments.FLIGHT_INPUT).equals(held), "the server lost keys the client will not send again");
            helper.assertTrue(player.getData(ModAttachments.FLIGHT_TRACKER).size() == 0, "the speed check still measures from the old place");
            helper.assertTrue(player.fallDistance == 0f, "a fall came along");
            helper.assertFalse(player.hasData(ModAttachments.RECALL_STATE), "a held recall came along");
            helper.assertTrue(shell.isRemoved(), "the shell is still in the world");
            helper.assertTrue(player.position().distanceTo(there) < 0.01, "not at the shell: " + player.position());
            helper.assertTrue(Bodies.data(player).equals(inShell.body()), "not wearing the shell's body");
            helper.assertTrue(player.getData(ModAttachments.DUST).equals(dust), "dust belongs to the mind, and it changed");
            helper.assertTrue(ItemCensus.of(player).plus(homeCensus(player)).equals(everything), "items changed in the link");
            helper.assertTrue(Links.state(player).link().orElseThrow().epoch() == epoch + 1, "the link's epoch");
            helper.assertTrue(Links.state(player).link().orElseThrow().from().position().distanceTo(from) < 0.01, "where the body was left");

            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(player.position().distanceTo(from) < 0.01, "not back home: " + player.position());
            helper.assertTrue(BodySnapshot.take(player).equals(home), "the main body changed");
            helper.assertTrue(player.getData(ModAttachments.DUST).equals(dust), "the dust changed on the way back");
            BodyHost back = space.getEntity(id) instanceof BodyHost found ? found : null;
            helper.assertTrue(back != null, "the shell did not come back with its id");
            helper.assertTrue(BodySnapshot.of(back).equals(inShell), "the shell's body changed");
            helper.assertTrue(back.role() == HostRole.SHELL && player.getUUID().equals(back.owner()), "not the player's shell");
            helper.assertTrue(back.epoch() == epoch + 1, "the shell's epoch " + back.epoch());
            helper.assertTrue(back.position().distanceTo(there) < 0.01, "the shell moved: " + back.position());
            helper.assertTrue(ItemCensus.of(player).plus(ItemCensus.of(BodySnapshot.of(back), registries)).equals(everything), "items changed in the wake");
            LinkState state = Links.state(player);
            helper.assertTrue(!state.linked() && state.outbox(id).isPresent(), "no outbox for the shell");
            helper.assertTrue(Links.link(player, back) == Links.Result.LINKED, "no second link");
            helper.assertTrue(Links.state(player).outbox(id).isEmpty(), "a stale outbox would rebuild the shell the player is in");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no second wake");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRefusedLinkChangesNothing(GameTestHelper helper) {
        withShell(helper, new BlockPos(77_300, 80, 0), (player, shell) -> {
            BodySnapshot before = BodySnapshot.take(player);
            Vec3 at = player.position();
            shell.setRole(HostRole.WRECK);
            helper.assertTrue(Links.link(player, shell) == Links.Result.NOT_A_SHELL, "linked to a wreck");
            shell.setRole(HostRole.SHELL);
            BodyHost someoneElses = BodyHost.spawn(player.serverLevel(), shell.position(), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), UUID.randomUUID());
            helper.assertTrue(Links.link(player, someoneElses) == Links.Result.NOT_YOURS, "linked to another player's shell");
            someoneElses.discard();
            helper.assertTrue(Links.link(player, someoneElses) == Links.Result.NOT_A_SHELL, "linked to a shell that is gone");
            helper.assertTrue(Links.wake(player) == Links.Result.NOT_LINKED, "woke with no link");
            helper.assertTrue(BodySnapshot.take(player).equals(before) && player.position().distanceTo(at) < 0.01, "a refusal changed the player");
            helper.assertFalse(Links.state(player).linked() || shell.isRemoved(), "a refusal changed the link");
            helper.succeed();
        });
    }

    /** One link at a time: the second would overwrite the record of the main body. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aSecondLinkIsRefusedAndTheMainBodyKept(GameTestHelper helper) {
        withShell(helper, new BlockPos(77_600, 80, 0), (player, shell) -> {
            BodyHost second = BodyHost.spawn(player.serverLevel(), shell.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            var link = Links.state(player).link();
            helper.assertTrue(Links.link(player, second) == Links.Result.ALREADY_LINKED, "linked twice");
            helper.assertTrue(Links.state(player).link().equals(link) && !second.isRemoved(), "the second link changed something");
            Links.wake(player);
            second.discard();
            helper.succeed();
        });
    }

    /** The teleport is the last thing that can refuse, so when it does nothing has changed. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aLockedDimensionStopsALinkBeforeAnythingChanges(GameTestHelper helper) {
        withShell(helper, new BlockPos(77_900, 80, 0), (player, shell) -> {
            BodyHost locked = BodyHost.spawn(helper.getLevel(), new Vec3(0.5, 100, 0.5), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            BodySnapshot before = BodySnapshot.take(player);
            ServerLevel level = player.serverLevel();
            helper.assertTrue(BodyInventoryMenu.open(player), "no body inventory");
            player.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 2));
            helper.assertTrue(Links.link(player, locked) == Links.Result.CANNOT_REACH, "linked into a locked dimension");
            helper.assertTrue(player.containerMenu instanceof BodyInventoryMenu, "a refusal closed the screen");
            helper.assertTrue(player.containerMenu.getCarried().is(Items.GOLD_INGOT), "a refusal took the cursor stack");
            player.containerMenu.setCarried(ItemStack.EMPTY);
            helper.assertTrue(player.serverLevel() == level, "the player left");
            helper.assertTrue(BodySnapshot.take(player).equals(before), "the body changed");
            helper.assertFalse(Links.state(player).linked() || locked.isRemoved(), "the link went ahead");
            locked.discard();
            helper.succeed();
        });
    }

    /**
     * A wake home can be refused too, for instance after a player linked from a dimension that is locked to them now. Asked again
     * and again it must not eat anything, above all the stack on the cursor with no room for it.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRefusedWakeChangesNothingHoweverOftenItIsAsked(GameTestHelper helper) {
        withShell(helper, new BlockPos(79_400, 80, 0), (player, shell) -> {
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            LinkState.Link link = Links.state(player).link().orElseThrow();
            LinkState.Place locked = new LinkState.Place(helper.getLevel().dimension(), new Vec3(0.5, 100, 0.5), 0f, 0f);
            player.setData(ModAttachments.LINK, LinkState.NONE.withLink(new LinkState.Link(link.shell(), link.epoch(), link.home(), locked)));
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            helper.assertTrue(BodyInventoryMenu.open(player), "no body inventory");
            player.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 2));
            ItemCensus before = ItemCensus.of(player);
            for (int asked = 0; asked < 3; asked++) {
                helper.assertTrue(Links.wake(player) == Links.Result.CANNOT_REACH, "woke into a locked dimension");
            }
            helper.assertTrue(ItemCensus.of(player).equals(before), "a refused wake cost items");
            helper.assertTrue(player.containerMenu instanceof BodyInventoryMenu, "a refused wake closed the screen");
            helper.assertTrue(player.containerMenu.getCarried().is(Items.GOLD_INGOT), "a refused wake took the cursor stack");
            helper.assertTrue(Links.state(player).linked(), "the link ended");
            player.containerMenu.setCarried(ItemStack.EMPTY);
            player.closeContainer();
            player.setData(ModAttachments.LINK, LinkState.NONE.withLink(link));
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no way home");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aLinkAndAWakeCrossDimensions(GameTestHelper helper) {
        withShell(helper, new BlockPos(79_700, 80, 0), (player, shell) -> {
            ServerLevel space = player.serverLevel();
            ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
            earth.getChunk(0, 0);
            Optional<BodyHost> onEarth = BodyHost.restore(earth, new LinkState.Place(earth.dimension(), new Vec3(8.5, 200, 8.5), 0f, 0f), UUID.randomUUID(), 0L,
                    BodySnapshot.of(shell), player.getUUID());
            helper.assertTrue(onEarth.isPresent(), "no shell on Earth");
            UUID id = onEarth.get().getUUID();
            ItemCensus everything = ItemCensus.of(BodySnapshot.take(player), player.registryAccess()).plus(ItemCensus.of(BodySnapshot.of(onEarth.get()), player.registryAccess()));
            helper.assertTrue(Links.link(player, onEarth.get()) == Links.Result.LINKED, "no link");
            helper.assertTrue(player.serverLevel() == earth, "the player is not on Earth");
            helper.assertTrue(Bodies.data(player).equals(BodySnapshot.of(shell).body()), "not wearing the shell's body on Earth");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(player.serverLevel() == space, "the player did not come home");
            BodyHost back = earth.getEntity(id) instanceof BodyHost found ? found : null;
            if (back != null) {
                helper.assertTrue(ItemCensus.of(player).plus(ItemCensus.of(BodySnapshot.of(back), player.registryAccess())).equals(everything), "items changed across dimensions");
                back.discard();
            } else {
                helper.assertTrue(Links.state(player).outbox(id).isPresent(), "the shell is neither back nor in the outbox");
            }
            helper.succeed();
        });
    }

    /** A request holds ids: a player who logged out before it ran must not take a shell's body with them. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRequestForAPlayerWhoLeftChangesNothing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(80_000, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            List<Links.Result> heard = new ArrayList<>();
            Links.requestLink(player, shell, heard::add);
            TestPlayers.remove(player);
            helper.runAfterDelay(2, () -> {
                boolean kept = !shell.isRemoved();
                shell.discard();
                helper.assertTrue(heard.equals(List.of(Links.Result.PLAYER_GONE)), "heard " + heard);
                helper.assertTrue(kept, "the shell went with a player who was gone");
                helper.succeed();
            });
        });
    }

    /** A stale object must be refused too, for whatever holds one: the file would never hear about the move. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aMoveRefusesAPlayerWhoIsGoneOrDead(GameTestHelper helper) {
        withShell(helper, new BlockPos(80_300, 80, 0), (player, shell) -> {
            player.setHealth(0f);
            Links.Result result = Links.link(player, shell);
            helper.assertTrue(result == Links.Result.PLAYER_GONE, "a corpse linked: " + result);
            helper.assertFalse(shell.isRemoved(), "a corpse took the shell");
            player.setHealth(10f);
            helper.succeed();
        });
    }

    /** A dying shell is not a body: its contents belong to the wreck, not to whoever gets there first. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aDyingShellCannotBeLinked(GameTestHelper helper) {
        withShell(helper, new BlockPos(80_600, 80, 0), (player, shell) -> {
            shell.setHealth(0f);
            helper.assertTrue(Links.link(player, shell) == Links.Result.NOT_A_SHELL, "linked to a dying shell");
            helper.succeed();
        });
    }

    /** An addon's listener throwing part way through a link: the move is undone and nothing is lost on either side. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aFailureDuringALinkIsUndone(GameTestHelper helper) {
        withShell(helper, new BlockPos(80_900, 80, 0), (player, shell) -> {
            var registries = player.registryAccess();
            BodySnapshot home = BodySnapshot.take(player);
            BodySnapshot inShell = BodySnapshot.of(shell);
            Vec3 at = player.position();
            player.addTag(TestEventListeners.THROW_ON_BODY_CHANGE);
            helper.assertTrue(Links.link(player, shell) == Links.Result.FAILED, "the failure was not reported");
            helper.assertFalse(Links.state(player).linked(), "still linked");
            helper.assertFalse(shell.isRemoved(), "the shell went");
            helper.assertTrue(player.position().distanceTo(at) < 0.01, "the player did not come back: " + player.position());
            helper.assertTrue(BodySnapshot.take(player).equals(home), "the main body changed");
            helper.assertTrue(BodySnapshot.of(shell).equals(inShell), "the shell changed");
            helper.succeed();
        });
    }

    /** The same on the way home: the player stays in the shell, and no second copy of it is left in the world. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aFailureDuringAWakeIsUndone(GameTestHelper helper) {
        withShell(helper, new BlockPos(81_200, 80, 0), (player, shell) -> {
            UUID id = shell.getUUID();
            ItemCensus everything = ItemCensus.of(BodySnapshot.take(player), player.registryAccess()).plus(ItemCensus.of(BodySnapshot.of(shell), player.registryAccess()));
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            Vec3 at = player.position();
            player.addTag(TestEventListeners.THROW_ON_BODY_CHANGE);
            helper.assertTrue(Links.wake(player) == Links.Result.FAILED, "the failure was not reported");
            helper.assertTrue(Links.state(player).linked(), "the link ended");
            helper.assertTrue(player.serverLevel().getEntity(id) == null, "a copy of the shell is in the world while the player wears it");
            helper.assertTrue(player.position().distanceTo(at) < 0.01, "the player did not stay: " + player.position());
            helper.assertTrue(ItemCensus.of(player).plus(homeCensus(player)).equals(everything), "items changed");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "the wake cannot be tried again");
            helper.succeed();
        });
    }

    /** A copy of the shell still in the world when it comes back gives way. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aStaleCopyOfAShellGivesWayToTheOneComingBack(GameTestHelper helper) {
        withShell(helper, new BlockPos(81_500, 80, 0), (player, shell) -> {
            ServerLevel space = player.serverLevel();
            UUID id = shell.getUUID();
            LinkState.Place at = new LinkState.Place(space.dimension(), shell.position(), 0f, 0f);
            Optional<BodyHost> fresh = BodyHost.restore(space, at, id, 9L, BodySnapshot.of(shell), player.getUUID());
            helper.assertTrue(fresh.isPresent(), "refused");
            helper.assertTrue(shell.isRemoved(), "the stale copy stayed");
            helper.assertTrue(space.getEntity(id) == fresh.get() && fresh.get().epoch() == 9L, "not the new copy");
            helper.succeed();
        });
    }

    /** An id the level will not take leaves the body with the caller, who keeps it in the outbox, instead of pretending. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShellTheLevelRefusesIsReported(GameTestHelper helper) {
        withShell(helper, new BlockPos(81_800, 80, 0), (player, shell) -> {
            ServerLevel space = player.serverLevel();
            ArmorStand taken = new ArmorStand(space, shell.getX(), shell.getY(), shell.getZ());
            UUID id = UUID.randomUUID();
            taken.setUUID(id);
            space.addFreshEntity(taken);
            LinkState.Place at = new LinkState.Place(space.dimension(), shell.position(), 0f, 0f);
            Optional<BodyHost> refused = BodyHost.restore(space, at, id, 1L, BodySnapshot.of(shell), player.getUUID());
            taken.discard();
            helper.assertTrue(refused.isEmpty(), "a refused shell was reported as restored");
            helper.succeed();
        });
    }

    /** A main body is never traded for a guess: the player stays where they are, and the tag stays in the player file. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void anUnreadableMainBodyLeavesThePlayerInTheShell(GameTestHelper helper) {
        withShell(helper, new BlockPos(78_200, 80, 0), (player, shell) -> {
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            LinkState.Link link = Links.state(player).link().orElseThrow();
            CompoundTag good = link.home();
            CompoundTag damaged = good.copy();
            damaged.remove("body");
            player.setData(ModAttachments.LINK, LinkState.NONE.withLink(new LinkState.Link(link.shell(), link.epoch(), damaged, link.from())));
            Vec3 at = player.position();
            helper.assertTrue(Links.wake(player) == Links.Result.HOME_UNREADABLE, "woke into a damaged body");
            helper.assertTrue(Links.state(player).link().orElseThrow().home().equals(damaged), "the tag was touched");
            helper.assertTrue(player.position().distanceTo(at) < 0.01, "the player moved");
            player.setData(ModAttachments.LINK, LinkState.NONE.withLink(link));
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "the good tag did not wake");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRelogWhileLinkedKeepsTheMainBody(GameTestHelper helper) {
        withShell(helper, new BlockPos(78_500, 80, 0), (player, shell) -> {
            BodySnapshot home = BodySnapshot.take(player);
            BodySnapshot inShell = BodySnapshot.of(shell);
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            ServerPlayer back = TestPlayers.rejoin(helper, player);
            helper.assertTrue(Links.state(back).linked(), "the link was lost in the relog");
            helper.assertTrue(Bodies.data(back).equals(inShell.body()), "not in the shell's body after the relog");
            helper.assertTrue(Links.wake(back) == Links.Result.WOKEN, "no wake after the relog");
            helper.assertTrue(BodySnapshot.take(back).equals(home), "the main body changed in the relog");
            helper.assertTrue(back.serverLevel().getEntity(shell.getUUID()) instanceof BodyHost restored && BodySnapshot.of(restored).equals(inShell), "the shell did not come back whole");
            helper.succeed();
        });
    }

    /**
     * The move runs between ticks, never inside anything else's tick. This test waits for those ticks, so it cannot use
     * {@link #withShell}, which cleans up as soon as its test returns.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theCommandsLinkAndWakeAtTheEndOfTheTick(GameTestHelper helper) {
        BlockPos origin = new BlockPos(78_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            String name = player.getScoreboardName();
            try {
                helper.assertTrue(dispatcher.execute("void_reclaimer link status " + name, source) == 0, "linked at the start");
                helper.assertTrue(dispatcher.execute("void_reclaimer link force " + name + " " + shell.getUUID(), source) == 1, "not asked for");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                helper.fail("the command did not parse: " + e.getMessage());
            }
            boolean ranInside = Links.state(player).linked();
            helper.runAfterDelay(2, () -> {
                boolean linked = Links.state(player).linked();
                try {
                    helper.assertTrue(dispatcher.execute("void_reclaimer link status " + name, source) == (linked ? 1 : 0), "status");
                    dispatcher.execute("void_reclaimer link wake " + name, source);
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                    helper.fail("the command did not parse: " + e.getMessage());
                }
                helper.runAfterDelay(2, () -> {
                    boolean stillLinked = Links.state(player).linked();
                    UUID id = shell.getUUID();
                    if (player.serverLevel().getEntity(id) != null) {
                        player.serverLevel().getEntity(id).discard();
                    }
                    TestPlayers.remove(player);
                    helper.assertFalse(ranInside, "the link ran inside the command");
                    helper.assertTrue(linked, "the link never ran");
                    helper.assertFalse(stillLinked, "the wake never ran");
                    helper.succeed();
                });
            });
        });
    }
}
