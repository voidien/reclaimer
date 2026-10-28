package com.voidreclaimer.space;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

/**
 * Players never start or respawn in the Overworld.
 *
 * <ul>
 *   <li>Login: anyone who isn't allowed in their current (locked) dimension is moved to the deep space spawn. This
 *       covers brand new players, who vanilla always places in the Overworld first.</li>
 *   <li>Switching from creative/spectator to survival/adventure inside a locked dimension does the same.</li>
 *   <li>Respawn: always the deep space spawn, unless the player has a valid respawn point inside deep space (the
 *       Backup Core, Phase 4). Vanilla beds and anchors can't be set there, see {@link SpawnDeviceBlocker}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class SpawnHandler {
    private SpawnHandler() {}

    public static Vec3 spawnPosition() {
        return new Vec3(Config.SPAWN_X.get() + 0.5, Config.SPAWN_Y.get(), Config.SPAWN_Z.get() + 0.5);
    }

    private static ServerLevel deepSpace(ServerPlayer player) {
        ServerLevel level = player.server.getLevel(ModDimensions.DEEP_SPACE);
        if (level == null) {
            VoidReclaimer.LOGGER.error("Dimension {} is missing; is the mod's datapack disabled?", ModDimensions.DEEP_SPACE.location());
        }
        return level;
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            moveToDeepSpaceIfStranded(player);
        }
    }

    @SubscribeEvent
    static void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        GameType next = event.getNewGameMode();
        if (event.getEntity() instanceof ServerPlayer player && (next == GameType.SURVIVAL || next == GameType.ADVENTURE)) {
            // The event fires before the change; queue the check so it sees the new mode.
            player.server.tell(new TickTask(player.server.getTickCount(), () -> moveToDeepSpaceIfStranded(player)));
        }
    }

    @SubscribeEvent
    static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        DimensionTransition original = event.getDimensionTransition();
        boolean validDeepSpaceRespawn = original.newLevel().dimension() == ModDimensions.DEEP_SPACE
                && player.getRespawnPosition() != null
                && !original.missingRespawnBlock();
        if (validDeepSpaceRespawn) {
            return;
        }
        ServerLevel level = deepSpace(player);
        if (level != null) {
            event.setDimensionTransition(new DimensionTransition(
                    level, spawnPosition(), Vec3.ZERO, 0f, 0f, DimensionTransition.DO_NOTHING));
        }
    }

    private static void moveToDeepSpaceIfStranded(ServerPlayer player) {
        if (player.isRemoved() || !DimensionLock.isLocked(player.level().dimension()) || DimensionLock.canBypass(player)) {
            return;
        }
        ServerLevel level = deepSpace(player);
        if (level != null) {
            Vec3 pos = spawnPosition();
            player.teleportTo(level, pos.x, pos.y, pos.z, 0f, 0f);
        }
    }
}
