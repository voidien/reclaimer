package com.voidreclaimer.space;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The vanilla dimensions are unreachable. Endgame travel goes to {@code void_reclaimer:ruined_earth} instead.
 * Players in creative or spectator mode may bypass the lock for debugging.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class DimensionLock {
    private DimensionLock() {}

    public static boolean isLocked(ResourceKey<Level> dimension) {
        return dimension == Level.OVERWORLD || dimension == Level.NETHER || dimension == Level.END;
    }

    public static boolean canBypass(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    /** Covers portals and cross-dimension /tp and /execute in ... run tp (both go through changeDimension). */
    @SubscribeEvent
    static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!isLocked(event.getDimension())) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Player player && canBypass(player)) {
            return;
        }
        event.setCanceled(true);
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.void_reclaimer.dimension_locked"), true);
        }
    }

    /** Nether portals can't be lit in deep space; otherwise the destination portal would still generate. */
    @SubscribeEvent
    static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof Level l && ModDimensions.isDeepSpace(l)) {
            event.setCanceled(true);
        }
    }
}
