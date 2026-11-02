package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The main body destroyed: it stays as a wreck with all it had, and the player goes on in a new starter body. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MainWreckGameTests {
    private MainWreckGameTests() {}

    private static void destroy(ServerPlayer player, ServerLevel level) {
        // Vanilla leaves a player invulnerable until the client says it has changed dimension, and a test player never says so.
        player.hasChangedDimension();
        player.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static int count(ListTag items, Item item, HolderLookup.Provider registries) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, items.getCompound(i));
            count += stack.is(item) ? stack.getCount() : 0;
        }
        return count;
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aDestroyedMainBodyLeavesAWreckWithAllItHad(GameTestHelper helper) {
        BlockPos origin = new BlockPos(97_600, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.SHIP));
            int sections = Bodies.data(player).sections().size();
            player.getInventory().setItem(3, new ItemStack(Items.IRON_INGOT, 5));
            player.getInventory().armor.set(0, new ItemStack(Items.IRON_BOOTS));
            player.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));
            destroy(player, space);
            UUID id = TestEventListeners.WRECKS.remove(player.getUUID());
            helper.assertTrue(id != null, "no event said a body was wrecked");
            helper.assertTrue(space.getEntity(id) instanceof BodyHost, "no wreck where the body died");
            BodyHost wreck = (BodyHost) space.getEntity(id);
            helper.assertTrue(wreck.role() == HostRole.WRECK, "it is a " + wreck.role());
            helper.assertTrue(player.getUUID().equals(wreck.owner()), "it has no owner");
            HolderLookup.Provider registries = player.registryAccess();
            helper.assertTrue(count(wreck.items(), Items.IRON_INGOT, registries) == 5, "the ingots are not in the wreck");
            helper.assertTrue(count(wreck.items(), Items.IRON_BOOTS, registries) == 1, "the boots are not in the wreck");
            helper.assertTrue(count(wreck.items(), Items.SHIELD, registries) == 1, "the shield is not in the wreck");
            helper.assertTrue(wreck.items().size() >= sections + 3, "the sections are not in the wreck: " + wreck.items().size() + " stacks");
            helper.assertTrue(player.getInventory().isEmpty(), "the player still has items");
            helper.assertTrue(Bodies.data(player).equals(Bodies.starter()), "the player is not in a new starter body");
            helper.assertTrue(player.isDeadOrDying(), "the player is not dead");
            LinkState state = Links.state(player);
            helper.assertTrue(state.outbox(id).isPresent(), "the body is not in the outbox");
            helper.assertTrue(state.known(id).filter(LinkState.KnownShell::wrecked).isPresent(), "the wreck is not on the list");
            helper.assertTrue(space.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(32.0)).isEmpty(), "items fell on the floor");
            wreck.discard();
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** The marker stays when the player arrives, follows the wreck as it drifts, and goes when the wreck has been emptied. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aWreckDriftsAndItsMarkerStaysUntilItIsEmptied(GameTestHelper helper) {
        BlockPos origin = new BlockPos(98_400, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            destroy(player, space);
            UUID id = TestEventListeners.WRECKS.remove(player.getUUID());
            BodyHost wreck = (BodyHost) space.getEntity(id);
            var start = wreck.position();
            ServerPlayer back = space.getServer().getPlayerList().respawn(player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            back.teleportTo(space, start.x, start.y, start.z, 0f, 0f);
            helper.runAfterDelay(45, () -> {
                back.tickCount += 20 - back.tickCount % 20;
                back.doTick();
                helper.assertTrue(wreck.position().distanceTo(start) > 0.3, "the wreck stayed at " + wreck.position());
                helper.assertTrue(wreck.position().distanceTo(start) < 3.0, "the wreck flew off to " + wreck.position());
                var marker = back.getData(ModAttachments.DEATH_MARKER);
                helper.assertTrue(marker.wreck().filter(id::equals).isPresent(), "arriving cleared the marker of a full wreck");
                while (!wreck.isRemoved()) {
                    back.getInventory().clearContent();
                    wreck.interact(back, net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                helper.assertTrue(back.getData(ModAttachments.DEATH_MARKER).pos().isEmpty(), "an emptied wreck kept its marker");
                back.setData(ModAttachments.LINK, LinkState.NONE);
                TestPlayers.remove(back);
    /** Ruined Earth's void kills whatever falls in, and the wreck of a body that fell must not go the same way, or everything is lost for good. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aBodyLostInTheVoidLeavesAWreckTheVoidCannotTake(GameTestHelper helper) {
        ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        BlockPos spot = new BlockPos(50_000, earth.getMinBuildHeight() - 80, 50_000);
        earth.setChunkForced(spot.getX() >> 4, spot.getZ() >> 4, true);
        helper.runAfterDelay(5, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 98_900.5, 300, 0.5);
            player.teleportTo(earth, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
            destroy(player, earth);
            UUID id = TestEventListeners.WRECKS.remove(player.getUUID());
            helper.assertTrue(id != null && earth.getEntity(id) instanceof BodyHost, "no wreck");
            BodyHost wreck = (BodyHost) earth.getEntity(id);
            helper.assertTrue(wreck.getY() >= earth.getMinBuildHeight(), "the wreck is at " + wreck.getY() + ", below the world");
            helper.runAfterDelay(60, () -> {
                helper.assertFalse(wreck.isRemoved(), "the void took the wreck");
                helper.assertTrue(wreck.role() == HostRole.WRECK, "it is a " + wreck.role());
                helper.assertTrue(count(wreck.items(), Items.DIAMOND, player.registryAccess()) == 7, "the diamonds went");
                wreck.discard();
                earth.setChunkForced(spot.getX() >> 4, spot.getZ() >> 4, false);
                player.setData(ModAttachments.LINK, LinkState.NONE);
                TestPlayers.remove(player);
                helper.succeed();
            });
        });
    }

    /** The death screen's shell card: the player respawns in a new body and is linked to the shell at once, so that body is there to come back to. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aPlayerWakesInTheShellTheyChose(GameTestHelper helper) {
        BlockPos origin = new BlockPos(98_800, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            BodyHost shell = BodyHost.spawn(space, player.position().add(0.0, 0.0, 6.0), HostRole.SHELL, Bodies.preset(BuiltinBodies.MECH), player.getUUID());
            UUID id = shell.getUUID();
            destroy(player, space);
            TestEventListeners.WRECKS.remove(player.getUUID());
            Links.chooseWake(player, java.util.Optional.of(UUID.randomUUID()));
            Links.chooseWake(player, java.util.Optional.of(id));
            ServerPlayer back = space.getServer().getPlayerList().respawn(player, false, net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            helper.startSequence()
                    .thenWaitUntil(() -> helper.assertTrue(Links.state(back).linked(), "not linked"))
                    .thenExecute(() -> {
                        helper.assertTrue(Links.state(back).link().filter(link -> link.shell().equals(id)).isPresent(), "linked to the wrong shell");
                        helper.assertTrue(Bodies.data(back).equals(Bodies.preset(BuiltinBodies.MECH)), "the player is not wearing the shell");
                        helper.assertTrue(Links.wake(back) == Links.Result.WOKEN, "no body to come back to");
                        helper.assertTrue(Bodies.data(back).equals(Bodies.starter()), "the body to come back to is not the new Drone");
                        back.setData(ModAttachments.LINK, LinkState.NONE);
                        TestPlayers.remove(back);
                    })
                    .thenSucceed();
    /** A crafting grid is carried too: a dead player's closing menu must not throw it on the floor. */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void whatIsInACraftingGridGoesInTheWreck(GameTestHelper helper) {
        BlockPos origin = new BlockPos(99_000, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, who) -> new net.minecraft.world.inventory.CraftingMenu(id, inventory, net.minecraft.world.inventory.ContainerLevelAccess.create(space, origin)),
                    net.minecraft.network.chat.Component.empty()));
            player.containerMenu.getSlot(1).set(new ItemStack(Items.DIAMOND, 3));
            destroy(player, space);
            UUID id = TestEventListeners.WRECKS.remove(player.getUUID());
            BodyHost wreck = (BodyHost) space.getEntity(id);
            helper.assertTrue(count(wreck.items(), Items.DIAMOND, player.registryAccess()) == 3, "the diamonds are not in the wreck");
            wreck.discard();
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** With the game rule that keeps inventories, nothing is left behind. Its own batch: the rule is the whole server's. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "keep_inventory")
    public static void keepingInventoriesLeavesNoWreck(GameTestHelper helper) {
        BlockPos origin = new BlockPos(98_000, 300, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerLevel space = TestPlayers.deepSpace(helper);
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Bodies.store(player, Bodies.preset(BuiltinBodies.SHIP));
            var body = Bodies.data(player);
            GameRules.BooleanValue rule = space.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
            rule.set(true, space.getServer());
            try {
                destroy(player, space);
            } finally {
                rule.set(false, space.getServer());
            }
            helper.assertTrue(TestEventListeners.WRECKS.remove(player.getUUID()) == null, "a wreck was left");
            helper.assertTrue(Bodies.data(player).equals(body), "the body was lost");
            TestPlayers.remove(player);
            helper.succeed();
        });
    }
}
