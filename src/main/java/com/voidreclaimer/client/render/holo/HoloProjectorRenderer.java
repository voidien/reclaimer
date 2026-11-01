package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.voidreclaimer.client.render.body.BodyRenderer;
import com.voidreclaimer.display.HoloDisplay;
import com.voidreclaimer.display.HoloGlobes;
import com.voidreclaimer.display.HoloProjectorBlock;
import com.voidreclaimer.display.HoloProjectorBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * Draws what a Holo Projector shows, in the air above the block: a faint panel, the text on it and a soft beam down to the
 * block. Everything goes into the frame's shared buffers (no endBatch, no Tesselator), and at full brightness, so a hologram
 * reads the same in the dark of space. Only drawn through the normal block entity pass; {@link HoloTransform} keeps a
 * billboard facing the viewer on a moving ship.
 */
public final class HoloProjectorRenderer implements BlockEntityRenderer<HoloProjectorBlockEntity> {
    private static final int VIEW_DISTANCE = 48;
    /** Above this size the picture may stick out of the block's render section, so the section must not cull it. */
    private static final float OFFSCREEN_SIZE = 2f;
    /** Half the width of the beam at the picture, in blocks. */
    private static final float BEAM_WIDTH = 0.35f;
    /** The most room a picture can need around the block: a full panel at the biggest scale. */
    private static final float MAX_REACH = 12f;
    /** Room between a globe and the lines over it, as a share of the globe's size: it must clear the Earth's atmosphere. */
    private static final float CAPTION_GAP = 0.12f;

    private final Font font;

    public HoloProjectorRenderer(BlockEntityRendererProvider.Context context) {
        font = context.getFont();
    }

    @Override
    public void render(HoloProjectorBlockEntity projector, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        HoloDisplay display = projector.display();
        if (projector.getLevel() == null) {
            return;
        }
        float time = projector.getLevel().getGameTime() + partialTick;
        float flicker = HoloStyle.flicker(time, projector.getBlockPos().asLong());
        switch (display.mode()) {
            case TEXT, READOUT -> {
                if (!display.lines().isEmpty()) {
                    drawText(projector, display, time, flicker, pose, buffers);
                }
            }
            case MODEL -> drawModel(projector, display, time, flicker, partialTick, pose, buffers);
            case BODY -> drawBody(display, time, flicker, partialTick, pose, buffers);
            case GLOBE -> drawGlobe(projector, display, time, flicker, pose, buffers);
        }
    }

    private void drawModel(HoloProjectorBlockEntity projector, HoloDisplay display, float time, float flicker, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        ResourceLocation id = ResourceLocation.tryParse(display.subject());
        BakedGeoModel baked = id == null ? null : HoloModels.baked(id);
        if (baked == null) {
            drawText(projector, display.withLines(List.of("NO MODEL", display.subject())), time, flicker, pose, buffers);
            return;
        }
        float size = HoloTransform.MODEL_SIZE * display.scale();
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + size / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(HoloTransform.spin(time)));
        HoloModels.draw(pose, buffers, id, baked, size, HoloStyle.MODEL_ALPHA * flicker, partialTick);
        pose.popPose();
    }

    private void drawBody(HoloDisplay display, float time, float flicker, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer == null) {
            return;
        }
        float size = HoloTransform.MODEL_SIZE * display.scale();
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + size / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(HoloTransform.spin(time)));
        BodyRenderer.drawHologram(viewer, size, HoloStyle.MODEL_ALPHA * flicker, partialTick, pose, buffers);
        pose.popPose();
    }

    private void drawGlobe(HoloProjectorBlockEntity projector, HoloDisplay display, float time, float flicker, PoseStack pose, MultiBufferSource buffers) {
        float size = HoloTransform.MODEL_SIZE * display.scale();
        Quaternionf camera = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + size / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(HoloTransform.spin(time)));
        Quaternionf parent = pose.last().pose().getNormalizedRotation(new Quaternionf());
        if (HoloGlobes.EARTH.equals(display.subject())) {
            HoloGlobeRenderer.earth(pose, buffers, display.progress(), size / 2, flicker);
        } else {
            HoloGlobeRenderer.dyson(pose, buffers, display.progress(), size / 2, flicker, time, HoloTransform.billboard(parent, camera));
        }
        pose.popPose();
        drawText(projector, display.withLines(HoloGlobes.caption(display.subject(), display.progress())).withHeight(display.height() + size * (1 + CAPTION_GAP)),
                time, flicker, pose, buffers);
    }

    private void drawText(HoloProjectorBlockEntity projector, HoloDisplay display, float time, float flicker, PoseStack pose, MultiBufferSource buffers) {
        HoloTextLayout layout = display == projector.display()
                ? HoloCaches.layout(projector, entry -> HoloTextLayout.of(entry.display(), font::width))
                : HoloTextLayout.of(display, font::width);
        float pixel = HoloTransform.PIXEL * display.scale();
        float halfHeight = HoloTransform.size(layout.height(), display.scale()) / 2;

        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        beam(pose, buffers, display.height(), flicker);
        pose.translate(0, display.height() + halfHeight, 0);
        pose.mulPose(turn(projector, display, pose));
        pose.scale(pixel, -pixel, pixel);
        pose.translate(-layout.width() / 2f, -layout.height() / 2f, 0);
        panel(pose, buffers, layout, HoloStyle.scroll(time), flicker);
        text(pose, buffers, layout, flicker);
        pose.popPose();
    }

    private static Quaternionf turn(HoloProjectorBlockEntity projector, HoloDisplay display, PoseStack pose) {
        if (!display.billboard()) {
            return HoloTransform.fixed(projector.getBlockState().getValue(HoloProjectorBlock.FACING));
        }
        Quaternionf parent = pose.last().pose().getNormalizedRotation(new Quaternionf());
        return HoloTransform.billboard(parent, Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
    }

    private static void beam(PoseStack pose, MultiBufferSource buffers, float height, float flicker) {
        if (height <= 0) {
            return;
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.GLOW, false));
        int color = HoloStyle.argb(HoloStyle.CYAN, HoloStyle.GLOW_ALPHA * flicker);
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(45 + 90 * side));
            Matrix4f matrix = pose.last().pose();
            vertex(glow, pose, matrix, -BEAM_WIDTH, height, 0, 0, 0, color);
            vertex(glow, pose, matrix, BEAM_WIDTH, height, 0, 1, 0, color);
            vertex(glow, pose, matrix, BEAM_WIDTH / 3, 0, 0, 1, 1, color);
            vertex(glow, pose, matrix, -BEAM_WIDTH / 3, 0, 0, 0, 1, color);
            pose.popPose();
        }
    }

    private static void panel(PoseStack pose, MultiBufferSource buffers, HoloTextLayout layout, float scroll, float flicker) {
        VertexConsumer panel = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.PANEL, false));
        int color = HoloStyle.argb(HoloStyle.CYAN, HoloStyle.PANEL_ALPHA * flicker);
        Matrix4f matrix = pose.last().pose();
        float w = layout.width();
        float h = layout.height();
        float u = w / HoloStyle.SCANLINE_PIXELS;
        float v = h / HoloStyle.SCANLINE_PIXELS;
        vertex(panel, pose, matrix, 0, h, 0, 0, v + scroll, color);
        vertex(panel, pose, matrix, w, h, 0, u, v + scroll, color);
        vertex(panel, pose, matrix, w, 0, 0, u, scroll, color);
        vertex(panel, pose, matrix, 0, 0, 0, 0, scroll, color);
    }

    private void text(PoseStack pose, MultiBufferSource buffers, HoloTextLayout layout, float flicker) {
        Matrix4f matrix = pose.last().pose();
        for (HoloTextLayout.Row row : layout.rows()) {
            font.drawInBatch(row.text(), row.x(), row.y(), HoloStyle.argb(row.color(), flicker), false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            font.drawInBatch(row.text(), row.x(), row.y(), HoloStyle.argb(row.color(), HoloStyle.SEE_THROUGH_ALPHA), false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack pose, Matrix4f matrix, float x, float y, float z, float u, float v, int argb) {
        buffer.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
    }

    @Override
    public boolean shouldRenderOffScreen(HoloProjectorBlockEntity projector) {
        return projector.display().scale() > OFFSCREEN_SIZE;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(HoloProjectorBlockEntity projector) {
        return new AABB(projector.getBlockPos()).inflate(MAX_REACH);
    }
}
