package com.voidreclaimer.client.camera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.client.hud.ScreenProjection;
import com.voidreclaimer.client.render.body.BoneProbe;
import com.voidreclaimer.debug.DebugReadout;
import com.voidreclaimer.debug.DebugShapes;
import com.voidreclaimer.debug.ShipOverlay;
import com.voidreclaimer.debug.DebugTime;
import com.voidreclaimer.debug.LogisticsOverlay;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * Photo mode's debug layer, toggled with G: hit boxes and chunk borders in the world, a crosshair, an outline on what the camera
 * looks at, and {@link DebugReadout}'s facts in a panel. Facts are read on the integrated server, so they are the real state and
 * not the client's copy. Every photo-mode screenshot also saves the full facts in a text file beside the picture.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class PhotoDebug {
    private static final int REFRESH_TICKS = 5;
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL = 0xB0101418;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int HEADING = 0xFF7FD4FF;
    /** A long data line in the panel; the text file always has all of it. */
    private static final int PANEL_LINE_CHARS = 120;
    private static final double OVERLAY_RANGE = 256.0;
    private static final float LABEL_SCALE = 0.5f;

    private static boolean on;
    private static boolean labels;
    private static boolean overlay;
    private static volatile DebugShapes shapes = new DebugShapes();
    private static boolean timeChanged;
    private static boolean chunkBorders;
    private static boolean hitBoxes;
    private static int age;
    private static volatile List<String> facts = List.of();
    private static volatile DebugReadout.Target target = DebugReadout.Target.NONE;

    private PhotoDebug() {}

    public static boolean on() {
        return on;
    }

    public static boolean labels() {
        return labels;
    }

    /** The body's socket and mount names over the model (L in photo mode); works with or without the rest of the layer. */
    public static void setLabels(boolean value) {
        labels = value;
        BoneProbe.capture(value);
    }

    public static boolean overlay() {
        return overlay;
    }

    /** Lines and labels in the world (O in photo mode): {@link ShipOverlay} and {@link LogisticsOverlay}. */
    public static void setOverlay(boolean value) {
        overlay = value;
        shapes = new DebugShapes();
        age = 0;
    }

    /** What photo mode's time keys ask for. */
    public enum Time { TOGGLE_FREEZE, STEP, TOGGLE_SLOW, RESET }

    /** Changes time on the integrated server. Leaving photo mode always puts it back. */
    public static void time(Minecraft mc, Time action) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        timeChanged |= action != Time.RESET;
        server.execute(() -> {
            switch (action) {
                case TOGGLE_FREEZE -> DebugTime.setFrozen(server, !server.tickRateManager().isFrozen());
                case STEP -> DebugTime.step(server, 1);
                case TOGGLE_SLOW -> DebugTime.setSlow(server, server.tickRateManager().tickrate() == DebugTime.NORMAL_RATE);
                case RESET -> DebugTime.reset(server);
            }
        });
    }

    /** Called when photo mode ends: only touches time if photo mode changed it, so a player's own {@code /tick} settings stay. */
    static void resetTime(Minecraft mc) {
        if (timeChanged) {
            timeChanged = false;
            time(mc, Time.RESET);
        }
    }

    /** Turns the layer on or off with photo mode's G key. */
    public static void toggle(Minecraft mc) {
        set(mc, !on);
    }

    /** Called when photo mode ends too, so hit boxes and chunk borders never outlive it. */
    public static void set(Minecraft mc, boolean value) {
        if (on == value) {
            return;
        }
        on = value;
        var dispatcher = mc.getEntityRenderDispatcher();
        if (value) {
            hitBoxes = dispatcher.shouldRenderHitBoxes();
            dispatcher.setRenderHitBoxes(true);
            chunkBorders = mc.debugRenderer.switchRenderChunkborder();
            if (!chunkBorders) {
                chunkBorders = mc.debugRenderer.switchRenderChunkborder();
            }
            age = 0;
        } else {
            dispatcher.setRenderHitBoxes(hitBoxes);
            if (chunkBorders) {
                mc.debugRenderer.switchRenderChunkborder();
            }
        }
    }

    private static Vec3 look() {
        return new Vec3(lookVector());
    }

    private static Vector3f lookVector() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
    }

    /** The facts as seen by a camera, read on the server thread. */
    private static java.util.concurrent.CompletableFuture<List<String>> read(Minecraft mc, Vec3 from, Vec3 look) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.level == null || mc.player == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(List.of("Facts need the integrated server"));
        }
        var dimension = mc.level.dimension();
        UUID id = mc.player.getUUID();
        return server.submit(() -> {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                return List.of("No server level");
            }
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            target = DebugReadout.target(level, from, look);
            List<String> lines = new ArrayList<>(DebugReadout.lines(level, from, look, player));
            if (overlay) {
                DebugShapes built = new DebugShapes();
                ShipOverlay.add(level, from, OVERLAY_RANGE, built, lines);
                LogisticsOverlay.add(level, from, OVERLAY_RANGE, built, lines);
                shapes = built;
            }
            String time = DebugTime.describe(server);
            if (!time.isEmpty()) {
                lines.add(0, time);
            }
            return lines;
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(on || overlay) || !PhotoMode.active() || age++ % REFRESH_TICKS != 0) {
            return;
        }
        read(mc, PhotoMode.position(), look()).thenAccept(lines -> facts = lines);
    }

    /** The outline on what the camera looks at. */
    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (!PhotoMode.active() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        if (overlay) {
            renderShapes(event);
        }
        if (!on) {
            return;
        }
        DebugReadout.Target now = target;
        AABB box = now.pos() != null ? new AABB(now.pos()) : now.entity() != null ? now.entity().getBoundingBox() : null;
        if (box == null) {
            return;
        }
        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box.inflate(0.01), 1f, 0.85f, 0.2f, 1f);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static void renderShapes(RenderLevelStageEvent event) {
        Camera camera = event.getCamera();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var buffer = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose last = pose.last();
        for (DebugShapes.Line line : shapes.lines()) {
            Vec3 way = line.to().subtract(line.from()).normalize();
            buffer.addVertex(last, (float) line.from().x, (float) line.from().y, (float) line.from().z).setColor(line.colour())
                    .setNormal(last, (float) way.x, (float) way.y, (float) way.z);
            buffer.addVertex(last, (float) line.to().x, (float) line.to().y, (float) line.to().z).setColor(line.colour())
                    .setNormal(last, (float) way.x, (float) way.y, (float) way.z);
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    /** The overlay's labels, over the picture. */
    public static void renderShapeTexts(GuiGraphics graphics, Minecraft mc, int width, int height) {
        Camera camera = mc.gameRenderer.getMainCamera();
        for (DebugShapes.Text text : shapes.texts()) {
            var screen = ScreenProjection.project(camera, text.at().subtract(camera.getPosition()), PhotoMode.state().fov(), width, height, 0, 0);
            if (screen.onScreen()) {
                graphics.pose().pushPose();
                graphics.pose().translate(screen.x(), screen.y(), 0);
                graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1f);
                graphics.drawString(mc.font, text.text(), 0, 0, text.colour(), true);
                graphics.pose().popPose();
            }
        }
    }

    /** The camera's own line, from the client. */
    private static String cameraLine(Minecraft mc) {
        PhotoState state = PhotoMode.state();
        Vec3 at = state.position();
        return String.format(Locale.ROOT, "Camera %.1f %.1f %.1f, yaw %.1f pitch %.1f roll %.1f, fov %.0f, %d fps. Body drawn at %.2f, parts %.2f",
                at.x, at.y, at.z, state.yaw(), state.pitch(), state.roll(), state.fov(), mc.getFps(),
                com.voidreclaimer.client.render.body.BodyRenderer.drawnBodyScale(), com.voidreclaimer.client.render.body.BodyRenderer.drawnPartScale());
    }

    /** A dot and a small name at each socket and mount, bright when filled. Sockets are blue, mounts gold. */
    public static void renderLabels(GuiGraphics graphics, Minecraft mc, int width, int height) {
        Camera camera = mc.gameRenderer.getMainCamera();
        for (BoneProbe.Point point : BoneProbe.latest()) {
            Vec3 direction = new Vec3(point.at());
            var screen = ScreenProjection.project(camera, direction, PhotoMode.state().fov(), width, height, 0, 0);
            if (!screen.onScreen()) {
                continue;
            }
            int colour = point.kind() == BoneProbe.Kind.SOCKET ? 0xFF7FD4FF : 0xFFFFC857;
            colour = point.filled() ? colour : (colour & 0x00FFFFFF) | 0x90000000;
            graphics.fill(screen.x() - 1, screen.y() - 1, screen.x() + 1, screen.y() + 1, colour);
            graphics.pose().pushPose();
            graphics.pose().translate(screen.x() + 2, screen.y() - 2, 0);
            graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1f);
            graphics.drawString(mc.font, point.name(), 0, 0, colour, true);
            graphics.pose().popPose();
        }
    }

    /** The points as text, in blocks from the body's feet along the body's own axes, for the text file. */
    private static List<String> pointLines(Minecraft mc) {
        Vec3 fromBody = mc.gameRenderer.getMainCamera().getPosition().subtract(mc.player.position());
        double yaw = Math.toRadians(com.voidreclaimer.client.render.body.BodyRenderer.facing(mc.player));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        List<String> lines = new ArrayList<>();
        lines.add("Attachment points (blocks from the feet: right, up, forward; * means something is installed)");
        BoneProbe.latest().stream().sorted(java.util.Comparator.comparing(BoneProbe.Point::name)).forEach(point -> {
            Vec3 at = new Vec3(point.at()).add(fromBody);
            lines.add(String.format(Locale.ROOT, "  %s %-34s %6.2f %6.2f %6.2f%s", point.kind() == BoneProbe.Kind.SOCKET ? "socket" : "mount ",
                    point.name(), at.dot(right), at.y, at.dot(forward), point.filled() ? " *" : ""));
        });
        return lines;
    }

    /** The panel and the crosshair; photo mode's screen calls this. */
    public static void render(GuiGraphics graphics, Minecraft mc, int width, int height) {
        int cx = width / 2;
        int cy = height / 2;
        graphics.fill(cx - 4, cy, cx + 5, cy + 1, 0xFFFFFFFF);
        graphics.fill(cx, cy - 4, cx + 1, cy + 5, 0xFFFFFFFF);
        Font font = mc.font;
        int panelWidth = Math.min(PANEL_WIDTH, width / 2 - 8);
        List<FormattedCharSequence> rows = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        List<String> all = new ArrayList<>();
        all.add(cameraLine(mc));
        all.addAll(facts);
        for (String line : all) {
            String shown = line.length() > PANEL_LINE_CHARS ? line.substring(0, PANEL_LINE_CHARS) + "..." : line;
            int colour = line.startsWith(" ") ? TEXT : HEADING;
            for (FormattedCharSequence row : font.split(Component.literal(shown), panelWidth - 6)) {
                rows.add(row);
                colours.add(colour);
            }
        }
        int visible = Math.min(rows.size(), (height - 8) / (font.lineHeight + 1));
        graphics.fill(2, 2, 2 + panelWidth, 6 + visible * (font.lineHeight + 1), PANEL);
        for (int i = 0; i < visible; i++) {
            graphics.drawString(font, rows.get(i), 5, 5 + i * (font.lineHeight + 1), colours.get(i), false);
        }
    }

    /**
     * Saves a screenshot of this frame under {@code screenshots/} and the full facts beside it as {@code <name>.txt}.
     *
     * @param name file name without an extension
     */
    public static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> mc.execute(() -> mc.gui.getChat().addMessage(message)));
        String camera = cameraLine(mc);
        List<String> points = labels ? pointLines(mc) : List.of();
        age = 0;
        read(mc, PhotoMode.position(), look()).thenAccept(lines -> {
            List<String> file = new ArrayList<>();
            file.add(camera);
            file.addAll(lines);
            file.addAll(points);
            Path path = mc.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve(name + ".txt");
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                VoidReclaimer.LOGGER.warn("Could not write {}: {}", path, e.toString());
            }
        });
    }
}
