package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.debug.ItemCensus;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.BodySnapshot;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A destroyed shell leaves a wreck, and nothing it held is lost on the way. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkWreckGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);

    private LinkWreckGameTests() {}

    /** A player with an empty inventory beside a shell carrying a kit. */
    private static void beside(GameTestHelper helper, BlockPos origin, BiConsumer<ServerPlayer, BodyHost> test) {
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            LinkGameTests.kitOne(player, Bodies.preset(BuiltinBodies.DRONE));
            BodyHost shell = BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            BodySnapshot.take(player).applyTo(shell);
            Bodies.store(player, Bodies.preset(BuiltinBodies.MECH));
            player.getInventory().clearContent();
            test.accept(player, shell);
        });
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, BodyHost host) {
        if (!host.isRemoved()) {
            host.discard();
        }
        player.getInventory().clearContent();
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aDestroyedShellLeavesAWreckWithEverythingInIt(GameTestHelper helper) {
        beside(helper, new BlockPos(81_100, 80, 0), (player, shell) -> {
            ItemCensus carried = ItemCensus.of(BodySnapshot.of(shell), player.registryAccess());
            ItemCensus own = ItemCensus.of(player);
            helper.assertTrue(carried.total() > 0, "the shell carries nothing, so this checks nothing");
            shell.kill();
            helper.assertTrue(shell.role() == HostRole.WRECK, "the shell is a " + shell.role());
            helper.assertFalse(shell.isDeadOrDying(), "a wreck is lying dying");
            helper.assertTrue(Links.link(player, shell) == Links.Result.NOT_A_SHELL, "linked to a wreck");
            helper.runAfterDelay(30, () -> {
                helper.assertFalse(shell.isRemoved(), "the wreck went away by itself");
                shell.interact(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(ItemCensus.of(player).since(own).equals(carried.counts()),
                        "taken " + ItemCensus.of(player).since(own) + ", carried " + carried.counts());
                helper.assertTrue(shell.isRemoved(), "the empty wreck stayed");
                finish(helper, player, shell);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aWreckKeepsWhatDoesNotFit(GameTestHelper helper) {
        beside(helper, new BlockPos(81_300, 80, 0), (player, shell) -> {
            shell.kill();
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                inventory.items.set(slot, new ItemStack(Items.STICK, 64));
            }
            inventory.offhand.set(0, new ItemStack(Items.STICK, 64));
            int before = shell.items().size();
            shell.interact(player, InteractionHand.MAIN_HAND);
            helper.assertFalse(shell.isRemoved(), "a wreck with its contents left went away");
            helper.assertTrue(shell.items().size() == before, "items left the wreck with nowhere to go: " + shell.items().size() + " of " + before);
            finish(helper, player, shell);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aWreckCannotBeBrokenByAccident(GameTestHelper helper) {
        beside(helper, new BlockPos(81_500, 80, 0), (player, shell) -> {
            ServerLevel level = player.serverLevel();
            shell.kill();
            // After the moment a hit leaves anything immune to the next.
            helper.runAfterDelay(25, () -> {
                helper.assertFalse(shell.hurt(level.damageSources().generic(), 1_000f), "a wreck took damage");
                helper.assertFalse(shell.hurt(level.damageSources().drown(), 1_000f), "a wreck drowned");
                helper.assertTrue(shell.role() == HostRole.WRECK && !shell.isDeadOrDying(), "the wreck did not last");
                finish(helper, player, shell);
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aLivingShellIgnoresARightClick(GameTestHelper helper) {
        beside(helper, new BlockPos(81_700, 80, 0), (player, shell) -> {
            int before = shell.items().size();
            shell.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(shell.items().size() == before && !shell.isRemoved(), "a right-click took a working shell apart");
            finish(helper, player, shell);
        });
    }
}
