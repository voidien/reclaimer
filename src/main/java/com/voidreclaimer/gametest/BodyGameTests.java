package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.body.InventoryLock;
import com.voidreclaimer.body.Modules;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.flight.FlightStats;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestones;
import com.voidreclaimer.replicator.Replicator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The modular body: sections and modules, install rules, chassis swap, save migration, inventory lock, Expansion. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyGameTests {
    private static final BodyData.Mount HULL_STORAGE = new BodyData.Mount("hull", ModuleKind.STORAGE, 0);
    private static final BodyData.Mount HULL_CORE = new BodyData.Mount("hull", ModuleKind.CORE, 0);

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
            helper.assertTrue(Bodies.view(player).missing().isEmpty(), "starter drone has empty sockets");
            helper.assertTrue(Bodies.data(player).sections().size() == 7, "starter drone sections");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 0.4) < 1e-6, "not drone scale");
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
    public static void modulesInstallAndRemoveByMount(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 62_000.5, 80, 0.5);
        try {
            int slot = put(player, 3, new ItemStack(ModItems.STORAGE_MK1.get()));
            helper.assertTrue(Bodies.installModule(player, HULL_STORAGE, slot) == BodyRules.Check.OK, "install failed");
            helper.assertTrue(player.getInventory().items.get(slot).isEmpty(), "module not taken from the inventory");
            helper.assertTrue(Bodies.stats(player).inventorySlots() == 27, "storage didn't add a row");
            helper.assertTrue(DustData.capacity(player) > 200f, "storage didn't add dust capacity");

            put(player, 4, new ItemStack(ModItems.THRUSTER_MK1.get()));
            helper.assertTrue(Bodies.installModule(player, HULL_STORAGE, 4) == BodyRules.Check.WRONG_MOUNT, "thruster on a storage mount");
            helper.assertTrue(Bodies.installModule(player, new BodyData.Mount("hull", ModuleKind.THRUSTER, 0), 4)
                    == BodyRules.Check.NO_SUCH_MOUNT, "the hull has no thruster mount");
            put(player, 5, new ItemStack(ModItems.THRUSTER_MK2.get()));
            helper.assertTrue(Bodies.installModule(player, new BodyData.Mount("rotor_front_right", ModuleKind.THRUSTER, 0), 5)
                    == BodyRules.Check.TIER_TOO_HIGH, "Mk II on a Mk I core");

            helper.assertTrue(Bodies.removeModule(player, HULL_STORAGE) == BodyRules.Check.OK, "remove failed");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.STORAGE_MK1.get())), "removed module not returned");
            helper.assertTrue(Bodies.removeModule(player, HULL_CORE) == BodyRules.Check.REQUIRED, "core module removed");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void sectionsComeOffWithTheirModules(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 62_500.5, 80, 0.5);
        try {
            helper.assertTrue(Bodies.removeSection(player, "rotor_front_left") == BodyRules.Check.OK, "remove failed");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.ROTOR_ARM.get())), "section not returned");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.THRUSTER_MK1.get())), "its thruster not returned");
            helper.assertTrue(FlightStats.of(player).acceleration() == 0.0, "still thrusting without the arm");
            helper.assertTrue(Bodies.view(player).missing().size() == 1, "missing count");
            helper.assertTrue(Bodies.removeSection(player, "hull") == BodyRules.Check.REQUIRED, "core section removed");

            int claw = put(player, 8, new ItemStack(ModItems.TOOL_CLAW.get()));
            helper.assertTrue(Bodies.installSection(player, "rotor_front_left", claw) == BodyRules.Check.WRONG_SOCKET, "claw on a rotor socket");
            int arm = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.ROTOR_ARM.get()));
            helper.assertTrue(Bodies.installSection(player, "rotor_front_left", arm) == BodyRules.Check.OK, "arm not reinstalled");
            helper.assertTrue(Bodies.view(player).missing().isEmpty(), "socket still empty");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void mechTorsoSwitchesTheLayout(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 63_000.5, 80, 0.5);
        try {
            TestPlayers.installModule(player, ModuleKind.SENSOR, 0, new ItemStack(ModItems.DUST_SENSOR.get()));
            put(player, 0, new ItemStack(ModItems.MECH_TORSO.get()));
            helper.assertTrue(Bodies.installSection(player, "hull", 0) == BodyRules.Check.TIER_TOO_HIGH, "mech torso on a Mk I core");

            put(player, 1, new ItemStack(ModItems.CORE_MK2.get()));
            helper.assertTrue(Bodies.installModule(player, HULL_CORE, 1) == BodyRules.Check.OK, "core swap failed");
            helper.assertTrue(player.getInventory().items.get(1).is(ModItems.CORE_MK1.get()), "old core not returned to the slot");
            helper.assertTrue(player.getData(ModAttachments.ENERGY).capacity() == 250f, "Mk II capacity not applied");

            helper.assertTrue(Bodies.installSection(player, "hull", 0) == BodyRules.Check.OK, "torso swap failed");
            BodyData body = Bodies.data(player);
            helper.assertTrue(Bodies.stats(player).size().equals(BuiltinBodies.MECH_SIZE), "not a mech");
            helper.assertTrue(body.coreSocket().equals("torso"), "core socket " + body.coreSocket());
            helper.assertTrue(body.module(new BodyData.Mount("torso", ModuleKind.CORE, 0)).is(ModItems.CORE_MK2.get()), "core module lost");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SCALE) - 1.2) < 1e-6, "scale not applied");
            helper.assertTrue(body.sections().size() == 1, "drone sections stayed on a mech: " + body.sections().keySet());
            Inventory inventory = player.getInventory();
            helper.assertTrue(inventory.countItem(ModItems.ROTOR_ARM.get()) == 4, "rotor arms not returned");
            helper.assertTrue(inventory.contains(new ItemStack(ModItems.DRONE_HULL.get())), "hull not returned");
            helper.assertTrue(inventory.contains(new ItemStack(ModItems.DUST_SENSOR.get())), "sensor not returned with its dome");
            helper.assertTrue(inventory.contains(new ItemStack(ModItems.THRUSTER_MK1.get())), "thruster not returned with its arm");
            helper.assertTrue(Bodies.view(player).missing().size() == 15, "mech missing count");

            // The back socket takes a Drone rotor arm as a jetpack.
            int arm = inventory.findSlotMatchingItem(new ItemStack(ModItems.ROTOR_ARM.get()));
            helper.assertTrue(Bodies.installSection(player, "back", arm) == BodyRules.Check.OK, "jetpack refused");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A body saved before sections (a Mech frame and parts per kind) loads as a Mech with its parts on mounts. */
    @GameTest(template = "empty")
    public static void preSectionBodiesMigrate(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 63_500.5, 80, 0.5);
        try {
            CompoundTag old = new CompoundTag();
            old.put("frame", item("void_reclaimer:mech_frame"));
            CompoundTag parts = new CompoundTag();
            parts.put("core", list(item("void_reclaimer:core_mk2")));
            parts.put("thruster", list(item("void_reclaimer:thruster_mk1"), item("void_reclaimer:thruster_mk2"), item("void_reclaimer:thruster_mk1")));
            parts.put("tool", list(item("void_reclaimer:salvage_tool"), item("void_reclaimer:nanite_swarm"), item("void_reclaimer:salvage_tool")));
            old.put("parts", parts);
            old.putInt("active_tool", 1);
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
            BodyData body = BodyData.CODEC.parse(ops, old).getOrThrow();

            helper.assertTrue(body.coreSocket().equals("torso"), "not migrated to a Mech: " + body.coreSocket());
            helper.assertTrue(body.core().is(ModItems.MECH_TORSO.get()), "frame didn't become the torso");
            helper.assertTrue(body.sections().size() == 15, "basic Mech sections: " + body.sections().size());
            helper.assertTrue(body.module(new BodyData.Mount("torso", ModuleKind.CORE, 0)).is(ModItems.CORE_MK2.get()), "core not placed");
            helper.assertTrue(body.modules().keySet().stream().filter(m -> m.kind() == ModuleKind.THRUSTER).count() == 3, "thrusters not placed");
            // A basic Mech has two tool mounts (its hands): the third tool waits to be handed back.
            helper.assertTrue(body.pending().size() == 1 && body.pending().get(0).is(ModItems.SALVAGE_TOOL.get()), "leftovers: " + body.pending());

            player.setData(ModAttachments.BODY, body);
            player.doTick();
            helper.assertTrue(Bodies.data(player).pending().isEmpty(), "leftover not handed out");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.SALVAGE_TOOL.get())), "leftover not in the inventory");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static CompoundTag item(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("count", 1);
        return tag;
    }

    private static ListTag list(CompoundTag... items) {
        ListTag list = new ListTag();
        for (CompoundTag item : items) {
            list.add(item);
        }
        return list;
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
                TestPlayers.installModule(player, ModuleKind.TOOL, 0, new ItemStack(ModItems.NANITE_SWARM.get()));
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
            helper.assertFalse(Replicator.data(player).isUnlocked(Milestones.EXPANSION), "unlocked below capacity");
            player.setData(ModAttachments.DUST, DustData.of(capacity, 0, 0, 0));
            Replicator.checkMilestones(player);
            helper.assertTrue(Replicator.data(player).isUnlocked(Milestones.EXPANSION), "not unlocked at capacity");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** Taking off a storage module locks its own rows again, so room is counted without them. */
    @GameTest(template = "empty")
    public static void removingStorageIsRefusedWhenItsRowsWereTheOnlyRoom(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 81_000.5, 80, 0.5);
        try {
            Inventory inventory = player.getInventory();
            put(player, 0, new ItemStack(ModItems.STORAGE_MK1.get()));
            helper.assertTrue(Bodies.installModule(player, HULL_STORAGE, 0) == BodyRules.Check.OK, "storage not installed");
            helper.assertTrue(Bodies.stats(player).inventorySlots() > 18, "storage added no rows");
            for (int i = 0; i < 18; i++) {
                put(player, i, new ItemStack(Items.STONE, 64));
            }
            helper.assertTrue(Bodies.removeModule(player, HULL_STORAGE) != BodyRules.Check.OK, "removal accepted with no room left");
            helper.assertTrue(Bodies.data(player).module(HULL_STORAGE).is(ModItems.STORAGE_MK1.get()), "module lost on a refused removal");
            helper.assertTrue(player.serverLevel().getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(8)).isEmpty(),
                    "the module was dropped into space");

            put(player, 0, ItemStack.EMPTY);
            helper.assertTrue(Bodies.removeModule(player, HULL_STORAGE) == BodyRules.Check.OK, "removal refused with a free slot");
            helper.assertTrue(inventory.contains(new ItemStack(ModItems.STORAGE_MK1.get())), "module not returned");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A transient max health modifier is not saved, and vanilla clamps the saved health to the max it finds at load. */
    @GameTest(template = "empty")
    public static void bodyHealthModifierIsSavedWithThePlayer(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 82_000.5, 80, 0.5);
        try {
            Bodies.apply(player);
            CompoundTag saved = player.saveWithoutId(new CompoundTag());
            boolean found = false;
            for (Tag tag : saved.getList("attributes", Tag.TAG_COMPOUND)) {
                CompoundTag attribute = (CompoundTag) tag;
                if (attribute.getString("id").equals("minecraft:generic.max_health")) {
                    found = !attribute.getList("modifiers", Tag.TAG_COMPOUND).isEmpty();
                }
            }
            helper.assertTrue(found, "the body's max health is not in the saved attributes: " + saved.get("attributes"));
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** Thruster flight hides the sneak key from the server, so the client reports it with the tool use. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void naniteBuildsAgainstABlockOnTheReportedSneak(GameTestHelper helper) {
        BlockPos origin = new BlockPos(83_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 8, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            try {
                TestPlayers.installModule(player, ModuleKind.TOOL, 0, new ItemStack(ModItems.NANITE_SWARM.get()));
                put(player, 5, new ItemStack(ModItems.SCAFFOLD.get(), 4));
                player.setYRot(0f);
                player.setXRot(0f);
                BlockPos wall = BlockPos.containing(player.getEyePosition().add(0, 0, 2));
                player.serverLevel().setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                BlockPos built = wall.north();

                helper.assertFalse(player.isShiftKeyDown(), "the test player is sneaking");
                Modules.use(player, false);
                helper.assertTrue(player.serverLevel().getBlockState(built).isAir(), "built against a block without sneak");
                Modules.use(player, true);
                helper.assertTrue(player.serverLevel().getBlockState(built).is(ModBlocks.SCAFFOLD.get()), "no build on the reported sneak");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    /** The assembly view's hover preview has to say what really happens when the part goes in. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aPreviewMatchesWhatInstallingReallyDoes(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 94_500.5, 80, 0.5);
        try {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.voidreclaimer.registry.ModItems.STORAGE_MK1.get()));
            var view = Bodies.view(player);
            var mount = view.mounts().stream().filter(m -> m.kind() == com.voidreclaimer.api.body.ModuleKind.STORAGE && view.data().module(m).isEmpty()).findFirst().orElse(null);
            helper.assertTrue(mount != null, "the starter body has no free storage mount");
            var stack = player.getMainHandItem();
            var preview = Bodies.withModule(view, mount, stack, player.registryAccess());
            helper.assertTrue(view.data().module(mount).isEmpty(), "the preview changed the real body");
            helper.assertTrue(Bodies.installModule(player, mount, player.getInventory().selected) == com.voidreclaimer.body.BodyRules.Check.OK, "the install was refused");
            var real = Bodies.stats(player);
            helper.assertTrue(real.equals(preview.stats()), "preview " + preview.stats() + " but really " + real);
            helper.assertFalse(com.voidreclaimer.body.assembly.StatDelta.between(view.stats(), real).isEmpty(), "storage changed nothing, so this checks nothing");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }
}
