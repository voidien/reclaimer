package com.voidreclaimer.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.voidreclaimer.body.assembly.AssemblyCamera;
import com.voidreclaimer.client.render.body.BodyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Draws the player's body in a box of a screen, standing still while a camera turns round it. */
final class BodyStage {
    /** How the body was drawn, for whatever has to find points on it. */
    record Drawn(Quaternionf pose, Vector3f shift) {
    }

    private BodyStage() {}

    /** The body in the box, with {@code camera}'s angles and focus, at {@code pixelsPerBlock}, clipped to the box. */
    static Drawn draw(GuiGraphics graphics, LocalPlayer player, AssemblyCamera camera, int left, int top, int right, int bottom, float pixelsPerBlock) {
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
        // The point of the body at the box's centre: the middle of its hitbox, plus the focus.
        Vector3f centre = new Vector3f(camera.focusX(), player.getBbHeight() / 2f + camera.focusY(), camera.focusZ());
        Vector3f shift = pose.transform(centre, new Vector3f()).negate();
        // What was drawn behind the body wrote depth at z 0, which would cut off anything far behind the focus.
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        graphics.enableScissor(left, top, right, bottom);
        BodyRenderer.setExplode(camera.explode());
        InventoryScreen.renderEntityInInventory(graphics, (left + right) / 2f, (top + bottom) / 2f, pixelsPerBlock, shift, pose, turn, player);
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
        return new Drawn(pose, shift);
    }
}
