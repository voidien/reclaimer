package com.voidreclaimer.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.client.input.ModKeyMappings;
import com.voidreclaimer.network.BodyActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Radial module wheel: hold the wheel key, point the mouse at a tool slot, release to make it the active tool (or
 * click). Doesn't pause the game.
 */
public final class ModuleWheelScreen extends Screen {
    private static final int RADIUS = 52;
    private static final int DEAD_ZONE = 14;

    private int hovered = -1;

    public ModuleWheelScreen() {
        super(Component.translatable("screen.void_reclaimer.module_wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int slots(LocalPlayer player) {
        return Bodies.view(player).toolMounts().size();
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || slots(player) == 0) {
            onClose();
            return;
        }
        InputConstants.Key key = ModKeyMappings.MODULE_WHEEL.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM
                && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getValue())) {
            select();
        }
    }

    private void select() {
        if (hovered >= 0) {
            PacketDistributor.sendToServer(BodyActionPayload.selectTool(hovered));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        select();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        BodyView view = Bodies.view(player);
        java.util.List<BodyData.Mount> tools = view.toolMounts();
        int count = tools.size();
        int cx = width / 2;
        int cy = height / 2;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        hovered = -1;
        if (count > 0 && dx * dx + dy * dy > DEAD_ZONE * DEAD_ZONE) {
            double angle = Math.atan2(dy, dx) + Math.PI / 2 + Math.PI / count;
            hovered = Math.floorMod((int) Math.floor(angle / (Mth.TWO_PI / count)), count);
        }
        graphics.drawCenteredString(font, title, cx, cy - RADIUS - 30, 0xFF7FD4FF);
        for (int i = 0; i < count; i++) {
            double a = -Math.PI / 2 + i * Mth.TWO_PI / count;
            int ix = cx + (int) Math.round(Math.cos(a) * RADIUS);
            int iy = cy + (int) Math.round(Math.sin(a) * RADIUS);
            boolean active = i == view.data().activeTool();
            int bg = i == hovered ? 0xE02E4A5E : 0xC0101418;
            graphics.fill(ix - 13, iy - 13, ix + 13, iy + 13, bg);
            graphics.renderOutline(ix - 13, iy - 13, 26, 26, active ? 0xFF7FD4FF : 0xFF3A4A5A);
            ItemStack tool = view.data().module(tools.get(i));
            if (!tool.isEmpty()) {
                graphics.renderItem(tool, ix - 8, iy - 8);
            }
            if (i == hovered) {
                Component name = tool.isEmpty() ? Component.translatable("body.void_reclaimer.empty_mount", BodyPanel.socketName(tools.get(i).socket())) : tool.getHoverName();
                graphics.drawCenteredString(font, name, cx, cy + RADIUS + 22, 0xFFDDE6EE);
            }
        }
        graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible; no blur or dimming.
    }
}
