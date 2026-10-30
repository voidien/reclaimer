package com.voidreclaimer.mixin;

import com.voidreclaimer.client.camera.PhotoMode;
import com.voidreclaimer.client.camera.PhotoState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Photo mode needs the camera somewhere other than the body, which no NeoForge event allows (they change angles, roll and
 * field of view only). After {@code Camera.setup} has run, this moves the camera to the free position and marks it detached so
 * the body is drawn.
 */
@Mixin(Camera.class)
abstract class CameraPhotoMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    private boolean detached;

    @Inject(method = "setup", at = @At("TAIL"))
    private void void_reclaimer$photoMode(CallbackInfo ci) {
        if (PhotoMode.active()) {
            PhotoState state = PhotoMode.state();
            setRotation(state.yaw(), state.pitch(), state.roll());
            setPosition(PhotoMode.position());
            detached = true;
        }
    }
}
