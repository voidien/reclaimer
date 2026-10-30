package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.BodyInventoryMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyInventoryGameTests {
    private BodyInventoryGameTests() {}

    @GameTest(template = "empty")
    public static void theBodyInventoryHasOnlyTheMainRowsAndTheHotbar(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 100_000.5, 80, 0.5);
        try {
            helper.assertTrue(BodyInventoryMenu.open(player), "the menu did not open for a survival player");
            helper.assertTrue(player.containerMenu instanceof BodyInventoryMenu, "wrong menu: " + player.containerMenu);
            helper.assertTrue(player.containerMenu.slots.size() == 36, "slots: " + player.containerMenu.slots.size() + " (no armor, offhand or crafting slots)");
            helper.assertTrue(player.containerMenu.stillValid(player), "menu not valid");
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            helper.assertFalse(BodyInventoryMenu.open(player), "the menu opened in creative");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void lockedRowsTakeNothingAndShiftClickFillsUnlockedOnes(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 101_000.5, 80, 0.5);
        try {
            BodyInventoryMenu.open(player);
            BodyInventoryMenu menu = (BodyInventoryMenu) player.containerMenu;
            // The starter Drone has one storage row: inventory slots 9..17 are open, 18..35 are locked.
            helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.STONE)), "an unlocked row refused an item");
            helper.assertFalse(menu.slots.get(26).mayPlace(new ItemStack(Items.STONE)), "a locked row took an item");
            helper.assertTrue(menu.isLocked(menu.slots.get(26)) && !menu.isLocked(menu.slots.get(0)), "locked flags wrong");
            player.getInventory().setItem(0, new ItemStack(Items.STONE, 5));
            ItemStack moved = menu.quickMoveStack(player, BodyInventoryMenu.MAIN_SLOTS);
            helper.assertTrue(moved.is(Items.STONE), "shift-click moved nothing");
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "the hotbar stack stayed");
            helper.assertTrue(player.getInventory().getItem(9).is(Items.STONE) && player.getInventory().getItem(9).getCount() == 5,
                    "the stack did not land in the first unlocked row: " + player.getInventory().getItem(9));
            for (int i = 18; i < 36; i++) {
                helper.assertTrue(player.getInventory().getItem(i).isEmpty(), "a locked slot received an item: " + i);
            }
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
