package com.voidreclaimer.space;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Zero gravity for every entity in deep space.
 *
 * <ul>
 *   <li>Living entities (including players) get a transient {@code generic.gravity} modifier that multiplies gravity
 *       to zero. The attribute is synced, so the client-side player movement agrees, and vanilla's "floating too
 *       long" kick is disabled automatically for zero-gravity players.</li>
 *   <li>Other entities (items, XP, projectiles, falling blocks) get the vanilla NoGravity flag plus a tag, so the
 *       flag is only cleared on entities this mod set it on.</li>
 *   <li>Fall damage is cancelled; gravity-affected blocks are held in place by {@code FallingBlockMixin}.</li>
 * </ul>
 * Fluids still flow normally.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID)
public final class ZeroGravity {
    public static final ResourceLocation GRAVITY_MODIFIER_ID = VoidReclaimer.id("zero_gravity");
    public static final String NO_GRAVITY_TAG = VoidReclaimer.MOD_ID + ".zero_g";
    private static final AttributeModifier ZERO_GRAVITY = new AttributeModifier(
            GRAVITY_MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    /** Spike only: -Dvoid_reclaimer.spike.noZeroG=true turns our zero-G systems off. */
    public static final boolean SPIKE_NO_ZERO_G = Boolean.getBoolean("void_reclaimer.spike.noZeroG");

    private ZeroGravity() {}

    /** Whether gravity is off at this position. Gravity generators (later phases) will hook in here. */
    public static boolean isZeroG(Level level, BlockPos pos) {
        return !SPIKE_NO_ZERO_G && ModDimensions.isDeepSpace(level);
    }

    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        boolean zeroG = isZeroG(event.getLevel(), entity.blockPosition());
        if (entity instanceof LivingEntity living) {
            // Applied on both sides so the local player's predicted movement matches before the attribute sync lands.
            AttributeInstance gravity = living.getAttribute(Attributes.GRAVITY);
            if (gravity == null) {
                return;
            }
            if (zeroG && !gravity.hasModifier(GRAVITY_MODIFIER_ID)) {
                gravity.addTransientModifier(ZERO_GRAVITY);
            } else if (!zeroG) {
                gravity.removeModifier(GRAVITY_MODIFIER_ID);
            }
        } else if (!event.getLevel().isClientSide()) {
            if (zeroG && !entity.isNoGravity()) {
                entity.setNoGravity(true);
                entity.addTag(NO_GRAVITY_TAG);
            } else if (!zeroG && entity.getTags().contains(NO_GRAVITY_TAG)) {
                entity.setNoGravity(false);
                entity.removeTag(NO_GRAVITY_TAG);
            }
        }
    }

    @SubscribeEvent
    static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (isZeroG(entity.level(), entity.blockPosition())) {
            event.setCanceled(true);
        }
    }
}
