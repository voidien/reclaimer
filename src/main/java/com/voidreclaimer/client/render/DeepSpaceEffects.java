package com.voidreclaimer.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dyson.DysonSphereData;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Deep space sky: black, a full sphere of stars (also below the horizon), and a small distant sun fixed high
 * overhead. Vanilla sky light only travels downward, so the sun is placed near the zenith to match where the light
 * comes from. No clouds, no sunrise glow, black fog.
 *
 * <p>Goes through the {@code renderSky} extension hook instead of patching LevelRenderer, which keeps Sodium happy.
 */
public final class DeepSpaceEffects extends DimensionSpecialEffects {
    /** Must match the {@code effects} field of {@code dimension_type/deep_space.json}. */
    public static final ResourceLocation ID = VoidReclaimer.id("deep_space");
    private static final ResourceLocation SUN_TEXTURE = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");

    private static final int STAR_COUNT = 3000;
    private static final long STAR_SEED = 0x5EED_0F_5AL;
    private static final float STAR_BRIGHTNESS = 0.85f;
    private static final float SKY_RADIUS = 100f;
    /** Vanilla's sun is 30; a smaller disc reads as farther away. */
    private static final float SUN_HALF_SIZE = 9f;
    private static final float SUN_TILT_X_DEGREES = 12f;
    private static final float SUN_TILT_Z_DEGREES = -8f;

    private VertexBuffer starBuffer;

    public DeepSpaceEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        FogType fluid = camera.getFluidInCamera();
        if (fluid == FogType.LAVA || fluid == FogType.POWDER_SNOW) {
            return true;
        }

        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        FogRenderer.setupNoFog();
        RenderSystem.setShaderColor(STAR_BRIGHTNESS, STAR_BRIGHTNESS, STAR_BRIGHTNESS, STAR_BRIGHTNESS);
        VertexBuffer stars = stars();
        stars.bind();
        stars.drawWithShader(pose.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();

        DysonSphereData dyson = DysonSphereData.of(level);
        float[] tint = DysonSky.sunTint(dyson);
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
        Matrix4f view = new Matrix4f(pose.last().pose());
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(SUN_TILT_X_DEGREES));
        pose.mulPose(Axis.ZP.rotationDegrees(SUN_TILT_Z_DEGREES));
        Matrix4f sun = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN_TEXTURE);
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(sun, -SUN_HALF_SIZE, SKY_RADIUS, -SUN_HALF_SIZE).setUv(0f, 0f);
        builder.addVertex(sun, SUN_HALF_SIZE, SKY_RADIUS, -SUN_HALF_SIZE).setUv(1f, 0f);
        builder.addVertex(sun, SUN_HALF_SIZE, SKY_RADIUS, SUN_HALF_SIZE).setUv(1f, 1f);
        builder.addVertex(sun, -SUN_HALF_SIZE, SKY_RADIUS, SUN_HALF_SIZE).setUv(0f, 1f);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        DysonSky.render(sun, view, camera.getPosition(), dyson, ticks + partialTick);
        pose.popPose();

        setupFog.run();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        return true;
    }

    private VertexBuffer stars() {
        if (starBuffer == null) {
            starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            starBuffer.bind();
            starBuffer.upload(buildStars(Tesselator.getInstance()));
            VertexBuffer.unbind();
        }
        return starBuffer;
    }

    /** Same construction as vanilla's star field, with more and slightly varied stars. */
    private static MeshData buildStars(Tesselator tesselator) {
        RandomSource random = RandomSource.create(STAR_SEED);
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < STAR_COUNT; i++) {
            float x = random.nextFloat() * 2f - 1f;
            float y = random.nextFloat() * 2f - 1f;
            float z = random.nextFloat() * 2f - 1f;
            float size = 0.12f + random.nextFloat() * 0.16f;
            float lengthSq = Mth.lengthSquared(x, y, z);
            if (lengthSq <= 0.01f || lengthSq >= 1f) {
                continue;
            }
            Vector3f center = new Vector3f(x, y, z).normalize(SKY_RADIUS);
            float spin = (float) (random.nextDouble() * Math.PI * 2.0);
            Quaternionf rotation = new Quaternionf().rotateTo(new Vector3f(0f, 0f, -1f), center).rotateZ(spin);
            builder.addVertex(new Vector3f(center).add(new Vector3f(size, -size, 0f).rotate(rotation)));
            builder.addVertex(new Vector3f(center).add(new Vector3f(size, size, 0f).rotate(rotation)));
            builder.addVertex(new Vector3f(center).add(new Vector3f(-size, size, 0f).rotate(rotation)));
            builder.addVertex(new Vector3f(center).add(new Vector3f(-size, -size, 0f).rotate(rotation)));
        }
        return builder.buildOrThrow();
    }

    /** Installed as {@link com.voidreclaimer.network.LaunchStreakPayload#clientHandler}: draws a streak if the player is in deep space. */
    public static void onLaunchStreak(com.voidreclaimer.network.LaunchStreakPayload payload) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null && com.voidreclaimer.space.ModDimensions.isDeepSpace(level)) {
            DysonSky.addStreak(payload.from(), payload.truss());
        }
    }
}
