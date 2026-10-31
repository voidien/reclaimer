package com.voidreclaimer.client.render;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.launch.LaunchAnimation;
import com.voidreclaimer.launch.LaunchControllerBlockEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The launch complex's model: the owner's {@code geo/entity/mass_driver.geo.json} with {@code textures/entity/mass_driver.png}.
 * The bones are posed in code ({@link LaunchAnimation}): the animation file is an empty placeholder.
 */
public class LaunchComplexModel extends DefaultedEntityGeoModel<LaunchControllerBlockEntity> {
    public LaunchComplexModel() {
        super(VoidReclaimer.id("mass_driver"));
    }

    @Override
    public void setCustomAnimations(LaunchControllerBlockEntity controller, long instanceId, AnimationState<LaunchControllerBlockEntity> state) {
        super.setCustomAnimations(controller, instanceId, state);
        long gameTime = controller.getLevel() == null ? 0L : controller.getLevel().getGameTime();
        float time = gameTime + state.getPartialTick();
        float sinceLaunch = controller.lastLaunch() < 0 ? -1f : time - controller.lastLaunch();
        LaunchAnimation.Pose pose = LaunchAnimation.at(controller.shownProgress(state.getPartialTick()), sinceLaunch, time);
        getBone("gun").ifPresent(bone -> bone.setRotX((float) Math.toRadians(pose.gunPitch())));
        getBone("dish_a").ifPresent(bone -> bone.setRotY((float) Math.toRadians(pose.dishA())));
        getBone("dish_b").ifPresent(bone -> bone.setRotY((float) Math.toRadians(pose.dishB())));
        getBone("crane").ifPresent(bone -> bone.setRotY((float) Math.toRadians(pose.crane())));
        getBone("crane2").ifPresent(bone -> bone.setRotY((float) Math.toRadians(pose.crane2())));
    }
}
