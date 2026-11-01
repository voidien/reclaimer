package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyInventoryMenu;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.debug.ItemCensus;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.energy.EnergyData;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.BodySnapshot;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModEntities;
import com.voidreclaimer.tether.TetherData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Remote control: the host that holds a body nobody is in. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkGameTests {
    private LinkGameTests() {}

    /** The host written to a save and read back, as a new entity in place of the old one. */
    private static BodyHost saveAndLoad(ServerLevel level, BodyHost host) {
        CompoundTag saved = new CompoundTag();
        host.saveAsPassenger(saved);
        host.discard();
        BodyHost restored = (BodyHost) EntityType.loadEntityRecursive(saved, level, entity -> entity);
        level.addFreshEntity(restored);
        return restored;
    }

    @GameTest(template = "empty")
    public static void aShellKeepsItsBodyHealthAndContentsThroughASave(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BodyData body = Bodies.preset(BuiltinBodies.SHIP, true);
        UUID owner = UUID.randomUUID();
        BodyHost shell = BodyHost.spawn(space, new Vec3(68_000.5, 80, 0.5), HostRole.SHELL, body, owner);
        CompoundTag diamonds = (CompoundTag) new ItemStack(Items.DIAMOND, 5).save(space.registryAccess(), new CompoundTag());
        diamonds.putByte("Slot", (byte) 3);
        ListTag items = new ListTag();
        items.add(diamonds);
        shell.setItems(items);
        shell.setEpoch(7L);
        shell.setHealth(9f);
        float maxHealth = shell.getMaxHealth();
        helper.assertTrue(maxHealth == Bodies.stats(shell).maxHealth() && maxHealth != 20f, "the body sets the health limit: " + maxHealth);

        BodyHost restored = saveAndLoad(space, shell);
        try {
            helper.assertTrue(restored.role() == HostRole.SHELL, "role " + restored.role());
            helper.assertTrue(Bodies.data(restored).equals(body), "the body changed in the save");
            helper.assertTrue(restored.getHealth() == 9f, "health " + restored.getHealth());
            helper.assertTrue(restored.getMaxHealth() == maxHealth, "max health " + restored.getMaxHealth());
            helper.assertTrue(Math.abs(restored.getScale() - Bodies.stats(restored).scale()) < 1.0e-3, "scale " + restored.getScale());
            helper.assertTrue(restored.items().equals(items), "contents " + restored.items());
            helper.assertTrue(restored.epoch() == 7L, "epoch " + restored.epoch());
            helper.assertTrue(owner.equals(restored.owner()), "owner " + restored.owner());
            helper.assertTrue(restored.isNoGravity(), "a shell floats");
            helper.succeed();
        } finally {
            restored.discard();
        }
    }

    /** The hit box is the body's: a Ship shell is a Ship's size in the world, with no custom hit box code. */
    @GameTest(template = "empty")
    public static void aShellIsTheSizeOfItsBody(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        for (var chassis : List.of(BuiltinBodies.DRONE, BuiltinBodies.MECH, BuiltinBodies.SHIP)) {
            BodyHost shell = BodyHost.spawn(space, new Vec3(68_500.5, 80, 0.5), HostRole.SHELL, Bodies.preset(chassis), null);
            double wanted = ModEntities.BODY_HOST.get().getHeight() * Bodies.stats(shell).scale();
            float height = shell.getBbHeight();
            shell.discard();
            helper.assertTrue(Math.abs(height - wanted) < 1.0e-3f, chassis.location() + " is " + height + " tall, not " + wanted);
        }
        helper.succeed();
    }

    /** A mirror is rebuilt by the seat, so a crash can never leave a stray copy of the main body in the world. */
    @GameTest(template = "empty")
    public static void onlyAMirrorIsLeftOutOfTheSave(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        for (HostRole role : HostRole.values()) {
            BodyHost host = BodyHost.spawn(space, new Vec3(69_000.5, 80, 0.5), role, Bodies.preset(BuiltinBodies.DRONE), null);
            boolean saved = host.shouldBeSaved();
            host.discard();
            helper.assertTrue(saved == (role != HostRole.MIRROR), role + " saved: " + saved);
        }
        helper.succeed();
    }

    /** A shell only crosses a portal with the player in it. */
    @GameTest(template = "empty")
    public static void aShellStaysInItsDimension(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        ServerLevel earth = helper.getLevel().getServer().getLevel(com.voidreclaimer.earth.RuinedEarth.DIMENSION);
        BodyHost shell = BodyHost.spawn(space, new Vec3(69_500.5, 80, 0.5), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), null);
        boolean portal = shell.canUsePortal(false);
        boolean moves = shell.canChangeDimensions(space, earth);
        shell.discard();
        helper.assertFalse(portal || moves, "a shell could leave: portal " + portal + ", dimensions " + moves);
        helper.succeed();
    }

    /** A body put on a host that is already in the world changes it at once: nothing else tells it to. */
    @GameTest(template = "empty")
    public static void aShellChangesSizeAndHealthWithItsBody(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BodyHost shell = BodyHost.spawn(space, new Vec3(70_500.5, 80, 0.5), HostRole.SHELL, Bodies.preset(BuiltinBodies.SHIP), null);
        try {
            float ship = shell.getBbHeight();
            shell.wear(Bodies.preset(BuiltinBodies.DRONE));
            helper.assertTrue(shell.getBbHeight() < ship, "the shell stayed Ship sized: " + shell.getBbHeight());
            helper.assertTrue(shell.getMaxHealth() == Bodies.stats(shell).maxHealth(), "max health " + shell.getMaxHealth());
            helper.assertTrue(shell.getHealth() <= shell.getMaxHealth(), "health above the Drone's limit: " + shell.getHealth());
            helper.succeed();
        } finally {
            shell.discard();
        }
    }

    @GameTest(template = "empty")
    public static void aDebugShellAppearsAheadOfThePlayerAndIsTheirs(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 71_000.5, 80, 0.5);
        BodyData body = Bodies.preset(BuiltinBodies.MECH, true);
        BodyHost shell = BodyHost.shellAhead(player, body);
        try {
            helper.assertTrue(shell.role() == HostRole.SHELL, "role " + shell.role());
            helper.assertTrue(player.getUUID().equals(shell.owner()), "owner " + shell.owner());
            helper.assertTrue(Bodies.data(shell).equals(body), "not the body it was given");
            helper.assertTrue(shell.distanceTo(player) > 4.0f * shell.getScale(), "too close for its size: " + shell.distanceTo(player));
            helper.succeed();
        } finally {
            shell.discard();
            TestPlayers.remove(player);
        }
    }

    /** Asks the command for its result, not a lookup: entity lookups see nothing in a chunk that does not tick entities. */
    @GameTest(template = "empty")
    public static void theSpawnCommandTakesTheBuiltInBodies(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 71_500.5, 80, 0.5);
        try {
            var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            int unknown = dispatcher.execute("void_reclaimer link spawn no_such_body", source);
            int known = dispatcher.execute("void_reclaimer link spawn drone", source);
            player.serverLevel().getEntitiesOfClass(BodyHost.class, player.getBoundingBox().inflate(32.0)).forEach(Entity::discard);
            helper.assertTrue(unknown == 0, "an unknown body made a shell");
            helper.assertTrue(known == 1, "the Drone made no shell");
            helper.succeed();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("the command did not parse: " + e.getMessage());
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static final float DAMAGE = 10f;
    /** A slot in a row the Drone has locked: a body that shrinks still holds what is there. */
    private static final int LOCKED_SLOT = 30;

    /**
     * The player with a body and something of everything a snapshot carries, different for each call so a part that fails to move
     * shows. The winch is left running: it must not carry over.
     */
    static void kit(ServerPlayer player, BodyData body, Item item, int slot, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, BlockPos anchor) {
        Bodies.store(player, body);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(slot, new ItemStack(item, 3));
        inventory.setItem(LOCKED_SLOT, new ItemStack(item, 2));
        inventory.armor.set(0, new ItemStack(Items.IRON_BOOTS));
        inventory.offhand.set(0, new ItemStack(item, 1));
        inventory.selected = slot;
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(effect, 6_000));
        player.setData(ModAttachments.TETHER, TetherData.NONE.withClip(GlobalPos.of(player.level().dimension(), anchor)).withRecalling(true));
        float capacity = Bodies.stats(player).energyCapacity();
        Energy.update(player, new EnergyData(capacity * 0.75f, capacity, false), true);
        player.setHealth(player.getMaxHealth() - DAMAGE);
    }

    static void kitOne(ServerPlayer player, BodyData body) {
        kit(player, body, Items.DIAMOND, 5, MobEffects.GLOWING, new BlockPos(1, 80, 2));
    }

    @GameTest(template = "empty")
    public static void aSnapshotComesBackFromASaveUnchanged(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 72_000.5, 80, 0.5);
        try {
            kitOne(player, Bodies.preset(BuiltinBodies.SHIP, true));
            BodySnapshot snapshot = BodySnapshot.take(player);
            helper.assertTrue(snapshot.effects().size() == 1 && snapshot.selected() == 5, "the kit is not in the snapshot: " + snapshot.effects());
            helper.assertTrue(snapshot.tether().anchor().isPresent(), "the tether is not in the snapshot");
            var loaded = BodySnapshot.load(snapshot.save(player.registryAccess()), player.registryAccess());
            helper.assertTrue(loaded.isPresent() && loaded.get().equals(snapshot), "the snapshot changed in the save");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A snapshot that cannot be read is no snapshot: a starter body in its place would throw away the real one for good. */
    @GameTest(template = "empty")
    public static void aDamagedSnapshotIsRefusedAndNotReplaced(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 72_250.5, 80, 0.5);
        try {
            var registries = player.registryAccess();
            kitOne(player, Bodies.preset(BuiltinBodies.MECH));
            CompoundTag whole = BodySnapshot.take(player).save(registries);
            helper.assertTrue(BodySnapshot.load(whole, registries).isPresent(), "a whole snapshot was refused");
            helper.assertTrue(BodySnapshot.load(new CompoundTag(), registries).isEmpty(), "an empty tag loaded");
            for (String part : List.of("body", "items")) {
                CompoundTag damaged = whole.copy();
                damaged.remove(part);
                helper.assertTrue(BodySnapshot.load(damaged, registries).isEmpty(), "loaded with no " + part);
            }
            CompoundTag corrupt = whole.copy();
            corrupt.putString("body", "not a body");
            helper.assertTrue(BodySnapshot.load(corrupt, registries).isEmpty(), "loaded with a corrupt body");
            CompoundTag noEnergy = whole.copy();
            noEnergy.remove("energy");
            helper.assertTrue(BodySnapshot.load(noEnergy, registries).isPresent(), "energy has a safe default, so it should load");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The body's limits have to be in place before its health and energy, or a big body is cut down to a small one's. */
    @GameTest(template = "empty")
    public static void aBigBodyKeepsItsHealthAndEnergyOnASmallPlayer(GameTestHelper helper) {
        swapBodies(helper, 72_500.5, BuiltinBodies.SHIP, BuiltinBodies.DRONE);
    }

    @GameTest(template = "empty")
    public static void aSmallBodyKeepsItsHealthAndEnergyOnABigPlayer(GameTestHelper helper) {
        swapBodies(helper, 73_000.5, BuiltinBodies.DRONE, BuiltinBodies.SHIP);
    }

    /** What a link will do: the two bodies change places, then change back. Nothing changes, and nothing appears or vanishes. */
    private static void swapBodies(GameTestHelper helper, double x, net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> shellBody,
                                   net.minecraft.resources.ResourceKey<com.voidreclaimer.api.body.Chassis> playerBody) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, x, 80, 0.5);
        var registries = player.registryAccess();
        BodyHost shell = null;
        try {
            kit(player, Bodies.preset(shellBody, true), Items.EMERALD, 2, MobEffects.LUCK, new BlockPos(3, 80, 4));
            BodySnapshot inShell = BodySnapshot.take(player);
            shell = BodyHost.spawn(player.serverLevel(), new Vec3(x, 80, 8.5), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            inShell.applyTo(shell);
            kitOne(player, Bodies.preset(playerBody, true));
            BodySnapshot home = BodySnapshot.take(player);
            helper.assertFalse(home.tether().recalling() || inShell.tether().recalling(), "a running winch went into a snapshot");
            ItemCensus everything = ItemCensus.of(home, registries).plus(ItemCensus.of(inShell, registries));

            inShell.applyTo(player);
            home.applyTo(shell);
            helper.assertTrue(BodySnapshot.take(player).equals(inShell), "the player is not the shell's body: " + player.getHealth() + " of " + player.getMaxHealth());
            helper.assertTrue(BodySnapshot.of(shell).equals(home), "the shell is not the player's body: " + shell.getHealth() + " of " + shell.getMaxHealth());
            helper.assertTrue(player.getActiveEffects().size() == 1, "the old body's effect stayed on: " + player.getActiveEffects());
            helper.assertTrue(ItemCensus.of(player).plus(ItemCensus.of(BodySnapshot.of(shell), registries)).equals(everything), "items appeared or vanished");

            BodySnapshot back = BodySnapshot.of(shell);
            inShell = BodySnapshot.take(player);
            back.applyTo(player);
            inShell.applyTo(shell);
            helper.assertTrue(BodySnapshot.take(player).equals(home), "the player did not get their own body back");
            helper.assertTrue(player.getInventory().selected == 5, "selected slot " + player.getInventory().selected);
            helper.succeed();
        } finally {
            if (shell != null) {
                shell.discard();
            }
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void aStackOnTheCursorComesAlong(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 73_500.5, 80, 0.5);
        try {
            helper.assertTrue(BodyInventoryMenu.open(player), "no body inventory: survival play has this screen open");
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
            player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 4));
            ItemCensus before = ItemCensus.of(player);
            BodySnapshot snapshot = BodySnapshot.take(player);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "the screen stayed open");
            helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "the cursor still holds a stack");
            helper.assertTrue(ItemCensus.of(snapshot, player.registryAccess()).equals(before), "before " + before);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** With no room, closing a screen drops the stack, and at a seat nobody may come back for it. The body takes it along instead. */
    @GameTest(template = "empty")
    public static void aStackWithNoRoomStaysWithTheBody(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 73_750.5, 80, 0.5);
        try {
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                inventory.setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            helper.assertTrue(BodyInventoryMenu.open(player), "no body inventory");
            player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 4));
            ItemCensus before = ItemCensus.of(player);
            BodySnapshot snapshot = BodySnapshot.take(player);
            helper.assertTrue(snapshot.body().pending().size() == 1, "not in the body's pending items: " + snapshot.body().pending());
            helper.assertTrue(Bodies.data(player).pending().size() == 1, "kept only in the snapshot, so a refused move would lose it");
            helper.assertTrue(ItemCensus.of(snapshot, player.registryAccess()).equals(before), "before " + before);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** The cursor stack of an open screen would ride into the new body, and the old inventory would be overwritten under it. */
    @GameTest(template = "empty")
    public static void aBodyWillNotGoOnOverAnOpenScreen(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 73_900.5, 80, 0.5);
        try {
            kitOne(player, Bodies.preset(BuiltinBodies.MECH));
            BodySnapshot snapshot = BodySnapshot.take(player);
            player.getInventory().setItem(1, new ItemStack(Items.EMERALD, 4));
            helper.assertTrue(BodyInventoryMenu.open(player), "no body inventory");
            boolean refused = false;
            try {
                snapshot.applyTo(player);
            } catch (IllegalStateException expected) {
                refused = true;
            }
            helper.assertTrue(refused, "a body went on over an open screen");
            helper.assertTrue(player.getInventory().getItem(1).is(Items.EMERALD), "the refused body still changed the inventory");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void aShellKeepsItsEffectsAndSelectedSlotThroughASave(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_000.5, 80, 0.5);
        ServerLevel space = player.serverLevel();
        BodyHost[] shell = {null};
        try {
            kitOne(player, Bodies.preset(BuiltinBodies.MECH));
            BodySnapshot snapshot = BodySnapshot.take(player);
            shell[0] = BodyHost.spawn(space, new Vec3(74_000.5, 80, 8.5), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), null);
            snapshot.applyTo(shell[0]);
            shell[0] = saveAndLoad(space, shell[0]);
            helper.assertTrue(BodySnapshot.of(shell[0]).equals(snapshot), "the shell's body changed in the save");
            helper.succeed();
        } finally {
            if (shell[0] != null) {
                shell[0].discard();
            }
            TestPlayers.remove(player);
        }
    }

    /** One item that can no longer be read costs that item, not the rest: this is why items stay in the game's own per-slot form. */
    @GameTest(template = "empty")
    public static void aBadItemCostsThatItemAndNotTheRest(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_250.5, 80, 0.5);
        try {
            var registries = player.registryAccess();
            player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 3));
            player.getInventory().setItem(4, new ItemStack(Items.EMERALD, 2));
            BodySnapshot snapshot = BodySnapshot.take(player);
            ListTag items = snapshot.items();
            items.getCompound(0).putString("id", "void_reclaimer:removed_from_the_game");
            CompoundTag tag = snapshot.save(registries);
            tag.put("items", items);
            BodySnapshot.load(tag, registries).orElseThrow().applyTo(player);
            helper.assertTrue(player.getInventory().getItem(3).isEmpty(), "the unreadable item is there");
            helper.assertTrue(player.getInventory().getItem(4).is(Items.EMERALD), "the good item went with it");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** A datapack may change a layout while a body lies in storage. What no longer fits comes back, as it does at login. */
    @GameTest(template = "empty")
    public static void aBodyFromStorageIsFittedToItsLayout(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_350.5, 80, 0.5);
        try {
            var registries = player.registryAccess();
            kitOne(player, Bodies.preset(BuiltinBodies.MECH));
            BodySnapshot whole = BodySnapshot.take(player);
            ItemStack orphan = new ItemStack(com.voidreclaimer.registry.ModItems.MECH_TORSO.get());
            BodySnapshot stale = new BodySnapshot(whole.body().withSection("socket_removed_by_a_datapack", orphan), whole.energy(), whole.tether(),
                    whole.items(), whole.selected(), whole.health(), whole.effects());
            ItemCensus before = ItemCensus.of(stale, registries);
            stale.applyTo(player);
            helper.assertTrue(Bodies.data(player).section("socket_removed_by_a_datapack").isEmpty(), "the orphan section stayed on");
            helper.assertTrue(ItemCensus.of(player).equals(before), "the orphan section vanished: " + ItemCensus.of(player));
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void aSnapshotKeepsLowPowerAndABadSlotIsTheFirst(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_400.5, 80, 0.5);
        try {
            float capacity = Bodies.stats(player).energyCapacity();
            Energy.update(player, new EnergyData(0f, capacity, true), true);
            BodySnapshot flat = BodySnapshot.take(player);
            Energy.update(player, new EnergyData(capacity, capacity, false), true);
            new BodySnapshot(flat.body(), flat.energy(), flat.tether(), flat.items(), 40, flat.health(), flat.effects()).applyTo(player);
            helper.assertTrue(player.getData(ModAttachments.ENERGY).lowPower(), "low power did not come along");
            helper.assertTrue(player.getInventory().selected == 0, "selected slot " + player.getInventory().selected);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    /** {@link TestPlayers#rejoin} goes through the real player file. A clean logout: it says nothing about a crash. */
    @GameTest(template = "empty")
    public static void aRejoinedPlayerIsReadFromTheirSave(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 74_500.5, 80, 0.5);
        kitOne(player, Bodies.preset(BuiltinBodies.MECH));
        BodySnapshot before = BodySnapshot.take(player);
        ServerPlayer back = TestPlayers.rejoin(helper, player);
        try {
            helper.assertTrue(back != player, "the same object came back");
            helper.assertTrue(BodySnapshot.take(back).equals(before), "the body changed in a relog");
            helper.succeed();
        } finally {
            TestPlayers.remove(back);
        }
    }

    /** The command's result is the total, so a test can read it: the cursor and the body's own parts count. */
    @GameTest(template = "empty")
    public static void theCensusCountsCursorAndBodyParts(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 75_500.5, 80, 0.5);
        try {
            var source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            int parts = ItemCensus.of(player).total();
            player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 3));
            player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 4));
            int counted = dispatcher.execute("void_reclaimer link census " + player.getScoreboardName(), source);
            helper.assertTrue(counted == parts + 7, "counted " + counted + ", with " + parts + " body parts and 7 items");
            helper.assertTrue(ItemCensus.of(player).since(new ItemCensus(java.util.Map.of())).size() >= 2, "no kinds counted");
            helper.succeed();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("the command did not parse: " + e.getMessage());
        } finally {
            TestPlayers.remove(player);
        }
    }
}
