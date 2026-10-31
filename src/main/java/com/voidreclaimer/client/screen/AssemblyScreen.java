package com.voidreclaimer.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.assembly.AssemblyCamera;
import com.voidreclaimer.body.assembly.StatDelta;
import com.voidreclaimer.client.render.body.BodyBounds;
import com.voidreclaimer.client.render.body.BodyRenderer;
import com.voidreclaimer.client.render.body.BoneProbe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * The full-window assembly view: the body's model in the centre, turned by dragging and zoomed by scrolling, with the Body
 * tab's socket tree on the left and its details and parts on the right ({@link BodyPanel}). Like the Replicator, it does not
 * pause the game. Closing it goes back to the Body tab.
 */
public final class AssemblyScreen extends Screen implements com.voidreclaimer.client.debug.GuiDebug.Regions {
    private static final int GUTTER = 6;
    private static final int TOP = 20;
    private static final int TREE_WIDTH = 112;
    private static final int DETAILS_WIDTH = 150;
    /** Room under the tree for its "n empty" line. */
    private static final int TREE_FOOTER = 56;
    private static final int PANEL = 0xFF101418;
    private static final int VIEW = 0xFF0A1420;
    private static final int GRID = 0x224C7FA8;
    private static final int GRID_MAJOR = 0x554C7FA8;
    private static final int GRID_TEXT = 0xFF4C7FA8;
    /** Grid lines stay at least this far apart, in GUI pixels. */
    private static final int GRID_MIN_PIXELS = 12;
    private static final int GRID_MAJOR_EVERY = 4;
    private static final int ACCENT = 0xFF7FD4FF;
    /** How much of the view the body fills at zoom 1. */
    private static final float FIT = 0.42f;
    private static final float START_YAW = 35f;
    /** {@code renderEntityInInventory} draws the body this far up the GUI's depth. */
    private static final float Z_BASE = 50f;
    /** How far behind the body's middle a marker still shows, as a share of its radius. */
    private static final float FAR_SHARE = 0.15f;
    private static final double PICK_RADIUS = 6.0;
    private static final int MARKER_SOCKET = 0xFF7FD4FF;
    private static final int MARKER_MOUNT = 0xFFFFC857;
    private static final int CHANGES_BACK = 0xD0101418;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int BETTER = 0xFF70E080;
    private static final int WORSE = 0xFFFF7060;
    private static final float START_PITCH = -18f;

    private final AssemblyCamera camera = new AssemblyCamera(START_YAW, START_PITCH);
    private BodyPanel panel;
    private int viewLeft;
    private int viewRight;
    private int viewTop;
    private int viewBottom;
    private boolean dragging;
    /** The mouse travelled since the press: a drag, not a click on a marker. */
    private boolean moved;
    private List<BoneProbe.Point> markers = List.of();
    /** How the body was drawn this frame, to turn a point on it back into the body's own space. */
    private float drawnScale = 1f;
    private final Quaternionf drawnPose = new Quaternionf();
    private final Vector3f drawnShift = new Vector3f();
    private long lastFrame;

    public AssemblyScreen() {
        super(Component.translatable("assembly.void_reclaimer.title"));
    }

    @Override
    protected void init() {
        int rightX = width - GUTTER - DETAILS_WIDTH;
        viewLeft = GUTTER * 2 + TREE_WIDTH;
        viewRight = rightX - GUTTER;
        viewTop = TOP;
        viewBottom = height - GUTTER;
        int rows = Math.max(BodyPanel.TREE_ROWS, (height - TOP - TREE_FOOTER) / BodyPanel.treeRowHeight());
        panel = new BodyPanel(font, GUTTER, TOP, new BodyPanel.Layout(TREE_WIDTH, rows, rightX, DETAILS_WIDTH, height - GUTTER));
        addRenderableWidget(Button.builder(Component.translatable("assembly.void_reclaimer.back"), b -> onClose())
                .bounds(width - GUTTER - 50, 3, 50, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("assembly.void_reclaimer.explode"), b -> camera.toggleExploded())
                .bounds(viewLeft + 3, viewTop + 3, 52, 13).build());
        BoneProbe.capture(true);
    }

    @Override
    public void removed() {
        BoneProbe.capture(false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PANEL);
        graphics.fill(viewLeft, viewTop, viewRight, viewBottom, VIEW);
        LocalPlayer player = minecraft.player;
        if (player != null) {
            renderGrid(graphics, pixelsPerBlock(player));
        }
    }

    /** The blueprint grid: squares of a whole number of blocks round the view's centre, every fourth line stronger. */
    private void renderGrid(GuiGraphics graphics, float pixelsPerBlock) {
        int step = AssemblyCamera.gridStep(pixelsPerBlock, GRID_MIN_PIXELS);
        float spacing = step * pixelsPerBlock;
        int cx = (viewLeft + viewRight) / 2;
        int cy = (viewTop + viewBottom) / 2;
        int columns = (int) ((viewRight - viewLeft) / 2 / spacing) + 1;
        int rows = (int) ((viewBottom - viewTop) / 2 / spacing) + 1;
        for (int i = -columns; i <= columns; i++) {
            int x = cx + Math.round(i * spacing);
            if (x >= viewLeft && x < viewRight) {
                graphics.fill(x, viewTop, x + 1, viewBottom, i % GRID_MAJOR_EVERY == 0 ? GRID_MAJOR : GRID);
            }
        }
        for (int i = -rows; i <= rows; i++) {
            int y = cy + Math.round(i * spacing);
            if (y >= viewTop && y < viewBottom) {
                graphics.fill(viewLeft, y, viewRight, y + 1, i % GRID_MAJOR_EVERY == 0 ? GRID_MAJOR : GRID);
            }
        }
        graphics.drawString(font, Component.translatable(step == 1 ? "assembly.void_reclaimer.grid_one" : "assembly.void_reclaimer.grid", step), viewLeft + 3, viewBottom - 11, GRID_TEXT, false);
    }

    @Override
    public java.util.List<com.voidreclaimer.debug.GuiLayout.Rect> debugRegions() {
        return panel.debugRegions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        long now = Util.getMillis();
        camera.advance(lastFrame == 0L ? 0f : (now - lastFrame) / 1000f);
        lastFrame = now;
        graphics.drawString(font, title, GUTTER, 6, ACCENT, false);
        // What the panel drew under the mouse last frame; the panel comes after the model, over it.
        BodyView current = Bodies.view(player);
        BodyPanel.Preview hovered = panel.preview();
        BodyView preview = hovered == null ? null : previewOf(current, hovered, player);
        BodyRenderer.setPreview(preview, hovered == null ? null : hovered.socket(), hovered == null ? null : hovered.mount());
        renderBody(graphics, player);
        BodyRenderer.setPreview(null, null, null);
        renderMarkers(graphics, player, mouseX, mouseY);
        if (preview != null) {
            renderChanges(graphics, hovered.stack().getHoverName(), StatDelta.between(current.stats(), preview.stats()));
        }
        panel.render(graphics, player, mouseX, mouseY);
    }

    private static BodyView previewOf(BodyView view, BodyPanel.Preview part, LocalPlayer player) {
        return part.mount() != null ? Bodies.withModule(view, part.mount(), part.stack(), player.registryAccess())
                : Bodies.withSection(view, part.socket(), part.stack(), player.registryAccess());
    }

    /** The stats a hovered part would change, in the view's bottom right: green is better, red worse. */
    private void renderChanges(GuiGraphics graphics, Component part, List<StatDelta.Row> rows) {
        List<Component> labels = new ArrayList<>();
        List<String> values = new ArrayList<>();
        int labelWidth = 0;
        int valueWidth = 0;
        for (StatDelta.Row row : rows) {
            Component label = Component.translatable("assembly.void_reclaimer.stat." + row.key());
            String value = number(row.before()) + " > " + number(row.after());
            labels.add(label);
            values.add(value);
            labelWidth = Math.max(labelWidth, font.width(label));
            valueWidth = Math.max(valueWidth, font.width(value));
        }
        int boxWidth = Math.max(labelWidth + valueWidth + 12, font.width(part) + 6);
        int x = viewRight - boxWidth - 3;
        int y = viewBottom - (rows.size() + 1) * 10 - 7;
        graphics.fill(x, y, viewRight - 3, y + (rows.size() + 1) * 10 + 4, CHANGES_BACK);
        graphics.drawString(font, part, x + 3, y + 3, ACCENT, false);
        for (int i = 0; i < rows.size(); i++) {
            int rowY = y + 13 + i * 10;
            graphics.drawString(font, labels.get(i), x + 3, rowY, TEXT, false);
            graphics.drawString(font, values.get(i), x + labelWidth + 9, rowY, rows.get(i).better() ? BETTER : WORSE, false);
        }
    }

    private static String number(double value) {
        double abs = Math.abs(value);
        return String.format(Locale.ROOT, abs >= 100 || value == Math.rint(value) ? "%.0f" : abs >= 1 ? "%.1f" : "%.3f", value);
    }

    /** Where a marker counts as on the far side: just behind the body's middle, so the edges of a thin body stay reachable. */
    private float farLimit(LocalPlayer player) {
        return Z_BASE - FAR_SHARE * BodyBounds.radius(Bodies.view(player)) * player.getScale() * drawnScale;
    }

    private void renderMarkers(GuiGraphics graphics, LocalPlayer player, int mouseX, int mouseY) {
        String area = panel.area();
        var chassis = Bodies.view(player).chassis();
        markers = AssemblyMarkers.visible(BoneProbe.latest(), panel.selectedSocket(), socket -> {
            var found = chassis.socket(socket);
            return area == null || (found != null && found.area().equals(area));
        }, farLimit(player));
        BoneProbe.Point over = overView(mouseX, mouseY) ? AssemblyMarkers.pick(markers, mouseX, mouseY, PICK_RADIUS).orElse(null) : null;
        graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        for (BoneProbe.Point point : markers) {
            int x = Math.round(point.at().x());
            int y = Math.round(point.at().y());
            boolean selected = point.mount() == null ? point.socket().equals(panel.selectedSocket()) && panel.selectedMount() == null
                    : point.mount().equals(panel.selectedMount());
            int colour = point.kind() == BoneProbe.Kind.SOCKET ? MARKER_SOCKET : MARKER_MOUNT;
            graphics.renderOutline(x - 3, y - 3, 7, 7, selected || point == over ? 0xFFFFFFFF : colour);
            if (point.filled()) {
                graphics.fill(x - 1, y - 1, x + 2, y + 2, colour);
            }
        }
        graphics.disableScissor();
        if (over != null) {
            Component name = over.mount() == null ? BodyPanel.socketName(over.socket())
                    : BodyPanel.socketName(over.socket()).copy().append(" / ").append(over.mount().kind().displayName());
            graphics.renderTooltip(font, name, mouseX, mouseY);
        }
    }

    /** Selects a marker's socket or mount and eases the view to it. */
    private void choose(LocalPlayer player, BoneProbe.Point point) {
        panel.select(point.socket(), point.mount());
        // Undo the draw: scale, the shift to the view's centre, then the turn, leaving the point in the body's own space.
        Vector3f local = new Vector3f((point.at().x() - (viewLeft + viewRight) / 2f) / drawnScale,
                (point.at().y() - (viewTop + viewBottom) / 2f) / drawnScale, -(point.at().z() - Z_BASE) / drawnScale)
                .sub(drawnShift);
        drawnPose.transformInverse(local);
        camera.focusOn(local.x(), local.y() - player.getBbHeight() / 2f, local.z());
    }

    /** Pixels per block at the current zoom, so the body fills the view at zoom 1 whatever its size. */
    private float pixelsPerBlock(LocalPlayer player) {
        float radius = Math.max(0.1f, BodyBounds.radius(Bodies.view(player)) * player.getScale());
        return Math.min(viewRight - viewLeft, viewBottom - viewTop) * FIT / radius * camera.zoom();
    }

    /** Draws the body facing the camera, with the camera's angles and focus, clipped to the view. */
    private void renderBody(GuiGraphics graphics, LocalPlayer player) {
        float yBodyRot = player.yBodyRot;
        float yBodyRotO = player.yBodyRotO;
        float yRot = player.getYRot();
        float yRotO = player.yRotO;
        float xRot = player.getXRot();
        float xRotO = player.xRotO;
        float yHeadRot = player.yHeadRot;
        float yHeadRotO = player.yHeadRotO;
        // Set like the inventory's preview: the body stands still here while the camera turns.
        player.yBodyRot = 180f;
        player.yBodyRotO = 180f;
        player.setYRot(180f);
        player.yRotO = 180f;
        player.setXRot(0f);
        player.xRotO = 0f;
        player.yHeadRot = 180f;
        player.yHeadRotO = 180f;

        Quaternionf turn = new Quaternionf().rotateX((float) Math.toRadians(camera.pitch())).rotateY((float) Math.toRadians(camera.yaw()));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(turn);
        // The point of the body at the view's centre: the middle of its hitbox, plus the focus.
        Vector3f centre = new Vector3f(camera.focusX(), player.getBbHeight() / 2f + camera.focusY(), camera.focusZ());
        Vector3f translate = pose.transform(centre, new Vector3f()).negate();
        drawnScale = pixelsPerBlock(player);
        drawnPose.set(pose);
        drawnShift.set(translate);
        graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        BodyRenderer.setExplode(camera.explode());
        InventoryScreen.renderEntityInInventory(graphics, (viewLeft + viewRight) / 2f, (viewTop + viewBottom) / 2f, drawnScale,
                translate, pose, turn, player);
        BodyRenderer.setExplode(0f);
        graphics.disableScissor();
        // A big body reaches far towards the viewer: nothing drawn later may end up behind it.
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        Lighting.setupFor3DItems();

        player.yBodyRot = yBodyRot;
        player.yBodyRotO = yBodyRotO;
        player.setYRot(yRot);
        player.yRotO = yRotO;
        player.setXRot(xRot);
        player.xRotO = xRotO;
        player.yHeadRot = yHeadRot;
        player.yHeadRotO = yHeadRotO;
    }

    private boolean overView(double mouseX, double mouseY) {
        return mouseX >= viewLeft && mouseX < viewRight && mouseY >= viewTop && mouseY < viewBottom;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && overView(mouseX, mouseY)) {
            dragging = true;
            moved = false;
            return true;
        }
        LocalPlayer player = minecraft.player;
        return button == 0 && player != null && panel.mouseClicked(player, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        LocalPlayer player = minecraft.player;
        if (dragging && !moved && player != null) {
            var picked = AssemblyMarkers.pick(markers, mouseX, mouseY, PICK_RADIUS);
            if (picked.isPresent()) {
                choose(player, picked.get());
            } else {
                panel.clearSelection();
                camera.focusCentre();
            }
        }
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            camera.drag(dragX, dragY);
            moved |= Math.abs(dragX) + Math.abs(dragY) > 0.5;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overView(mouseX, mouseY)) {
            camera.scroll(scrollY);
            return true;
        }
        LocalPlayer player = minecraft.player;
        return player != null && mouseX < viewLeft && panel.mouseScrolled(player, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(new ReplicatorScreen(ReplicatorScreen.Start.BODY));
    }
}
