package com.voidreclaimer.mixin;

import com.voidreclaimer.flight.ThrusterFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla travel applies air drag every tick, which would stop zero-G drift. For the local player under thruster
 * flight, travel is replaced by {@link ThrusterFlight#travel}. Covers {@code Player.travel}, which ends in this method.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void void_reclaimer$thrusterTravel(Vec3 travelVector, CallbackInfo ci) {
        if ((Object) this instanceof Player player && player.isLocalPlayer() && ThrusterFlight.isActive(player)) {
            ThrusterFlight.travel(player);
            ci.cancel();
        }
    }
}
