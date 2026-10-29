package com.voidreclaimer.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.voidreclaimer.dyson.DysonSphereData;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Dyson Sphere as seen from deep space, drawn around the sun in the sun's own frame (the sun sits at +Y).
 * Programmer art: coloured quads, no textures.
 *
 * <ul>
 *   <li>Swarm: points orbiting the star on tilted bands, more and brighter with every satellite (tiers 1-2).</li>
 *   <li>Shell: a geodesic-style lattice with welding sparks (tier 3) that fills in to a dark shell with a corona
 *       and glowing equatorial conduits (tier 4).</li>
 * </ul>
 * The power beams to the Quantum Receiver Dish wait for that block.
 */
final class DysonSky {
    private static final float RADIUS = 99f;
    private static final int MAX_SWARM_POINTS = 700;
    private static final int BANDS = 6;
    private static final float SWARM_ANGULAR_SPEED = 0.012f;
    private static final long SWARM_SEED = 0xD750_5A3AL;
    private static final int LATTICE_RINGS = 4;
    private static final int LATTICE_SPOKES = 12;
    private static final int SEGMENTS = 36;
    /** Angular radius (radians) of the shell around the star. */
    private static final float SHELL_RADIUS = 0.24f;

    private DysonSky() {}

    /** Tint for the star's own disc: amber as the swarm thickens, dimmer as the shell closes. */
    static float[] sunTint(DysonSphereData data) {
        float sunlight = data.sunlight();
        float amber = data.tier() >= 2 ? 0.25f : data.swarmDensity() * 0.25f;
        return new float[]{1f, 1f - amber * 0.35f, 1f - amber, sunlight};
    }

    /** Draws everything with alpha blending already on; {@code frame} puts the star at +Y. */
    static void render(Matrix4f frame, DysonSphereData data, float time) {
        if (data.tier() <= 0) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // The quads are built from tangent vectors, so their winding depends on the view: draw both faces.
        RenderSystem.disableCull();
        drawSwarm(frame, data, time);
        if (data.shellProgress() > 0f || data.tier() >= 3) {
            drawShell(frame, data, time);
        }
        RenderSystem.enableCull();
    }

    private static void drawSwarm(Matrix4f frame, DysonSphereData data, float time) {
        float density = Math.max(data.swarmDensity(), data.satellites() > 0 ? 0.04f : 0f);
        int points = Math.max(1, (int) (MAX_SWARM_POINTS * density));
        // Tier 1 is "faint"; the full swarm is bright. Once the shell closes the swarm is hidden behind it.
        float alpha = (0.25f + 0.65f * density) * (1f - data.shellProgress());
        if (alpha <= 0.01f) {
            return;
        }
        BufferBuilder builder = begin();
        RandomSource random = RandomSource.create(SWARM_SEED);
        for (int i = 0; i < points; i++) {
            int band = i % BANDS;
            float radius = 0.16f + 0.05f * band + random.nextFloat() * 0.02f;
            // Inclined orbits around the star: ellipses of different flatness and roll, all centred on it.
            float squash = 0.3f + 0.7f * (band % 3) / 2f;
            float roll = band * 0.55f;
            float phase = random.nextFloat() * Mth.TWO_PI;
            float direction = band % 2 == 0 ? 1f : -0.7f;
            float angle = phase + time * SWARM_ANGULAR_SPEED * direction * (1.2f - radius);
            Vector3f center = orbitPoint(radius, angle, squash, roll);
            float size = 0.35f + random.nextFloat() * 0.35f;
            // Pale blue-white, slightly cyan once dense.
            quad(builder, frame, center, size, 0.7f, 0.9f + 0.1f * density, 1f, alpha);
        }
        draw(builder);
    }

    private static void drawShell(Matrix4f frame, DysonSphereData data, float time) {
        float progress = data.shellProgress();
        boolean complete = data.tier() >= 4;
        float lattice = complete ? 1f : 0.45f + 0.55f * progress;

        BufferBuilder builder = begin();
        // Dark shell panels grow with progress and cover the star when complete.
        if (progress > 0f) {
            float cover = complete ? 1f : progress * progress;
            disc(builder, frame, SHELL_RADIUS, 0.05f, 0.045f, 0.04f, 0.9f * cover);
        }
        // Brass-and-silicon lattice: rings and spokes of thin bars.
        float bar = complete ? 0.5f : 0.22f;
        for (int ring = 1; ring <= LATTICE_RINGS; ring++) {
            float radius = SHELL_RADIUS * ring / LATTICE_RINGS;
            for (int s = 0; s < SEGMENTS; s++) {
                float a = Mth.TWO_PI * s / SEGMENTS;
                Vector3f center = onSphere(radius, a, 0f);
                quad(builder, frame, center, bar, 0.42f, 0.33f, 0.2f, lattice);
            }
        }
        for (int spoke = 0; spoke < LATTICE_SPOKES; spoke++) {
            float a = Mth.TWO_PI * spoke / LATTICE_SPOKES;
            for (int step = 1; step < LATTICE_RINGS * 3; step++) {
                Vector3f center = onSphere(SHELL_RADIUS * step / (LATTICE_RINGS * 3f), a, 0f);
                quad(builder, frame, center, bar * 0.8f, 0.36f, 0.3f, 0.22f, lattice);
            }
        }
        draw(builder);

        // Everything below is additive: sparks, conduits, corona.
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        builder = begin();
        if (!complete) {
            // Orange welding sparks that flicker along the lattice.
            RandomSource random = RandomSource.create(SWARM_SEED + (long) (time / 6f));
            for (int i = 0; i < 10; i++) {
                float radius = SHELL_RADIUS * (0.2f + 0.8f * random.nextFloat());
                Vector3f center = onSphere(radius, random.nextFloat() * Mth.TWO_PI, 0f);
                quad(builder, frame, center, 0.5f, 1f, 0.55f, 0.15f, 0.9f * random.nextFloat());
            }
        } else {
            // Glowing equatorial conduits, and the corona leaking past the edge.
            for (int band = -1; band <= 1; band++) {
                for (int s = 0; s < SEGMENTS * 2; s++) {
                    float a = Mth.TWO_PI * s / (SEGMENTS * 2);
                    Vector3f center = onSphere(SHELL_RADIUS * (0.55f + 0.18f * band), a, 0f);
                    float pulse = 0.5f + 0.5f * Mth.sin(a * 6f - time * 0.15f);
                    quad(builder, frame, center, 0.3f, 1f, 0.6f, 0.2f, 0.25f + 0.5f * pulse);
                }
            }
            for (int s = 0; s < SEGMENTS * 2; s++) {
                float a = Mth.TWO_PI * s / (SEGMENTS * 2);
                float flicker = 0.6f + 0.4f * Mth.sin(a * 5f + time * 0.08f);
                quad(builder, frame, onSphere(SHELL_RADIUS * 1.04f, a, 0f), 1.1f, 1f, 0.8f, 0.45f, 0.35f * flicker);
            }
        }
        draw(builder);
        RenderSystem.defaultBlendFunc();
    }

    /** A point on an inclined orbit around the star: the circle squashed along one axis, then rolled about the star. */
    private static Vector3f orbitPoint(float radius, float angle, float squash, float roll) {
        float x = radius * Mth.cos(angle);
        float z = radius * Mth.sin(angle) * squash;
        float xr = x * Mth.cos(roll) - z * Mth.sin(roll);
        float zr = x * Mth.sin(roll) + z * Mth.cos(roll);
        return onSphere((float) Math.hypot(xr, zr), (float) Math.atan2(zr, xr), 0f);
    }

    /** A point on the sky sphere at angular distance {@code radius} from the star, going round it by {@code angle}. */
    private static Vector3f onSphere(float radius, float angle, float tilt) {
        float sin = Mth.sin(radius);
        Vector3f p = new Vector3f(sin * Mth.cos(angle), Mth.cos(radius), sin * Mth.sin(angle));
        // Tilt the band about the X axis.
        float c = Mth.cos(tilt);
        float s = Mth.sin(tilt);
        return new Vector3f(p.x, p.y * c - p.z * s, p.y * s + p.z * c).mul(RADIUS);
    }

    private static void disc(BufferBuilder builder, Matrix4f frame, float angularRadius, float r, float g, float b, float a) {
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            float a0 = Mth.TWO_PI * i / steps;
            float a1 = Mth.TWO_PI * (i + 1) / steps;
            Vector3f p0 = onSphere(angularRadius, a0, 0f);
            Vector3f p1 = onSphere(angularRadius, a1, 0f);
            Vector3f c = new Vector3f(0f, RADIUS, 0f);
            // A fan of quads that degenerate to triangles at the centre.
            builder.addVertex(frame, c.x, c.y, c.z).setColor(r, g, b, a);
            builder.addVertex(frame, p0.x, p0.y, p0.z).setColor(r, g, b, a);
            builder.addVertex(frame, p1.x, p1.y, p1.z).setColor(r, g, b, a);
            builder.addVertex(frame, c.x, c.y, c.z).setColor(r, g, b, a);
        }
    }

    /** A small quad facing the viewer at the middle of the sky sphere. */
    private static void quad(BufferBuilder builder, Matrix4f frame, Vector3f center, float size, float r, float g, float b, float a) {
        Vector3f normal = new Vector3f(center).normalize();
        Vector3f u = normal.cross(0f, 0f, 1f, new Vector3f());
        if (u.lengthSquared() < 1.0E-4f) {
            u.set(1f, 0f, 0f);
        }
        u.normalize(size);
        Vector3f v = new Vector3f(normal).cross(u).normalize(size);
        corner(builder, frame, center, u, v, -1, -1, r, g, b, a);
        corner(builder, frame, center, u, v, 1, -1, r, g, b, a);
        corner(builder, frame, center, u, v, 1, 1, r, g, b, a);
        corner(builder, frame, center, u, v, -1, 1, r, g, b, a);
    }

    private static void corner(BufferBuilder builder, Matrix4f frame, Vector3f center, Vector3f u, Vector3f v, float su, float sv,
                               float r, float g, float b, float a) {
        builder.addVertex(frame, center.x + u.x * su + v.x * sv, center.y + u.y * su + v.y * sv, center.z + u.z * su + v.z * sv)
                .setColor(r, g, b, a);
    }

    private static BufferBuilder begin() {
        return com.mojang.blaze3d.vertex.Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void draw(BufferBuilder builder) {
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }
}
