package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.registry.ModEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
            helper.assertTrue(shell.distanceTo(player) > player.getBbWidth(), "the shell is inside the player");
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
}
