package com.voidreclaimer.client.screen;

import com.voidreclaimer.body.BodyInventoryMenu;
import com.voidreclaimer.client.input.ModKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The body's inventory (E): the slots the body has, in a panel like the Replicator's with a tab strip that leads to the
 * Replicator, Body and Logs tabs. Rows the body has not unlocked are darkened. R opens the Replicator.
 */
public class BodyInventoryScreen extends AbstractContainerScreen<BodyInventoryMenu> {
    static final int WIDTH = 320;
    static final int HEIGHT = 244;
    private static final int PANEL = 0xE0101418;
    private static final int PANEL_LIGHT = 0xFF1E2630;
    private static final int SLOT = 0xFF0C1014;
    private static final int LOCKED = 0xCC05070A;
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int DIM = 0xFF7A8794;

    public BodyInventoryScreen(BodyInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int tabWidth = 50;
        int x = leftPos + 6;
        Button inventory = addRenderableWidget(Button.builder(Component.translatable("menu.void_reclaimer.inventory"), b -> {})
                .bounds(x, topPos + 18, tabWidth + 6, 16).build());
        inventory.active = false;
        x += tabWidth + 8;
        x = tab(x, tabWidth, "replicator.void_reclaimer.tab.replicator", ReplicatorScreen.Start.RECIPES);
        x = tab(x, tabWidth, "replicator.void_reclaimer.tab.body", ReplicatorScreen.Start.BODY);
        tab(x, tabWidth, "replicator.void_reclaimer.tab.logs", ReplicatorScreen.Start.LOGS);
    }

    private int tab(int x, int width, String key, ReplicatorScreen.Start start) {
        addRenderableWidget(Button.builder(Component.translatable(key), b -> minecraft.setScreen(new ReplicatorScreen(start)))
                .bounds(x, topPos + 18, width, 16).build());
        return x + width + 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, PANEL);
        graphics.renderOutline(leftPos, topPos, WIDTH, HEIGHT, 0xFF3A4A5A);
        graphics.fill(leftPos + 6, topPos + 38, leftPos + 6 + 9 * 18 + 4, topPos + HEIGHT - 6, PANEL_LIGHT);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("menu.void_reclaimer.inventory"), BodyInventoryMenu.GRID_X, 40 - 1 + 0, ACCENT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Slot slot : menu.slots) {
            if (menu.isLocked(slot)) {
                graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, LOCKED);
            }
        }
        Slot hovered = hoveredSlot;
        if (hovered != null && !hovered.hasItem() && menu.isLocked(hovered)) {
            graphics.renderTooltip(font, Component.translatable("menu.void_reclaimer.inventory.locked"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.REPLICATOR.matches(keyCode, scanCode)) {
            minecraft.setScreen(new ReplicatorScreen(ReplicatorScreen.Start.RECIPES));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
