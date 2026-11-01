package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.client.render.body.BodyBounds;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Any GeckoLib model on a Holo Projector. A model id such as {@code void_reclaimer:entity/mining_probe} means
 * {@code geo/entity/mining_probe.geo.json} and {@code textures/entity/mining_probe.png} in that namespace; without that
 * texture the glow is used, so a model that keeps its texture elsewhere still shows as a plain hologram.
 */
final class HoloModels {
    private static final Subject SUBJECT = new Subject();
    private static final Model MODEL = new Model();
    private static final Renderer RENDERER = new Renderer();

    private HoloModels() {}

    static ResourceLocation geoFile(ResourceLocation id) {
        return id.withPath(path -> "geo/" + path + ".geo.json");
    }

    static ResourceLocation textureFile(ResourceLocation id) {
        return id.withPath(path -> "textures/" + path + ".png");
    }

    /** The baked model of an id, or null when no such model is loaded. */
    @Nullable
    static BakedGeoModel baked(ResourceLocation id) {
        return GeckoLibCache.getBakedModels().get(geoFile(id));
    }

    /** Draws a model so that its longest side is {@code size} blocks, centred on the pose. */
    static void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation id, BakedGeoModel baked, float size, float alpha, float partialTick) {
        float[] box = BodyBounds.box(baked);
        HoloTransform.Fit fit = HoloTransform.fit(box, size);
        boolean textured = Minecraft.getInstance().getResourceManager().getResource(textureFile(id)).isPresent();
        MODEL.show(id, textured ? textureFile(id) : HoloStyle.GLOW);
        pose.pushPose();
        pose.scale(fit.scale(), fit.scale(), fit.scale());
        pose.translate(-fit.centerX(), -fit.centerY(), -fit.centerZ());
        // GeoObjectRenderer moves a model to the middle of its block; a hologram places its own.
        pose.translate(-0.5f, -0.51f, -0.5f);
        RENDERER.color = HoloStyle.argb(HoloStyle.MODEL_TINT, alpha);
        RENDERER.render(pose, SUBJECT, buffers, null, null, LightTexture.FULL_BRIGHT, partialTick);
        RENDERER.color = HoloStyle.argb(HoloStyle.CYAN, HoloStyle.WASH_ALPHA * alpha);
        RENDERER.render(pose, SUBJECT, buffers, RenderType.entityTranslucentEmissive(HoloStyle.PANEL, false), null, LightTexture.FULL_BRIGHT, partialTick);
        pose.popPose();
    }

    /** Nothing to animate: one shared subject with no controllers. */
    private static final class Subject implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return 0;
        }
    }

    /** Whichever model is being drawn this moment; only used on the render thread. */
    private static final class Model extends GeoModel<Subject> {
        private ResourceLocation geo;
        private ResourceLocation texture;

        void show(ResourceLocation id, ResourceLocation texture) {
            this.geo = geoFile(id);
            this.texture = texture;
        }

        @Override
        public ResourceLocation getModelResource(Subject subject) {
            return geo;
        }

        @Override
        public ResourceLocation getTextureResource(Subject subject) {
            return texture;
        }

        /** Never loaded: a subject has no controllers to play it. */
        @Override
        public ResourceLocation getAnimationResource(Subject subject) {
            return geo;
        }
    }

    private static final class Renderer extends GeoObjectRenderer<Subject> {
        private int color = -1;

        Renderer() {
            super(MODEL);
        }

        @Override
        public RenderType getRenderType(Subject subject, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucentEmissive(texture, false);
        }

        @Override
        public Color getRenderColor(Subject subject, float partialTick, int packedLight) {
            return new Color(color);
        }
    }
}
