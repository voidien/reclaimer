package com.voidreclaimer.space;

import com.voidreclaimer.Config;
import com.voidreclaimer.VoidReclaimer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Soft push-back near the top and bottom build limits of deep space, so drifting entities never reach vanilla's
 * void damage. The push grows linearly through the margin; past the build limit, movement further out is also
 * stopped. Runs where the entity's movement is simulated: the local client for players, the server for everything
 * else.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class VoidBoundary {
    private VoidBoundary() {}

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (!ModDimensions.isDeepSpace(level) || entity.isPassenger()) {
            return;
        }
        if (entity instanceof Player player) {
            if (!player.isLocalPlayer() || player.isSpectator()) {
                return;
            }
        } else if (level.isClientSide()) {
            return;
        }

        double margin = Config.BOUNDARY_MARGIN.get();
        double push = Config.BOUNDARY_PUSH.get();
        double y = entity.getY();
        double bottom = level.getMinBuildHeight();
        double top = level.getMaxBuildHeight();
        Vec3 motion = entity.getDeltaMovement();
        double dy = motion.y;

        if (y < bottom + margin) {
            dy += push * Math.min((bottom + margin - y) / margin, 1.0);
            if (y < bottom) {
                dy = Math.max(dy, push);
            }
        } else if (y > top - margin) {
            dy -= push * Math.min((y - (top - margin)) / margin, 1.0);
            if (y > top) {
                dy = Math.min(dy, -push);
            }
        } else {
            return;
        }
        entity.setDeltaMovement(motion.x, dy, motion.z);
        entity.hasImpulse = true;
    }
}
