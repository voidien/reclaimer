package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.display.HoloGlobes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Draws the Dyson and Earth globes into the frame's shared buffers. The pose is at the globe's middle, one unit is one block. */
final class HoloGlobeRenderer {
    private static final float STAR_RADIUS = 0.35f;
    private static final float SATELLITE_SIZE = 0.035f;
    private static final float EARTH_ALPHA = 0.75f;
    private static final float AIR_RADIUS = 1.06f;
    private static final float AIR_ALPHA = 0.12f;
    private static final float LATTICE_ALPHA = 0.08f;
    private static final float PANEL_ALPHA = 0.5f;
    private static final float STAR_ALPHA = 0.9f;

    private HoloGlobeRenderer() {}

    static void earth(PoseStack pose, MultiBufferSource buffers, HoloGlobes.Progress progress, float radius, float flicker) {
        VertexConsumer panel = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.PANEL, false));
        Matrix4f matrix = pose.last().pose();
        for (SphereMesh.Cell cell : SphereMesh.of(GlobeLook.EARTH_ROWS, GlobeLook.EARTH_COLS)) {
            int sector = GlobeLook.sector(cell.row(), cell.col());
            cell(panel, pose, matrix, cell, radius, HoloStyle.argb(GlobeLook.earthColor(sector, progress.isRestored(sector)), EARTH_ALPHA * flicker));
        }
        int air = HoloStyle.argb(GlobeLook.atmosphere(progress.era()), AIR_ALPHA * flicker);
        for (SphereMesh.Cell cell : SphereMesh.of(GlobeLook.EARTH_ROWS, GlobeLook.EARTH_COLS)) {
            cell(panel, pose, matrix, cell, radius * AIR_RADIUS, air);
        }
    }

    static void dyson(PoseStack pose, MultiBufferSource buffers, HoloGlobes.Progress progress, float radius, float flicker, float time, Quaternionf facing) {
        VertexConsumer panel = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.PANEL, false));
        Matrix4f matrix = pose.last().pose();
        int star = HoloStyle.argb(GlobeLook.star(), STAR_ALPHA * flicker);
        for (SphereMesh.Cell cell : SphereMesh.of(8, 16)) {
            cell(panel, pose, matrix, cell, radius * STAR_RADIUS, star);
        }
        if (progress.tier() >= 3 || progress.shell() > 0) {
            int lattice = HoloStyle.argb(GlobeLook.shell(), LATTICE_ALPHA * flicker);
            int standing = HoloStyle.argb(GlobeLook.shell(), PANEL_ALPHA * flicker);
            for (SphereMesh.Cell cell : SphereMesh.of(GlobeLook.SHELL_ROWS, GlobeLook.SHELL_COLS)) {
                cell(panel, pose, matrix, cell, radius * 0.8f, GlobeLook.panelStands(cell.row(), cell.col(), progress.shell()) ? standing : lattice);
            }
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(HoloStyle.GLOW, false));
        int dot = HoloStyle.argb(HoloStyle.CYAN, flicker);
        Vector3f right = facing.transform(new Vector3f(SATELLITE_SIZE, 0, 0));
        Vector3f up = facing.transform(new Vector3f(0, SATELLITE_SIZE, 0));
        int shown = GlobeLook.satellitesShown(progress.satellites());
        for (int i = 0; i < shown; i++) {
            float[] at = GlobeLook.satellite(i, time);
            float x = at[0] * radius * 0.8f;
            float y = at[1] * radius * 0.8f;
            float z = at[2] * radius * 0.8f;
            vertex(glow, pose, matrix, x - right.x - up.x, y - right.y - up.y, z - right.z - up.z, 0, 1, dot);
            vertex(glow, pose, matrix, x + right.x - up.x, y + right.y - up.y, z + right.z - up.z, 1, 1, dot);
            vertex(glow, pose, matrix, x + right.x + up.x, y + right.y + up.y, z + right.z + up.z, 1, 0, dot);
            vertex(glow, pose, matrix, x - right.x + up.x, y - right.y + up.y, z - right.z + up.z, 0, 0, dot);
        }
    }

    private static void cell(VertexConsumer buffer, PoseStack pose, Matrix4f matrix, SphereMesh.Cell cell, float radius, int color) {
        float[] c = cell.corners();
        float v = cell.row() / HoloStyle.SCANLINE_PIXELS;
        float nextV = (cell.row() + 1) / HoloStyle.SCANLINE_PIXELS;
        vertex(buffer, pose, matrix, c[0] * radius, c[1] * radius, c[2] * radius, 0, v, color);
        vertex(buffer, pose, matrix, c[3] * radius, c[4] * radius, c[5] * radius, 0, nextV, color);
        vertex(buffer, pose, matrix, c[6] * radius, c[7] * radius, c[8] * radius, 1, nextV, color);
        vertex(buffer, pose, matrix, c[9] * radius, c[10] * radius, c[11] * radius, 1, v, color);
    }

    private static void vertex(VertexConsumer buffer, PoseStack pose, Matrix4f matrix, float x, float y, float z, float u, float v, int argb) {
        buffer.addVertex(matrix, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
    }
}
