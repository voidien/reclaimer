package com.voidreclaimer.client.render.holo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/**
 * A recording of vertices: positions and texture coordinates, all a hologram needs. Baked once away from the render thread,
 * then replayed into the frame's buffer with one tint. Vertices past {@link #MAX_VERTICES} are dropped whole quads at a time,
 * so a huge ship loses its far blocks rather than costing the frame.
 */
public final class HoloMesh implements VertexConsumer {
    public static final int MAX_VERTICES = 60_000;
    private static final int STRIDE = 5;

    private final FloatArrayList data = new FloatArrayList();
    private final float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    private boolean full;

    public int vertices() {
        return data.size() / STRIDE;
    }

    /** The box round everything recorded: min x, y, z, then max x, y, z. */
    public float[] box() {
        return box;
    }

    /** Adds a vertex through the same calls a block model makes; for tests and for code that builds a mesh by hand. */
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // Only ever stop at a quad's start, or the replay would join up the wrong corners.
        if (vertices() % 4 == 0 && vertices() >= MAX_VERTICES) {
            full = true;
        }
        if (!full) {
            data.add(x);
            data.add(y);
            data.add(z);
            data.add(0f);
            data.add(0f);
            box[0] = Math.min(box[0], x);
            box[1] = Math.min(box[1], y);
            box[2] = Math.min(box[2], z);
            box[3] = Math.max(box[3], x);
            box[4] = Math.max(box[4], y);
            box[5] = Math.max(box[5], z);
        }
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        if (!full) {
            data.set(data.size() - 2, u);
            data.set(data.size() - 1, v);
        }
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }

    /** Draws the recording with one colour, 0xAARRGGBB. */
    public void replay(PoseStack.Pose pose, VertexConsumer out, int argb) {
        Matrix4f matrix = pose.pose();
        float[] values = data.elements();
        int size = data.size();
        for (int i = 0; i < size; i += STRIDE) {
            out.addVertex(matrix, values[i], values[i + 1], values[i + 2]).setColor(argb).setUv(values[i + 3], values[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
        }
    }
}
