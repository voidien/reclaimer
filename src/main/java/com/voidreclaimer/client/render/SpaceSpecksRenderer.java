package com.voidreclaimer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.voidreclaimer.ClientConfig;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.space.ModDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Space specks: faint motes scattered on a fixed world-space lattice around the camera. They are the same everywhere
 * in deep space (they carry no information about dust clouds); their only job is parallax, so movement is visible
 * in empty space. At speed they stretch into streaks along the direction of travel.
 *
 * <p>Drawn after particles as plain translucent quads; it does not touch chunk rendering (Sodium-safe).
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class SpaceSpecksRenderer {
    private static final int CELL = 6;
    private static final double RADIUS = 30.0;
    private static final float BASE_PRESENCE = 0.7f;
    private static final float SPECK_SIZE = 0.05f;
    private static final float REST_ALPHA = 0.4f;
    private static final float MOVING_ALPHA = 0.95f;
    /** Speed (blocks/tick) at which specks reach full brightness and streaks are longest. */
    private static final double FULL_SPEED = 0.5;
    private static final double STREAK_MIN_SPEED = 0.12;
    /** Streak length in ticks of travel. */
    private static final double STREAK_TICKS = 5.0;

    private SpaceSpecksRenderer() {}

    @SubscribeEvent
    static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !ClientConfig.SPECKS_ENABLED.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !ModDimensions.isDeepSpace(mc.level)) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        Vec3 velocity = player.getDeltaMovement();
        double speed = velocity.length();
        float speedFactor = (float) Mth.clamp(speed / FULL_SPEED, 0.0, 1.0);
        float presence = BASE_PRESENCE * ClientConfig.SPECKS_DENSITY.get().floatValue();
        boolean streaks = speed > STREAK_MIN_SPEED;
        Vec3 streakVector = velocity.scale(STREAK_TICKS);

        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());

        PoseStack poseStack = event.getPoseStack();
        Matrix4f pose = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());

        int range = (int) Math.ceil(RADIUS / CELL);
        int baseX = Mth.floor(cam.x / CELL);
        int baseY = Mth.floor(cam.y / CELL);
        int baseZ = Mth.floor(cam.z / CELL);
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    int cx = baseX + dx;
                    int cy = baseY + dy;
                    int cz = baseZ + dz;
                    long hash = hash(cx, cy, cz);
                    if (unit(hash, 0) >= presence) {
                        continue;
                    }
                    double rx = (cx + unit(hash, 1)) * CELL - cam.x;
                    double ry = (cy + unit(hash, 2)) * CELL - cam.y;
                    double rz = (cz + unit(hash, 3)) * CELL - cam.z;
                    double distance = Math.sqrt(rx * rx + ry * ry + rz * rz);
                    if (distance > RADIUS || distance < 0.5) {
                        continue;
                    }
                    float fade = (float) (1.0 - distance / RADIUS);
                    float alpha = fade * Mth.lerp(speedFactor, REST_ALPHA, MOVING_ALPHA) * (0.6f + 0.4f * unit(hash, 4));
                    float size = SPECK_SIZE * (0.7f + 0.6f * unit(hash, 5));
                    if (streaks) {
                        drawStreak(consumer, pose, rx, ry, rz, streakVector, size * 1.4f, alpha);
                    } else {
                        drawDot(consumer, pose, (float) rx, (float) ry, (float) rz, left, up, size, alpha);
                    }
                }
            }
        }
        buffers.endBatch(RenderType.debugQuads());
    }

    private static void drawDot(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                                Vector3f left, Vector3f up, float size, float alpha) {
        float lx = left.x * size, ly = left.y * size, lz = left.z * size;
        float ux = up.x * size, uy = up.y * size, uz = up.z * size;
        vertex(consumer, pose, x - lx - ux, y - ly - uy, z - lz - uz, alpha);
        vertex(consumer, pose, x - lx + ux, y - ly + uy, z - lz + uz, alpha);
        vertex(consumer, pose, x + lx + ux, y + ly + uy, z + lz + uz, alpha);
        vertex(consumer, pose, x + lx - ux, y + ly - uy, z + lz - uz, alpha);
    }

    /**
     * A thin quad from the speck back along its apparent motion. Relative to the camera a speck moves by -velocity
     * each tick, so its recent positions lie toward +velocity.
     */
    private static void drawStreak(VertexConsumer consumer, Matrix4f pose, double x, double y, double z,
                                   Vec3 streak, float width, float alpha) {
        Vec3 head = new Vec3(x, y, z);
        Vec3 tail = head.add(streak);
        Vec3 axis = streak.normalize();
        Vec3 side = axis.cross(head).normalize().scale(width);
        if (!Double.isFinite(side.x)) {
            return;
        }
        float tailAlpha = alpha * 0.25f;
        vertex(consumer, pose, (float) (head.x - side.x), (float) (head.y - side.y), (float) (head.z - side.z), alpha);
        vertex(consumer, pose, (float) (head.x + side.x), (float) (head.y + side.y), (float) (head.z + side.z), alpha);
        vertex(consumer, pose, (float) (tail.x + side.x), (float) (tail.y + side.y), (float) (tail.z + side.z), tailAlpha);
        vertex(consumer, pose, (float) (tail.x - side.x), (float) (tail.y - side.y), (float) (tail.z - side.z), tailAlpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(0.80f, 0.88f, 1.0f, alpha);
    }

    private static long hash(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        return h ^ (h >>> 33);
    }

    /** A value in [0, 1) from byte lane {@code lane} of the hash. */
    private static float unit(long hash, int lane) {
        long mixed = hash * (0x9E3779B97F4A7C15L + lane * 0x632BE59BD9B4E019L);
        mixed ^= mixed >>> 29;
        return ((mixed >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
