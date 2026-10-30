package com.voidreclaimer.mixin;

import com.voidreclaimer.client.camera.OrbitCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The orbit camera turns the camera round the body before the third-person camera is moved back, so it has to change the
 * rotation {@code Camera.setup} starts from; NeoForge's camera events only change the final view angles, which would look
 * away from the body instead of round it. Both offsets are 0 unless the orbit key is held or still easing back.
 */
@Mixin(Camera.class)
abstract class CameraOrbitMixin {
    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float void_reclaimer$orbitYaw(Entity entity, float partialTick) {
        return entity.getViewYRot(partialTick) + OrbitCamera.yawOffset(partialTick);
    }

    @Redirect(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float void_reclaimer$orbitPitch(Entity entity, float partialTick) {
        return entity.getViewXRot(partialTick) + OrbitCamera.pitchOffset(partialTick);
    }
}
