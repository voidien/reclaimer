package com.voidreclaimer.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.assembly.AssemblyCamera;
import com.voidreclaimer.client.render.body.BodyBounds;
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
public final class AssemblyScreen extends Screen {
    private static final int GUTTER = 6;
    private static final int TOP = 20;
    private static final int TREE_WIDTH = 112;
    private static final int DETAILS_WIDTH = 150;
    /** Room under the tree for its "n empty" line. */
    private static final int TREE_FOOTER = 56;
    private static final int PANEL = 0xE0101418;
    private static final int VIEW = 0xC00C1014;
    private static final int ACCENT = 0xFF7FD4FF;
    /** How much of the view the body fills at zoom 1. */
    private static final float FIT = 0.42f;
    private static final float START_YAW = 215f;
    private static final float START_PITCH = -18f;

    private final AssemblyCamera camera = new AssemblyCamera(START_YAW, START_PITCH);
    private BodyPanel panel;
    private int viewLeft;
    private int viewRight;
    private int viewTop;
    private int viewBottom;
    private boolean dragging;
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
        panel = new BodyPanel(font, GUTTER, TOP, new BodyPanel.Layout(TREE_WIDTH, rows, rightX, DETAILS_WIDTH));
        addRenderableWidget(Button.builder(Component.translatable("assembly.void_reclaimer.back"), b -> onClose())
                .bounds(width - GUTTER - 50, 3, 50, 14).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, PANEL);
        graphics.fill(viewLeft, viewTop, viewRight, viewBottom, VIEW);
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
        renderBody(graphics, player);
        panel.render(graphics, player, mouseX, mouseY);
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
        graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        InventoryScreen.renderEntityInInventory(graphics, (viewLeft + viewRight) / 2f, (viewTop + viewBottom) / 2f, pixelsPerBlock(player),
                translate, pose, turn, player);
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
            return true;
        }
        LocalPlayer player = minecraft.player;
        return button == 0 && player != null && panel.mouseClicked(player, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            camera.drag(dragX, dragY);
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
