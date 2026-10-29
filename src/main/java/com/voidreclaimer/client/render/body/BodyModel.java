package com.voidreclaimer.client.render.body;

import com.voidreclaimer.VoidReclaimer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Picks the rig, texture and animation file of the body being drawn right now ({@link BodyRenderer#frame()}). */
public final class BodyModel extends GeoModel<BodyAnimatable> {
    @Override
    public ResourceLocation getModelResource(BodyAnimatable animatable) {
        return VoidReclaimer.id("geo/body/" + BodyRenderer.currentRig() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BodyAnimatable animatable) {
        BodyRenderer.Frame frame = BodyRenderer.frame();
        return frame == null ? BodyTextures.art(BodyLook.DEFAULT_RIG) : frame.rigTexture();
    }

    @Override
    public ResourceLocation getTextureResource(BodyAnimatable animatable, GeoRenderer<BodyAnimatable> renderer) {
        return getTextureResource(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(BodyAnimatable animatable) {
        return VoidReclaimer.id("animations/body/" + BodyRenderer.currentRig() + ".animation.json");
    }
}
