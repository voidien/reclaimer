package com.voidreclaimer.client.camera;

import com.mojang.blaze3d.platform.InputConstants;
import com.voidreclaimer.client.input.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Takes the input for photo mode while the camera is free. It is a screen only so the mouse and keys reach it instead of the body;
 * the game keeps running behind it. Drag with the left mouse button to look, WASD to move (Ctrl for fast), Space and Shift for up
 * and down, scroll for the field of view, Q and E to roll (R resets), G for the debug layer and L for the body's attachment labels ({@link PhotoDebug}), T for chat ({@link PhotoScript}'s commands), Enter or F2 for a screenshot, Escape or the photo-mode key to leave.
 */
public final class PhotoModeScreen extends Screen {
    private static final double SPEED = 0.35;
    private static final double FAST = 4.0;

    public PhotoModeScreen() {
        super(Component.translatable("screen.void_reclaimer.photo_mode"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (!PhotoMode.chatting()) {
            PhotoMode.exit(Minecraft.getInstance());
        }
    }

    private boolean down(int glfwKey) {
        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), glfwKey);
    }

    @Override
    public void tick() {
        if (minecraft.player == null) {
            onClose();
            return;
        }
        PhotoState state = PhotoMode.state();
        double forward = (down(GLFW.GLFW_KEY_W) ? 1 : 0) - (down(GLFW.GLFW_KEY_S) ? 1 : 0);
        double right = (down(GLFW.GLFW_KEY_D) ? 1 : 0) - (down(GLFW.GLFW_KEY_A) ? 1 : 0);
        double up = (down(GLFW.GLFW_KEY_SPACE) ? 1 : 0) - (down(GLFW.GLFW_KEY_LEFT_SHIFT) ? 1 : 0);
        double speed = down(GLFW.GLFW_KEY_LEFT_CONTROL) ? SPEED * FAST : SPEED;
        state.move(forward, right, up, speed, minecraft.player.getEyePosition());
        float roll = (down(GLFW.GLFW_KEY_E) ? 1f : 0f) - (down(GLFW.GLFW_KEY_Q) ? 1f : 0f);
        if (roll != 0f) {
            state.rollBy(roll * 1.5f);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            PhotoMode.state().look(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        PhotoMode.state().zoom(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.PHOTO_MODE.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            PhotoMode.state().resetRoll();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_T) {
            // On the next frame: opened now, chat would also receive this key's typed "t".
            minecraft.tell(() -> PhotoMode.openChat(minecraft));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_G) {
            PhotoDebug.toggle(minecraft);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_L) {
            PhotoDebug.setLabels(!PhotoDebug.labels());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_F2) {
            // The HUD is already hidden and this screen draws nothing without the debug layer, so the frame is clean.
            PhotoDebug.capture(minecraft, "photo_" + net.minecraft.Util.getFilenameFormattedDateTime());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Nothing else is drawn: the picture is the point. The controls are in the key binding's description.
        if (PhotoDebug.labels()) {
            PhotoDebug.renderLabels(graphics, minecraft, width, height);
        }
        if (PhotoDebug.on()) {
            PhotoDebug.render(graphics, minecraft, width, height);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
}
