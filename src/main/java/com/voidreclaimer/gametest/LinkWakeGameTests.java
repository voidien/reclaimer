package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BuiltinBodies;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.BackupCores;
import com.voidreclaimer.link.BodyHost;
import com.voidreclaimer.link.HostRole;
import com.voidreclaimer.link.LinkState;
import com.voidreclaimer.link.Links;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Where a mind goes home to when the place it left is closed. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LinkWakeGameTests {
    private static final Vec3 AWAY = new Vec3(0.0, 0.0, 4.0);
    private static final ResourceKey<Level> NO_SUCH_LEVEL = ResourceKey.create(Registries.DIMENSION, VoidReclaimer.id("no_such_level"));

    private LinkWakeGameTests() {}

    /** Links {@code player} to a new shell, and closes the place the main body was left in. */
    private static UUID linkedWithHomeClosed(GameTestHelper helper, ServerPlayer player) {
        BodyHost shell = BodyHost.spawn(player.serverLevel(), player.position().add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
        UUID id = shell.getUUID();
        helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
        LinkState state = Links.state(player);
        LinkState.Link link = state.link().orElseThrow();
        LinkState.Place closed = new LinkState.Place(NO_SUCH_LEVEL, link.from().position(), 0f, 0f);
        player.setData(ModAttachments.LINK, state.withLink(new LinkState.Link(link.shell(), link.epoch(), link.home(), closed)));
        return id;
    }

    private static void finish(GameTestHelper helper, ServerPlayer player, UUID id) {
        ServerLevel shellLevel = helper.getLevel().getServer().getLevel(ModDimensions.DEEP_SPACE);
        if (shellLevel.getEntity(id) != null) {
            shellLevel.getEntity(id).discard();
        }
        player.setData(ModAttachments.LINK, LinkState.NONE);
        TestPlayers.remove(player);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aClosedHomeWakesYouAtABackupCoreForNothing(GameTestHelper helper) {
        BlockPos origin = new BlockPos(147_400, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 8.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel level = player.serverLevel();
            var state = ModBlocks.BACKUP_CORE.get().defaultBlockState();
            level.setBlock(origin, state, 3);
            state.getBlock().setPlacedBy(level, origin, state, player, new ItemStack(ModItems.BACKUP_CORE.get()));
            BackupCoreBlockEntity core = (BackupCoreBlockEntity) level.getBlockEntity(origin);
            core.setCharges(2);
            BackupCores.refresh(player);
            UUID id = linkedWithHomeClosed(helper, player);
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(player.position().distanceTo(Vec3.atCenterOf(origin)) < 3.0, "woke at " + player.position());
            helper.assertTrue(core.charges() == 2, "the wake spent a charge: " + core.charges());
            helper.assertFalse(Links.state(player).linked(), "still linked");
            level.removeBlock(origin, false);
            finish(helper, player, id);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aClosedHomeAndNoCoreWakesYouAtTheSpawn(GameTestHelper helper) {
        BlockPos origin = new BlockPos(147_600, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            UUID id = linkedWithHomeClosed(helper, player);
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(player.serverLevel().dimension() == ModDimensions.DEEP_SPACE, "woke in " + player.serverLevel().dimension());
            helper.assertTrue(player.position().distanceTo(SpawnHandler.spawnPosition()) < 1.0, "woke at " + player.position());
            finish(helper, player, id);
        });
    }

    /** A wake to where the body was left stays exactly that: the chain only starts when that is closed. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anOpenHomeIsWhereYouWake(GameTestHelper helper) {
        BlockPos origin = new BlockPos(147_800, 80, 0);
        NaniteGameTests.inEntityTickingArea(helper, origin, 1, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            Vec3 home = player.position();
            BodyHost shell = BodyHost.spawn(player.serverLevel(), home.add(AWAY), HostRole.SHELL, Bodies.preset(BuiltinBodies.DRONE), player.getUUID());
            UUID id = shell.getUUID();
            helper.assertTrue(Links.link(player, shell) == Links.Result.LINKED, "no link");
            helper.assertTrue(Links.wake(player) == Links.Result.WOKEN, "no wake");
            helper.assertTrue(player.position().distanceTo(home) < 0.01, "woke at " + player.position());
            finish(helper, player, id);
        });
    }
}
