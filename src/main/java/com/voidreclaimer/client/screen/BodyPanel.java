package com.voidreclaimer.client.screen;

import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.PartKind;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.network.BodyActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Replicator's Body tab: the body's slots by kind on the left, and on the right either the body's stats or, for
 * the selected slot, its part and the compatible parts in the inventory (click one to install it). Looks (casing,
 * accent tint, thruster trail) are picked along the bottom. Every change goes to the server, which validates it.
 */
final class BodyPanel {
    private static final int SLOT = 16;
    private static final int STEP = 17;
    private static final int ROW = 18;
    private static final int LABEL_WIDTH = 58;
    private static final int LEFT_WIDTH = 190;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int DIM = 0xFF7A8794;
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int WARN = 0xFFFF6060;
    private static final int[] ACCENTS = {0x7FD4FF, 0xF2C14E, 0xFFB347, 0x7CFC8A, 0x5EEAD4, 0xC792EA, 0xFF6B6B, 0xFFFFFF};

    private final Font font;
    private final int x;
    private final int y;
    private final int width;
    @Nullable
    private PartKind selectedKind;
    private int selectedIndex;

    BodyPanel(Font font, int x, int y, int width) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
    }

    private int rightX() {
        return x + LEFT_WIDTH + 8;
    }

    private int rightWidth() {
        return width - LEFT_WIDTH - 8;
    }

    private int cosmeticsY() {
        return y + PartKind.values().length * ROW + 4;
    }

    // ---- rendering ----------------------------------------------------------------------------------------

    void render(GuiGraphics graphics, LocalPlayer player, int mouseX, int mouseY) {
        BodyData body = Bodies.data(player);
        ItemStack hovered = ItemStack.EMPTY;
        Component hoverText = null;
        for (PartKind kind : PartKind.values()) {
            int rowY = y + kind.ordinal() * ROW;
            graphics.drawString(font, kind.displayName(), x + 2, rowY + 4, DIM, false);
            for (int i = 0; i < body.slots(kind); i++) {
                int sx = x + LABEL_WIDTH + i * STEP;
                boolean selected = kind == selectedKind && i == selectedIndex;
                boolean hover = inside(mouseX, mouseY, sx, rowY, SLOT, SLOT);
                graphics.fill(sx, rowY, sx + SLOT, rowY + SLOT, selected ? 0xFF2E4A5E : hover ? 0xFF2A3644 : 0xFF1E2630);
                graphics.renderOutline(sx, rowY, SLOT, SLOT, 0xFF3A4A5A);
                if (kind == PartKind.TOOL && i == body.activeTool()) {
                    graphics.renderOutline(sx - 1, rowY - 1, SLOT + 2, SLOT + 2, ACCENT);
                }
                ItemStack part = body.get(kind, i);
                if (!part.isEmpty()) {
                    graphics.renderItem(part, sx, rowY);
                    if (hover) {
                        hovered = part;
                    }
                }
            }
        }
        renderCosmetics(graphics, body.cosmetics(), mouseX, mouseY);
        if (selectedKind != null && selectedIndex < body.slots(selectedKind)) {
            Object[] tip = renderSlotDetails(graphics, player, body, mouseX, mouseY);
            if (tip[0] instanceof ItemStack stack && !stack.isEmpty()) {
                hovered = stack;
            }
            if (tip[1] instanceof Component text) {
                hoverText = text;
            }
        } else {
            selectedKind = null;
            renderStats(graphics, player, body.stats());
        }
        if (!hovered.isEmpty()) {
            List<Component> lines = new ArrayList<>(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                    net.minecraft.client.Minecraft.getInstance(), hovered));
            if (hoverText != null) {
                lines.add(hoverText);
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private void renderStats(GuiGraphics graphics, LocalPlayer player, BodyStats stats) {
        int tx = rightX() + 2;
        int ty = y + 2;
        graphics.drawString(font, Component.translatable("body.void_reclaimer.stats"), tx, ty, ACCENT, false);
        ty += 12;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.size", stats.size().displayName(),
                fmt(stats.size().scale())), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.mass", fmt(stats.mass()), fmt(stats.massBudget())),
                stats.overloaded() ? WARN : TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.flight",
                String.format(Locale.ROOT, "%.3f", stats.flight().acceleration()), fmt(stats.flight().maxSpeed() * 20)), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.energy", fmt(stats.energyCapacity()),
                fmt(stats.solarRate())), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.health", fmt(stats.maxHealth() / 2f),
                fmt(stats.reach())), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.dust", fmt(DustData.capacity(player)),
                fmt(stats.dustIntake())), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.inventory", stats.inventorySlots()), TEXT);
        ty += 10;
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.tier", stats.coreTier()), TEXT);
        if (stats.overloaded()) {
            ty += 12;
            for (var seq : font.split(Component.translatable("body.void_reclaimer.overloaded"), rightWidth() - 4)) {
                graphics.drawString(font, seq, tx, ty, WARN, false);
                ty += 10;
            }
        }
    }

    /** Returns {hovered stack, extra hover line}. */
    private Object[] renderSlotDetails(GuiGraphics graphics, LocalPlayer player, BodyData body, int mouseX, int mouseY) {
        int tx = rightX() + 2;
        int ty = y + 2;
        graphics.drawString(font, selectedKind.displayName(), tx, ty, ACCENT, false);
        ty += 12;
        ItemStack installed = body.get(selectedKind, selectedIndex);
        ItemStack hovered = ItemStack.EMPTY;
        Component hoverText = null;
        if (installed.isEmpty()) {
            graphics.drawString(font, Component.translatable("body.void_reclaimer.empty_slot"), tx, ty, DIM, false);
        } else {
            graphics.drawString(font, installed.getHoverName(), tx, ty, TEXT, false);
            if (BodyRules.canRemove(body, selectedKind, selectedIndex) == BodyRules.Check.OK) {
                boolean hover = inside(mouseX, mouseY, tx, ty + 11, font.width(removeLabel()) + 4, 11);
                graphics.drawString(font, removeLabel(), tx, ty + 12, hover ? WARN : DIM, false);
            }
        }
        ty += 28;
        graphics.drawString(font, Component.translatable("body.void_reclaimer.install"), tx, ty, DIM, false);
        ty += 11;
        List<Integer> candidates = candidates(player, selectedKind);
        if (candidates.isEmpty()) {
            for (var seq : font.split(Component.translatable("body.void_reclaimer.no_parts"), rightWidth() - 4)) {
                graphics.drawString(font, seq, tx, ty, DIM, false);
                ty += 10;
            }
        }
        int perRow = Math.max(1, (rightWidth() - 2) / STEP);
        for (int i = 0; i < candidates.size(); i++) {
            int sx = tx + (i % perRow) * STEP;
            int sy = ty + (i / perRow) * STEP;
            ItemStack stack = player.getInventory().items.get(candidates.get(i));
            BodyRules.Check check = BodyRules.canInstall(body, selectedKind, selectedIndex, stack);
            boolean hover = inside(mouseX, mouseY, sx, sy, SLOT, SLOT);
            graphics.fill(sx, sy, sx + SLOT, sy + SLOT, hover ? 0xFF2A3644 : 0xFF1E2630);
            graphics.renderItem(stack, sx, sy);
            if (check != BodyRules.Check.OK) {
                graphics.fill(sx, sy, sx + SLOT, sy + SLOT, 0xA0101418);
            }
            if (hover) {
                hovered = stack;
                hoverText = check == BodyRules.Check.OK
                        ? Component.translatable("body.void_reclaimer.click_install").withColor(ACCENT)
                        : Component.translatable(BodyRules.messageKey(check)).withColor(WARN);
            }
        }
        return new Object[] {hovered, hoverText};
    }

    private void renderCosmetics(GuiGraphics graphics, Cosmetics cosmetics, int mouseX, int mouseY) {
        int cy = cosmeticsY();
        Component casing = Component.translatable("body.void_reclaimer.casing", cosmetics.casing().displayName());
        Component trail = Component.translatable("body.void_reclaimer.trail", cosmetics.trail().displayName());
        int casingWidth = font.width(casing) + 6;
        button(graphics, x, cy, casingWidth, casing, mouseX, mouseY);
        button(graphics, x + casingWidth + 4, cy, font.width(trail) + 6, trail, mouseX, mouseY);
        int sy = cy + 14;
        graphics.drawString(font, Component.translatable("body.void_reclaimer.accent"), x + 2, sy + 2, DIM, false);
        int sx = x + LABEL_WIDTH;
        for (int i = 0; i < ACCENTS.length; i++) {
            int bx = sx + i * 12;
            graphics.fill(bx, sy, bx + 10, sy + 10, 0xFF000000 | ACCENTS[i]);
            if (cosmetics.accent() == ACCENTS[i]) {
                graphics.renderOutline(bx - 1, sy - 1, 12, 12, 0xFFFFFFFF);
            }
        }
    }

    private void button(GuiGraphics graphics, int bx, int by, int bw, Component label, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, bx, by, bw, 12);
        graphics.fill(bx, by, bx + bw, by + 12, hover ? 0xFF2E4A5E : 0xFF1E2630);
        graphics.drawString(font, label, bx + 3, by + 2, TEXT, false);
    }

    private static void line(GuiGraphics graphics, int tx, int ty, Component text, int color) {
        graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, text, tx, ty, color, false);
    }

    private static Component removeLabel() {
        return Component.translatable("body.void_reclaimer.remove");
    }

    // ---- input --------------------------------------------------------------------------------------------

    boolean mouseClicked(LocalPlayer player, double mouseX, double mouseY) {
        BodyData body = Bodies.data(player);
        for (PartKind kind : PartKind.values()) {
            int rowY = y + kind.ordinal() * ROW;
            for (int i = 0; i < body.slots(kind); i++) {
                if (inside(mouseX, mouseY, x + LABEL_WIDTH + i * STEP, rowY, SLOT, SLOT)) {
                    boolean same = kind == selectedKind && i == selectedIndex;
                    selectedKind = same ? null : kind;
                    selectedIndex = i;
                    return true;
                }
            }
        }
        Cosmetics cosmetics = body.cosmetics();
        int cy = cosmeticsY();
        Component casing = Component.translatable("body.void_reclaimer.casing", cosmetics.casing().displayName());
        int casingWidth = font.width(casing) + 6;
        Component trail = Component.translatable("body.void_reclaimer.trail", cosmetics.trail().displayName());
        if (inside(mouseX, mouseY, x, cy, casingWidth, 12)) {
            PacketDistributor.sendToServer(BodyActionPayload.cosmetics(cosmetics.withCasing(cosmetics.casing().next())));
            return true;
        }
        if (inside(mouseX, mouseY, x + casingWidth + 4, cy, font.width(trail) + 6, 12)) {
            PacketDistributor.sendToServer(BodyActionPayload.cosmetics(cosmetics.withTrail(cosmetics.trail().next())));
            return true;
        }
        for (int i = 0; i < ACCENTS.length; i++) {
            if (inside(mouseX, mouseY, x + LABEL_WIDTH + i * 12, cy + 14, 10, 10)) {
                PacketDistributor.sendToServer(BodyActionPayload.cosmetics(cosmetics.withAccent(ACCENTS[i])));
                return true;
            }
        }
        if (selectedKind == null) {
            return false;
        }
        int tx = rightX() + 2;
        int ty = y + 14;
        if (!body.get(selectedKind, selectedIndex).isEmpty() && inside(mouseX, mouseY, tx, ty + 11, font.width(removeLabel()) + 4, 11)) {
            PacketDistributor.sendToServer(BodyActionPayload.remove(selectedKind, selectedIndex));
            return true;
        }
        ty += 39;
        List<Integer> candidates = candidates(player, selectedKind);
        int perRow = Math.max(1, (rightWidth() - 2) / STEP);
        for (int i = 0; i < candidates.size(); i++) {
            if (inside(mouseX, mouseY, tx + (i % perRow) * STEP, ty + (i / perRow) * STEP, SLOT, SLOT)) {
                PacketDistributor.sendToServer(BodyActionPayload.install(selectedKind, selectedIndex, candidates.get(i)));
                return true;
            }
        }
        return false;
    }

    /** Inventory slots holding parts of {@code kind}. */
    private static List<Integer> candidates(LocalPlayer player, PartKind kind) {
        List<Integer> slots = new ArrayList<>();
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (Parts.isPart(items.get(i), kind)) {
                slots.add(i);
            }
        }
        return slots;
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
