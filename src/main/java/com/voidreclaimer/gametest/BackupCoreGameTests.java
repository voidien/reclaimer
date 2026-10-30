package com.voidreclaimer.gametest;

import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.BackupCoreList;
import com.voidreclaimer.core.BackupCores;
import com.voidreclaimer.core.CoreCharging;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustFieldSeed;
import com.voidreclaimer.dust.StarterNebula;
import com.voidreclaimer.nanite.Nanites;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.space.SpawnHandler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 4b: Backup Cores (ownership, limit, charging, respawn), the death marker, and the starter nebula lock. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BackupCoreGameTests {
    private BackupCoreGameTests() {}

    /** Places a core with the Nanite Swarm (offhand core) at {@code pos}; returns the placement result. */
    private static Nanites.Result place(ServerPlayer player, BlockPos pos) {
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.BACKUP_CORE.get()));
        return Nanites.buildAt(player, pos, Direction.UP);
    }

    private static BackupCoreBlockEntity core(ServerLevel level, BlockPos pos) {
        return (BackupCoreBlockEntity) level.getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void placingRegistersCoresUpToTheLimitAndSunlightCharges(GameTestHelper helper) {
        BlockPos origin = new BlockPos(57_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 6, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel space = player.serverLevel();
            try {
                for (int i = 0; i < BackupCoreList.MAX_CORES; i++) {
                    helper.assertTrue(place(player, origin.offset(2 + i * 2, 0, 0)) == Nanites.Result.BUILT, "core " + i + " not placed");
                }
                BackupCoreList list = BackupCores.list(player);
                helper.assertTrue(list.cores().size() == BackupCoreList.MAX_CORES, "cores not registered: " + list);
                BackupCoreBlockEntity first = core(space, origin.offset(2, 0, 0));
                helper.assertTrue(first.isOwnedBy(player.getUUID()), "owner not set");
                helper.assertTrue(first.name().equals("Core 1"), "default name: " + first.name());
                helper.assertTrue(place(player, origin.offset(0, 0, 3)) == Nanites.Result.BLOCKED, "placed past the limit");
                helper.assertTrue(space.getBlockState(origin.offset(0, 0, 3)).isAir(), "fourth core exists");

                // Salvaging (breaking) one frees a slot.
                space.destroyBlock(origin.offset(6, 0, 0), false);
                helper.assertTrue(BackupCores.list(player).cores().size() == BackupCoreList.MAX_CORES - 1, "removed core still listed");

                // The core charges when its update tick finds sky light, which a fresh chunk only has once the light engine has caught
                // up: wait for the charge instead of a fixed delay.
                helper.succeedWhen(() -> {
                    helper.assertTrue(first.sunFraction() > 0f, "sunlight did not charge");
                    TestPlayers.remove(player);
                });
            } catch (RuntimeException | AssertionError e) {
                TestPlayers.remove(player);
                throw e;
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void chargingSpendsDustAndRenameNeedsOwnerNearby(GameTestHelper helper) {
        BlockPos origin = new BlockPos(58_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 4, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel space = player.serverLevel();
            try {
                BlockPos pos = origin.offset(2, 0, 0);
                place(player, pos);
                player.setData(ModAttachments.DUST, DustData.of(25, 25, 0, 0));
                helper.assertTrue(BackupCores.charge(player, pos), "first charge failed");
                helper.assertTrue(BackupCores.charge(player, pos), "second charge failed");
                helper.assertFalse(BackupCores.charge(player, pos), "charged without enough dust");
                helper.assertTrue(core(space, pos).charges() == 2, "charges: " + core(space, pos).charges());
                DustData left = player.getData(ModAttachments.DUST);
                helper.assertTrue(Math.abs(left.get(DustKinds.METALLIC) - 5) < 1e-3 && Math.abs(left.get(DustKinds.SILICATE) - 5) < 1e-3, "dust left: " + left);
                helper.assertTrue(BackupCores.list(player).cores().get(0).charges() == 2, "core list not updated");

                helper.assertTrue(BackupCores.rename(player, pos, "  Home  "), "rename failed");
                helper.assertTrue(core(space, pos).name().equals("Home"), "name: " + core(space, pos).name());
                player.teleportTo(space, origin.getX() + 30.5, origin.getY(), origin.getZ() + 0.5, 0f, 0f);
                helper.assertFalse(BackupCores.rename(player, pos, "Far"), "renamed from far away");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void respawnAtChosenCoreSpendsACharge(GameTestHelper helper) {
        BlockPos origin = new BlockPos(59_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 4, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel space = player.serverLevel();
            ServerPlayer respawned = null;
            try {
                BlockPos pos = origin.offset(2, 0, 0);
                place(player, pos);
                core(space, pos).setCharges(1);
                player.teleportTo(space, origin.getX() + 100.5, origin.getY(), origin.getZ() + 0.5, 0f, 0f);
                player.hurt(space.damageSources().genericKill(), Float.MAX_VALUE);
                helper.assertTrue(player.isDeadOrDying(), "player did not die");
                helper.assertTrue(player.getData(ModAttachments.DEATH_MARKER).pos().isPresent(), "no death marker");

                BackupCores.select(player, Optional.of(GlobalPos.of(space.dimension(), pos)));
                respawned = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
                double distance = respawned.position().distanceTo(Vec3.atCenterOf(pos));
                helper.assertTrue(respawned.level() == space && distance < 2, "not restored at the core, " + distance + " away");
                helper.assertTrue(core(space, pos).charges() == 0, "charge not spent");
                helper.assertTrue(BackupCores.list(respawned).selected().isEmpty(), "choice not cleared");
                helper.assertTrue(respawned.getData(ModAttachments.DEATH_MARKER).pos().isPresent(), "death marker lost on respawn");

                // No charge left: the free spawn, even if chosen.
                respawned.hasChangedDimension();
                respawned.hurt(space.damageSources().genericKill(), Float.MAX_VALUE);
                BackupCores.select(respawned, Optional.of(GlobalPos.of(space.dimension(), pos)));
                ServerPlayer again = respawned.server.getPlayerList().respawn(respawned, false, Entity.RemovalReason.KILLED);
                respawned = again;
                helper.assertTrue(again.position().distanceTo(SpawnHandler.spawnPosition()) < 2, "empty core used for respawn");
                helper.succeed();
            } finally {
                TestPlayers.remove(respawned != null ? respawned : player);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void deathMarkerClearsOnArrival(GameTestHelper helper) {
        BlockPos origin = new BlockPos(60_000, 80, 0);
        NaniteGameTests.inLoadedArea(helper, origin, 2, () -> {
            ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, origin.getX() + 50.5, origin.getY(), origin.getZ() + 0.5);
            ServerLevel space = player.serverLevel();
            try {
                player.setData(ModAttachments.DEATH_MARKER,
                        new com.voidreclaimer.core.DeathMarker(Optional.of(GlobalPos.of(space.dimension(), origin))));
                for (int i = 0; i < 20; i++) {
                    player.doTick();
                }
                helper.assertTrue(player.getData(ModAttachments.DEATH_MARKER).pos().isPresent(), "cleared while far away");
                player.teleportTo(space, origin.getX() + 3.5, origin.getY(), origin.getZ() + 0.5, 0f, 0f);
                for (int i = 0; i < 20; i++) {
                    player.doTick();
                }
                helper.assertTrue(player.getData(ModAttachments.DEATH_MARKER).pos().isEmpty(), "not cleared on arrival");
                helper.succeed();
            } finally {
                TestPlayers.remove(player);
            }
        });
    }

    @GameTest(template = "empty")
    public static void starterNebulaIsLockedAtWorldCreation(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        helper.assertTrue(space.hasData(ModAttachments.STARTER_NEBULA), "nebula choice not locked in");
        boolean enabled = space.getData(ModAttachments.STARTER_NEBULA).enabled();
        DustFieldSeed seed = space.getData(ModAttachments.DUST_FIELD_SEED);
        helper.assertTrue(seed.nebula().isPresent() == enabled, "seed nebula doesn't match the locked choice");
        boolean rule = space.getServer().getGameRules().getBoolean(StarterNebula.RULE);
        space.getServer().getGameRules().getRule(StarterNebula.RULE).set(!rule, space.getServer());
        try {
            helper.assertTrue(space.getData(ModAttachments.STARTER_NEBULA).enabled() == enabled, "rule change altered an existing world");
        } finally {
            space.getServer().getGameRules().getRule(StarterNebula.RULE).set(rule, space.getServer());
        }
        helper.succeed();
    }
}
