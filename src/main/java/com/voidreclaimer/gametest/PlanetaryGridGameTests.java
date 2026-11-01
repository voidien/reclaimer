package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.EarthRestoration;
import com.voidreclaimer.earth.PlanetaryGridItem;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.registry.ModDataComponents;
import com.voidreclaimer.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Planetary Grid carries the planet's map to wherever it is held. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlanetaryGridGameTests {
    private PlanetaryGridGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theItemReadsThePlanetFromDeepSpace(GameTestHelper helper) {
        ServerLevel earth = SpireGameTests.earth(helper);
        SectorMap before = SectorMap.of(earth);
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 134_000.5, 100, 0.5);
        try {
            ItemStack grid = new ItemStack(ModItems.PLANETARY_GRID.get());
            helper.assertTrue(grid.has(ModDataComponents.PLANET_SECTORS.get()), "the grid is what the map layer looks for");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, grid);

            EarthRestoration.set(earth, new SectorMap(0b1010));
            player.tickCount = 40;
            player.getInventory().tick();
            ItemStack held = player.getMainHandItem();
            helper.assertTrue(PlanetaryGridItem.map(held).restored() == 0b1010, "the item did not take the map: " + PlanetaryGridItem.map(held));

            EarthRestoration.set(earth, new SectorMap(0b1011));
            player.tickCount = 41;
            player.getInventory().tick();
            helper.assertTrue(PlanetaryGridItem.map(held).restored() == 0b1010, "it should only look once a second");
            player.tickCount = 60;
            player.getInventory().tick();
            helper.assertTrue(PlanetaryGridItem.map(held).count() == 3, "the item did not follow the map: " + PlanetaryGridItem.map(held));
            helper.succeed();
        } finally {
            EarthRestoration.set(earth, before);
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aSavedGridKeepsItsMap(GameTestHelper helper) {
        ItemStack grid = new ItemStack(ModItems.PLANETARY_GRID.get());
        grid.set(ModDataComponents.PLANET_SECTORS.get(), 0b101);
        var saved = grid.save(helper.getLevel().registryAccess());
        ItemStack back = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) saved);
        helper.assertTrue(PlanetaryGridItem.map(back).count() == 2, "the map did not survive a save: " + back);
        helper.succeed();
    }
}
