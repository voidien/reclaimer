package com.voidreclaimer.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.ClientConfig;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.CloudShape;
import com.voidreclaimer.dust.DustCloud;
import com.voidreclaimer.dust.DustField;
import com.voidreclaimer.dust.DustKinds;
import com.voidreclaimer.dust.DustSensor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Dust Sensor scanner view (toggle with the scanner key): every cloud within {@link #RANGE} blocks of its edge is drawn
 * as a wire mesh in its kind's color, following its true lumpy edge ({@link CloudShape}), with a faint tinted shell.
 *
 * <p>Meshes are computed once per cloud on a background thread and cached. Drawn through a NeoForge render stage as
 * plain lines and translucent quads, unfogged (fog state is restored afterwards); chunk rendering is untouched
 * (Sodium-safe).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class CloudScannerRenderer {
    /** Clouds whose edge is within this distance are shown (the sensor hum range). */
    public static final double RANGE = 512.0;
    private static final float LINE_ALPHA = 0.55f;
    private static final float SHELL_ALPHA = 0.07f;
    private static final int MAX_CACHED = 32;

    private static final Map<Key, CompletableFuture<CloudShape>> SHAPES = new HashMap<>();

    private CloudScannerRenderer() {}

    private record Key(long seed, DustCloud cloud) {}

    @SubscribeEvent
    static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !ClientConfig.CLOUD_SCANNER.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || !DustSensor.has(player)) {
            return;
        }
        DustField field = DustField.of(mc.level);
        if (field == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        Matrix4f pose = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drewAny = false;
        for (DustCloud cloud : field.cloudsAround(cam, RANGE)) {
            if (cloud.surfaceDistance(cam) > RANGE) {
                continue;
            }
            CloudShape shape = shape(field, cloud);
            if (shape == null) {
                continue;
            }
            int rgb = DustKinds.color(mc.level.registryAccess(), cloud.kind());
            drawShell(buffers.getBuffer(RenderType.debugQuads()), pose, shape, cam, rgb);
            drawWires(buffers.getBuffer(RenderType.lines()), poseStack.last(), shape, cam, rgb);
            drewAny = true;
        }
        if (drewAny) {
            // A scanner overlay, not scenery: draw it unfogged (deep space fog is black), then restore the fog.
            float fogStart = RenderSystem.getShaderFogStart();
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            buffers.endBatch(RenderType.debugQuads());
            buffers.endBatch(RenderType.lines());
            RenderSystem.setShaderFogStart(fogStart);
        }
    }

    /** The cached shape, or null while it is still being computed. */
    private static CloudShape shape(DustField field, DustCloud cloud) {
        Key key = new Key(field.seed(), cloud);
        CompletableFuture<CloudShape> future = SHAPES.get(key);
        if (future == null) {
            if (SHAPES.size() >= MAX_CACHED) {
                SHAPES.clear();
            }
            future = CompletableFuture.supplyAsync(() -> CloudShape.compute(field, cloud), Util.backgroundExecutor());
            SHAPES.put(key, future);
        }
        return future.getNow(null);
    }

    private static void drawWires(VertexConsumer lines, PoseStack.Pose pose, CloudShape shape, Vec3 cam, int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        for (int lat = 0; lat <= CloudShape.LAT_BANDS; lat++) {
            for (int lon = 0; lon < CloudShape.LON_SEGMENTS; lon++) {
                if (lat > 0 && lat < CloudShape.LAT_BANDS) {
                    line(lines, pose, shape.vertex(lat, lon), shape.vertex(lat, lon + 1), cam, r, g, b);
                }
                if (lat < CloudShape.LAT_BANDS && lon % 2 == 0) {
                    line(lines, pose, shape.vertex(lat, lon), shape.vertex(lat + 1, lon), cam, r, g, b);
                }
            }
        }
    }

    private static void line(VertexConsumer lines, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 cam, float r, float g, float b) {
        Vec3 normal = to.subtract(from).normalize();
        lines.addVertex(pose, (float) (from.x - cam.x), (float) (from.y - cam.y), (float) (from.z - cam.z))
                .setColor(r, g, b, LINE_ALPHA).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        lines.addVertex(pose, (float) (to.x - cam.x), (float) (to.y - cam.y), (float) (to.z - cam.z))
                .setColor(r, g, b, LINE_ALPHA).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void drawShell(VertexConsumer quads, Matrix4f pose, CloudShape shape, Vec3 cam, int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f, g = (rgb >> 8 & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        for (int lat = 0; lat < CloudShape.LAT_BANDS; lat++) {
            for (int lon = 0; lon < CloudShape.LON_SEGMENTS; lon++) {
                quadVertex(quads, pose, shape.vertex(lat, lon), cam, r, g, b);
                quadVertex(quads, pose, shape.vertex(lat + 1, lon), cam, r, g, b);
                quadVertex(quads, pose, shape.vertex(lat + 1, lon + 1), cam, r, g, b);
                quadVertex(quads, pose, shape.vertex(lat, lon + 1), cam, r, g, b);
            }
        }
    }

    private static void quadVertex(VertexConsumer quads, Matrix4f pose, Vec3 p, Vec3 cam, float r, float g, float b) {
        quads.addVertex(pose, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).setColor(r, g, b, SHELL_ALPHA);
    }
}
