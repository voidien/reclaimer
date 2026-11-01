package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.machine.ConfigChoiceCondition;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Remote Shell items, which put remote control within reach in survival. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RemoteShellGameTests {
    private RemoteShellGameTests() {}

    private static List<BodyHost> shells(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(BodyHost.class, new AABB(player.blockPosition()).inflate(32.0));
    }

    private static void use(ServerPlayer player, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        item.use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aRemoteShellUnfoldsIntoItsBody(GameTestHelper helper) {
        BlockPos origin = new BlockPos(149_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 2, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            use(player, ModItems.MECH_SHELL.get());
            List<BodyHost> found = shells(player);
            helper.assertTrue(found.size() == 1, "found " + found.size() + " shells");
            BodyHost shell = found.get(0);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the item was not used up");
            helper.assertTrue(shell.role() == HostRole.SHELL && player.getUUID().equals(shell.owner()), "not the player's shell");
            Object worn = Bodies.chassis(level.registryAccess(), Bodies.data(shell));
            Object mech = Bodies.chassis(level.registryAccess(), Bodies.preset(BuiltinBodies.MECH));
            helper.assertTrue(worn.equals(mech), "it wears " + worn);
            helper.assertTrue(Links.state(player).known(shell.getUUID()).isPresent(), "not on the player's list");
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "it cannot be linked to");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            shells(player).forEach(Entity::discard);
            player.setData(ModAttachments.LINK, LinkState.NONE);
            TestPlayers.remove(player);
            helper.succeed();
        });
    }

    /** One tick: the recipes are what make remote control reachable in survival. */
    @GameTest(template = "empty")
    public static void theSeatAndTheShellsCanBeMade(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String name : List.of("uplink_seat", "drone_shell_parts", "mech_shell_parts", "ship_shell_parts", "shuttle_shell_parts")) {
            helper.assertTrue(recipes.byKey(VoidReclaimer.id("replicator/" + name)).isPresent(), "no recipe for " + name);
        }
        helper.succeed();
    }

    /** Its parts cost more than any body holds, so it is paid in real sections and a fee a Titan can hold. */
    @GameTest(template = "empty")
    public static void theTitanShellTakesTheCoreSectionsAndAFeeABodyCanHold(GameTestHelper helper) {
        var recipe = helper.getLevel().getServer().getRecipeManager().byKey(VoidReclaimer.id("replicator/titan_shell_parts"));
        helper.assertTrue(recipe.isPresent(), "no recipe for the Titan shell");
        var value = (com.voidreclaimer.replicator.ReplicatorRecipe) recipe.get().value();
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 0.5, 80, 0.5);
        player.getInventory().clearContent();
        com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.preset(com.voidreclaimer.body.BuiltinBodies.TITAN));
        for (var mount : List.copyOf(com.voidreclaimer.body.Bodies.data(player).modules().keySet())) {
            if (mount.kind() == com.voidreclaimer.api.body.ModuleKind.STORAGE) {
                com.voidreclaimer.body.Bodies.store(player, com.voidreclaimer.body.Bodies.data(player).withModule(mount, new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.STORAGE_MK2.get())));
            }
        }
        float capacity = com.voidreclaimer.dust.DustData.capacity(player);
        var dust = com.voidreclaimer.dust.DustData.of(0, 0, 0, 0);
        for (var cost : value.cost().amounts().entrySet()) {
            helper.assertTrue(cost.getValue() <= capacity, "a Titan can't hold the fee: " + cost);
            dust = dust.with(cost.getKey(), cost.getValue());
        }
        player.setData(com.voidreclaimer.registry.ModAttachments.DUST, dust);
        com.voidreclaimer.replicator.Replicator.store(player, com.voidreclaimer.replicator.Replicator.data(player).withUnlocked(value.milestone()));
        var id = VoidReclaimer.id("replicator/titan_shell_parts");
        helper.assertTrue(com.voidreclaimer.replicator.Replicator.queue(player, id) == com.voidreclaimer.replicator.ReplicatorLogic.QueueResult.NOT_ENOUGH_ITEMS, "queued with no sections");
        player.getInventory().add(new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.TITAN_SAUCER.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.TITAN_NECK.get()));
        player.getInventory().add(new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.TITAN_BRIDGE.get()));
        helper.assertTrue(com.voidreclaimer.replicator.Replicator.queue(player, id) == com.voidreclaimer.replicator.ReplicatorLogic.QueueResult.QUEUED, "not queued with the sections and the fee");
        helper.assertTrue(player.getInventory().isEmpty(), "the sections stayed");
        TestPlayers.remove(player);
        helper.succeed();
    }

    /** The extension point: one setting of a config choice keeps one set of recipes. */
    @GameTest(template = "empty")
    public static void theConfigKeepsOneSetOfShellPrices(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String setting : List.of("cheap", "reduced", "premium")) {
            helper.assertTrue(recipes.byKey(VoidReclaimer.id("replicator/ship_shell_" + setting)).isEmpty(), "a second price for the ship shell: " + setting);
            helper.assertTrue(recipes.byKey(VoidReclaimer.id("replicator/shuttle_shell_" + setting)).isEmpty(), "a second price for the shuttle shell: " + setting);
            helper.assertTrue(recipes.byKey(VoidReclaimer.id("replicator/titan_shell_" + setting)).isEmpty(), "a second price for the titan shell: " + setting);
        }
        helper.assertTrue(new ConfigChoiceCondition("shellPrices", "parts").test(null), "the default did not pass");
        helper.assertFalse(new ConfigChoiceCondition("shellPrices", "cheap").test(null), "another setting passed");
        helper.assertFalse(new ConfigChoiceCondition("noSuchChoice", "parts").test(null), "an unknown choice passed");
        helper.succeed();
    }
}
