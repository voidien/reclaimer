package com.voidreclaimer.gametest;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import com.voidreclaimer.space.SpawnHandler;
import com.voidreclaimer.space.ZeroGravity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Phase 1: deep space, spawn, dimension lock, zero gravity, void boundary. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceGameTests {
    private SpaceGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void zeroGravityHoldsEntitiesAndBlocks(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        BlockPos origin = new BlockPos(10_000, 64, 0);
        space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        // Place things once the forced chunk is fully ticking, as it would be around a player.
        helper.runAfterDelay(5, () -> {
            // The game test world persists between runs; start from a clean area.
            space.getEntitiesOfClass(Entity.class, new AABB(origin).inflate(16)).forEach(Entity::discard);
            space.setBlock(origin, Blocks.SAND.defaultBlockState(), 3);
            space.setBlock(origin.east(2), Blocks.ANVIL.defaultBlockState(), 3);
            ItemEntity item = new ItemEntity(space, origin.getX() + 4.5, origin.getY(), origin.getZ() + 0.5, new ItemStack(Items.STONE));
            item.setDeltaMovement(Vec3.ZERO);
            space.addFreshEntity(item);
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, space);
            stand.moveTo(origin.getX() + 6.5, origin.getY(), origin.getZ() + 0.5);
            space.addFreshEntity(stand);

            helper.runAfterDelay(60, () -> {
                try {
                    helper.assertTrue(space.getBlockState(origin).is(Blocks.SAND), "sand fell in zero-G");
                    helper.assertTrue(space.getBlockState(origin.east(2)).is(Blocks.ANVIL), "anvil fell in zero-G");
                    helper.assertTrue(space.getEntitiesOfClass(FallingBlockEntity.class, new AABB(origin).inflate(8)).isEmpty(),
                            "a gravity block detached into a falling block entity");
                    helper.assertTrue(item.isNoGravity() && item.getTags().contains(ZeroGravity.NO_GRAVITY_TAG), "item not zero-G");
                    helper.assertTrue(Math.abs(item.getY() - origin.getY()) < 0.01, "item moved vertically: " + item.getY());
                    helper.assertTrue(stand.getAttributeValue(Attributes.GRAVITY) == 0.0, "armor stand gravity not 0");
                    helper.assertTrue(Math.abs(stand.getY() - origin.getY()) < 0.01, "armor stand moved vertically");
                    helper.succeed();
                } finally {
                    item.discard();
                    stand.discard();
                    space.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                    space.setBlock(origin.east(2), Blocks.AIR.defaultBlockState(), 3);
                    space.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
                }
            });
        });
    }

    @GameTest(template = "empty")
    public static void gravityStillWorksInOverworld(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, 1, 2, 1);
        helper.assertTrue(stand.getAttributeValue(Attributes.GRAVITY) > 0.0, "overworld armor stand has no gravity");
        ItemEntity item = helper.spawnItem(Items.STONE, 1, 2, 1);
        helper.assertFalse(item.isNoGravity(), "overworld item is zero-G");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void voidBoundaryPushesBack(GameTestHelper helper) {
        ServerLevel space = TestPlayers.deepSpace(helper);
        int x = 11_000;
        space.setChunkForced(x >> 4, 0, true);
        double bottom = space.getMinBuildHeight() + 5;
        double top = space.getMaxBuildHeight() - 5;
        Entity low = spawnFloatingItem(space, x + 0.5, bottom, 0.5);
        Entity high = spawnFloatingItem(space, x + 2.5, top, 0.5);
        helper.runAfterDelay(100, () -> {
            try {
                helper.assertTrue(!low.isRemoved() && low.getY() > bottom + 5, "bottom item not pushed up: " + low.getY());
                helper.assertTrue(!high.isRemoved() && high.getY() < top - 5, "top item not pushed down: " + high.getY());
                helper.succeed();
            } finally {
                low.discard();
                high.discard();
                space.setChunkForced(x >> 4, 0, false);
            }
        });
    }

    @GameTest(template = "empty")
    public static void survivalLoginMovesToDeepSpaceSpawn(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.join(helper);
        try {
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            helper.assertTrue(player.level().dimension() == Level.OVERWORLD, "precondition: player starts in the overworld");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            assertAtDeepSpaceSpawn(helper, player);
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty")
    public static void creativeLoginMayStayInOverworld(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.join(helper);
        try {
            helper.assertTrue(player.isCreative(), "game test players join in creative");
            helper.assertTrue(player.level().dimension() == Level.OVERWORLD, "creative player was moved");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void switchingToSurvivalInOverworldMovesToDeepSpace(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.join(helper);
        player.setGameMode(GameType.SURVIVAL);
        helper.succeedWhen(() -> {
            helper.assertTrue(player.level().dimension() == ModDimensions.DEEP_SPACE, "not moved yet");
            TestPlayers.remove(player);
        });
    }

    @GameTest(template = "empty")
    public static void respawnIgnoresOverworldSpawnPoint(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 12_000.5, 80, 0.5);
        ServerPlayer respawned = null;
        try {
            player.setRespawnPosition(Level.OVERWORLD, new BlockPos(0, 100, 0), 0f, true, false);
            respawned = player.server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
            assertAtDeepSpaceSpawn(helper, respawned);
            helper.assertTrue(respawned.getAttributeValue(Attributes.GRAVITY) == 0.0, "respawned player has gravity");
            helper.succeed();
        } finally {
            TestPlayers.remove(respawned != null ? respawned : player);
        }
    }

    @GameTest(template = "empty")
    public static void dimensionLockBlocksSurvivalButNotCreative(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.survivalInDeepSpace(helper, 13_000.5, 80, 0.5);
        try {
            ServerLevel overworld = player.server.overworld();
            player.teleportTo(overworld, 0.5, 100, 0.5, 0f, 0f);
            helper.assertTrue(player.level().dimension() == ModDimensions.DEEP_SPACE, "survival player reached the overworld");
            player.teleportTo(player.server.getLevel(Level.NETHER), 0.5, 100, 0.5, 0f, 0f);
            helper.assertTrue(player.level().dimension() == ModDimensions.DEEP_SPACE, "survival player reached the nether");

            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            player.teleportTo(overworld, 0.5, 100, 0.5, 0f, 0f);
            helper.assertTrue(player.level().dimension() == Level.OVERWORLD, "creative player was blocked");
            helper.succeed();
        } finally {
            TestPlayers.remove(player);
        }
    }

    private static void assertAtDeepSpaceSpawn(GameTestHelper helper, ServerPlayer player) {
        helper.assertTrue(player.level().dimension() == ModDimensions.DEEP_SPACE, "player not in deep space: " + player.level().dimension());
        Vec3 spawn = SpawnHandler.spawnPosition();
        helper.assertTrue(player.position().distanceTo(spawn) < 0.01,
                "player at " + player.position() + ", expected spawn " + spawn + " (config y=" + Config.SPAWN_Y.get() + ")");
    }

    private static Entity spawnFloatingItem(ServerLevel level, double x, double y, double z) {
        ItemEntity item = new ItemEntity(level, x, y, z, new ItemStack(Items.DIRT));
        item.setDeltaMovement(Vec3.ZERO);
        item.setNeverPickUp();
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
        return item;
    }
}
