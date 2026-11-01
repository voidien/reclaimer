package com.voidreclaimer.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.earth.SkyEra;
import com.voidreclaimer.earth.SkyLook;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The sky of ruined Earth in its three eras ({@link SkyEra}, looks in {@link SkyLook}): a vertical gradient dome and a
 * tinted sun disc. Like {@link DeepSpaceEffects} it goes through the {@code renderSky} extension hook, so chunk
 * rendering stays untouched (Sodium-safe). Fog distance and color follow the era in {@link RuinedEarthFog}.
 */
public final class RuinedEarthEffects extends DimensionSpecialEffects {
    /** Must match the {@code effects} field of {@code dimension_type/ruined_earth.json}. */
    public static final ResourceLocation ID = VoidReclaimer.id("ruined_earth");
    private static final ResourceLocation SUN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final float SKY_RADIUS = 100f;
    /** Rings of the dome from straight down to straight up, and segments around. */
    private static final int RINGS = 16;
    private static final int SEGMENTS = 24;

    public RuinedEarthEffects() {
        super(192f, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    /** True means "handled": the ruined era has no clouds, the others let vanilla draw them. */
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return !SkyLook.in(level).clouds();
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW || fluid == FogType.WATER) {
            return false;
        }
        SkyLook look = SkyLook.in(level);

        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        FogRenderer.setupNoFog();

        drawDome(pose.last().pose(), look);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360f));
        Matrix4f sun = pose.last().pose();
        float half = look.sunHalfSize();
        float r = ((look.sunColor() >> 16) & 0xFF) / 255f;
        float g = ((look.sunColor() >> 8) & 0xFF) / 255f;
        float b = (look.sunColor() & 0xFF) / 255f;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(r, g, b, look.sunAlpha());
        RenderSystem.setShaderTexture(0, SUN_TEXTURE);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(sun, -half, SKY_RADIUS, -half).setUv(0f, 0f);
        builder.addVertex(sun, half, SKY_RADIUS, -half).setUv(1f, 0f);
        builder.addVertex(sun, half, SKY_RADIUS, half).setUv(1f, 1f);
        builder.addVertex(sun, -half, SKY_RADIUS, half).setUv(0f, 1f);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.popPose();

        setupFog.run();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        return true;
    }

    /** A sphere around the camera, horizon color from straight down up to the horizon, then fading to the zenith color. */
    private static void drawDome(Matrix4f frame, SkyLook look) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int ring = 0; ring < RINGS; ring++) {
            float lowAngle = Mth.lerp(ring / (float) RINGS, -Mth.HALF_PI, Mth.HALF_PI);
            float highAngle = Mth.lerp((ring + 1) / (float) RINGS, -Mth.HALF_PI, Mth.HALF_PI);
            int lowColor = colorAt(look, lowAngle);
            int highColor = colorAt(look, highAngle);
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float a0 = segment * Mth.TWO_PI / SEGMENTS;
                float a1 = (segment + 1) * Mth.TWO_PI / SEGMENTS;
                vertex(builder, frame, lowAngle, a0, lowColor);
                vertex(builder, frame, lowAngle, a1, lowColor);
                vertex(builder, frame, highAngle, a1, highColor);
                vertex(builder, frame, highAngle, a0, highColor);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    /** Horizon color at and below the horizon; above it, a smooth rise to the zenith color. */
    private static int colorAt(SkyLook look, float elevation) {
        if (elevation <= 0f) {
            return look.horizon();
        }
        float t = Mth.sqrt(elevation / Mth.HALF_PI);
        return SkyLook.mix(look.horizon(), look.zenith(), t);
    }

    private static void vertex(BufferBuilder builder, Matrix4f frame, float elevation, float azimuth, int rgb) {
        float horizontal = Mth.cos(elevation) * SKY_RADIUS;
        builder.addVertex(frame, Mth.sin(azimuth) * horizontal, Mth.sin(elevation) * SKY_RADIUS, Mth.cos(azimuth) * horizontal)
                .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }
}
