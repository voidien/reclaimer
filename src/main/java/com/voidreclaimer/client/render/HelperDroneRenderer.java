package com.voidreclaimer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.helper.HelperDrone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Draws a helper drone with the mining probe model ({@code geo/entity/mining_probe.geo.json}) at half size. */
public class HelperDroneRenderer extends GeoEntityRenderer<HelperDrone> {
    public HelperDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(VoidReclaimer.id("mining_probe")));
        shadowRadius = 0f;
        withScale(0.5f);
    }

    /** GeckoLib only turns living entities; this one is a plain entity, so use its own yaw. */
    @Override
    protected void applyRotations(HelperDrone drone, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(drone, poseStack, ageInTicks, Mth.rotLerp(partialTick, drone.yRotO, drone.getYRot()), partialTick, nativeScale);
    }
}
