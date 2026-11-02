package com.voidreclaimer.mixin;

import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.client.camera.FirstPersonCockpit;
import com.voidreclaimer.client.camera.PhotoMode;
import com.voidreclaimer.client.camera.PhotoState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Photo mode needs the camera somewhere other than the body, which no NeoForge event allows (they change angles, roll and
 * field of view only). After {@code Camera.setup} has run, this moves the camera to the free position and marks it detached so
 * the body is drawn. It also moves the first-person camera to a big ship's cockpit.
 */
@Mixin(Camera.class)
abstract class CameraPhotoMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    @Shadow
    private boolean detached;

    private Camera self() {
        return (Camera) (Object) this;
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void void_reclaimer$photoMode(CallbackInfo ci) {
        if (PhotoMode.active()) {
            PhotoState state = PhotoMode.state();
            setRotation(state.yaw(), state.pitch(), state.roll());
            setPosition(PhotoMode.position());
            detached = true;
        } else if (!detached && self().getEntity() instanceof Player player) {
            Chassis.Cockpit cockpit = FirstPersonCockpit.of(player);
            if (cockpit != null) {
                // Move the eyes to the cockpit: forward along the view, up and right across it, scaled with the body.
                float scale = player.getScale();
                Camera camera = self();
                Vector3f look = camera.getLookVector();
                Vector3f up = camera.getUpVector();
                Vector3f left = camera.getLeftVector();
                // A big body lags the view, and the cockpit is where the body is: the eyes stay in it while the look moves on ahead.
                float partial = net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                com.voidreclaimer.body.BodyHeading heading = com.voidreclaimer.body.BodyTurning.at(player, partial);
                if (heading != null) {
                    var turn = com.voidreclaimer.body.BodyBoxes.turn(heading.yaw(), heading.pitch());
                    var offset = turn.transform(new org.joml.Vector3d(cockpit.right(), cockpit.up(), -cockpit.forward())).mul(scale);
                    setPosition(camera.getPosition().add(offset.x, offset.y, offset.z));
                    return;
                }
                setPosition(camera.getPosition().add(
                        (look.x * cockpit.forward() + up.x * cockpit.up() - left.x * cockpit.right()) * scale,
                        (look.y * cockpit.forward() + up.y * cockpit.up() - left.y * cockpit.right()) * scale,
                        (look.z * cockpit.forward() + up.z * cockpit.up() - left.z * cockpit.right()) * scale));
            }
        }
    }
}
