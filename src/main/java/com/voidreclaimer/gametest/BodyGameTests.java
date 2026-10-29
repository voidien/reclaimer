package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.InventoryLock;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.body.PartKind;
import com.voidreclaimer.body.SizeClass;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestone;
import com.voidreclaimer.replicator.Replicator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 5a: the modular body (stats, install rules, frame swap, inventory lock, modules, Expansion). */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyGameTests {
    private BodyGameTests() {}

    private static int put(ServerPlayer player, int slot, ItemStack stack) {
        player.getInventory().items.set(slot, stack);
        return slot;
    }

    @GameTest(template = "empty")
    public static void starterBodyIsAppliedOnJoin(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 61_000.5, 80, 0.5);
        try {
            Bodies.apply(player);
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - SizeClass.DRONE.scale()) < 1e-6, "not drone scale");
            helper.assertTrue(player.getMaxHealth() == 12f, "drone health " + player.getMaxHealth());
            helper.assertTrue(player.getData(ModAttachments.ENERGY).capacity() == 100f, "energy capacity");
            helper.assertTrue(Bodies.stats(player).inventorySlots() == 18, "starter inventory");
            FlightStats flight = FlightStats.of(player);
            helper.assertTrue(Math.abs(flight.acceleration() - 0.03) < 1e-6 && Math.abs(flight.maxSpeed() - 0.5) < 1e-6,
                    "starter handling changed: " + flight);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void installAndRemoveMoveItemsAndApplyStats(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 62_000.5, 80, 0.5);
        try {
            int slot = put(player, 3, new ItemStack(ModItems.STORAGE_MK1.get()));
            helper.assertTrue(Bodies.install(player, PartKind.STORAGE, 0, slot) == BodyRules.Check.OK, "install failed");
            helper.assertTrue(player.getInventory().items.get(slot).isEmpty(), "part not taken from the inventory");
            helper.assertTrue(Bodies.stats(player).inventorySlots() == 27, "storage didn't add a row");
            helper.assertTrue(DustData.capacity(player) > 200f, "storage didn't add dust capacity");

            put(player, 4, new ItemStack(ModItems.THRUSTER_MK1.get()));
            helper.assertTrue(Bodies.install(player, PartKind.STORAGE, 0, 4) == BodyRules.Check.WRONG_SLOT, "thruster went into storage");
            put(player, 5, new ItemStack(ModItems.THRUSTER_MK2.get()));
            helper.assertTrue(Bodies.install(player, PartKind.THRUSTER, 1, 5) == BodyRules.Check.TIER_TOO_HIGH, "Mk II on a Mk I core");

            helper.assertTrue(Bodies.remove(player, PartKind.STORAGE, 0) == BodyRules.Check.OK, "remove failed");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.STORAGE_MK1.get())), "removed part not returned");
            helper.assertTrue(Bodies.remove(player, PartKind.CORE, 0) == BodyRules.Check.REQUIRED, "core removed");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void biggerFrameTransfersPartsAndScales(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 63_000.5, 80, 0.5);
        try {
            TestPlayers.installModule(player, PartKind.SENSOR, 0, new ItemStack(ModItems.DUST_SENSOR.get()));
            put(player, 0, new ItemStack(ModItems.MECH_FRAME.get()));
            helper.assertTrue(Bodies.install(player, PartKind.FRAME, 0, 0) == BodyRules.Check.TIER_TOO_HIGH, "mech frame on a Mk I core");

            put(player, 1, new ItemStack(ModItems.CORE_MK2.get()));
            helper.assertTrue(Bodies.install(player, PartKind.CORE, 0, 1) == BodyRules.Check.OK, "core swap failed");
            helper.assertTrue(player.getInventory().items.get(1).is(ModItems.CORE_MK1.get()), "old core not returned to the slot");
            helper.assertTrue(player.getData(ModAttachments.ENERGY).capacity() == 250f, "Mk II capacity not applied");

            helper.assertTrue(Bodies.install(player, PartKind.FRAME, 0, 0) == BodyRules.Check.OK, "frame swap failed");
            BodyData body = Bodies.data(player);
            helper.assertTrue(Bodies.stats(player).size() == SizeClass.MECH, "not a mech");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - SizeClass.MECH.scale()) < 1e-6, "scale not applied");
            helper.assertTrue(player.getMaxHealth() == 20f, "mech health");
            helper.assertTrue(body.get(PartKind.THRUSTER, 0).is(ModItems.THRUSTER_MK1.get()), "thruster not transferred");
            helper.assertTrue(body.get(PartKind.SENSOR, 0).is(ModItems.DUST_SENSOR.get()), "sensor not transferred");
            helper.assertTrue(body.slots(PartKind.THRUSTER) == 4, "mech thruster slots");
            helper.assertTrue(player.getInventory().items.get(0).is(ModItems.DRONE_FRAME.get()), "old frame not returned");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void lockedSlotsTakeNothingNewButCanBeEmptied(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 64_000.5, 80, 0.5);
        try {
            Inventory inventory = player.getInventory();
            inventory.items.set(30, new ItemStack(Items.DIRT, 5)); // a legacy item in a now-locked row
            for (int i = 0; i < 18; i++) {
                inventory.items.set(i, new ItemStack(Items.STONE, 64));
            }
            helper.assertTrue(InventoryLock.isLocked(player, 18) && !InventoryLock.isLocked(player, 17), "lock boundary");
            helper.assertFalse(inventory.add(new ItemStack(Items.COBBLESTONE)), "added into a locked slot");
            helper.assertFalse(inventory.add(new ItemStack(Items.DIRT)), "merged into a locked slot");
            for (int i = 18; i < 36; i++) {
                if (i != 30) {
                    helper.assertTrue(inventory.items.get(i).isEmpty(), "locked slot " + i + " filled");
                }
            }
            Slot locked = player.inventoryMenu.slots.stream()
                    .filter(s -> s.container == inventory && s.getContainerSlot() == 30).findFirst().orElseThrow();
            helper.assertFalse(locked.mayPlace(new ItemStack(Items.STONE)), "menu slot accepts items");
            helper.assertTrue(locked.mayPickup(player), "legacy item trapped");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void naniteModuleBuildsWithAnEmptyHand(GameTestHelper helper) {
        BlockPos origin = new BlockPos(65_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 8, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            try {
                TestPlayers.installModule(player, PartKind.TOOL, 0, new ItemStack(ModItems.NANITE_SWARM.get()));
                put(player, 5, new ItemStack(ModItems.SCAFFOLD.get(), 4));
                BlockPos target = Nanites.target(player, Modules.naniteRange(player));

                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                Modules.use(player);
                helper.assertTrue(player.serverLevel().getBlockState(target).isAir(), "built with a busy hand");

                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                Modules.use(player);
                helper.assertTrue(player.serverLevel().getBlockState(target).is(ModBlocks.SCAFFOLD.get()), "module didn't build");
                helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.NANITE_SWARM.get()), "no cooldown");

                Modules.setNaniteRange(player, 6);
                helper.assertTrue(Modules.naniteRange(player) == 6, "range not stored on the module");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty")
    public static void fillingStorageReachesExpansion(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 66_000.5, 80, 0.5);
        try {
            float capacity = DustData.capacity(player);
            player.setData(ModAttachments.DUST, DustData.of(capacity - 1f, 0, 0, 0));
            Replicator.checkMilestones(player);
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestone.EXPANSION), "unlocked below capacity");
            player.setData(ModAttachments.DUST, DustData.of(capacity, 0, 0, 0));
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestone.EXPANSION), "not unlocked at capacity");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
