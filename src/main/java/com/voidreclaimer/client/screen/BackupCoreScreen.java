package com.voidreclaimer.client.screen;

import com.voidreclaimer.core.BackupCoreBlockEntity;
import com.voidreclaimer.core.CoreCharging;
import com.voidreclaimer.network.CoreActionPayload;
import com.voidreclaimer.registry.ModAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Backup Core UI: rename the core, see its charges and sunlight progress, and charge it instantly with dust. */
public final class BackupCoreScreen extends Screen {
    private static final int WIDTH = 220;
    private static final int HEIGHT = 120;
    private static final int PANEL = 0xE0101820;
    private static final int BORDER = 0xFF3A6A80;
    private static final int TEXT = 0xCFE8FF;
    private static final int DIM = 0x8899AA;
    private static final int ACCENT = 0x7FD4FF;

    private final BlockPos pos;
    private EditBox nameBox;
    private Button chargeButton;
    private int left;
    private int top;

    public BackupCoreScreen(BlockPos pos) {
        super(Component.translatable("block.void_reclaimer.backup_core"));
        this.pos = pos;
    }

    private BackupCoreBlockEntity core() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof BackupCoreBlockEntity core ? core : null;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        BackupCoreBlockEntity core = core();
        nameBox = addRenderableWidget(new EditBox(font, left + 10, top + 22, WIDTH - 80, 16, Component.translatable("screen.void_reclaimer.core.name")));
        nameBox.setMaxLength(BackupCoreBlockEntity.MAX_NAME_LENGTH);
        nameBox.setValue(core == null ? "" : core.name());
        addRenderableWidget(Button.builder(Component.translatable("screen.void_reclaimer.core.rename"), b -> rename())
                .bounds(left + WIDTH - 66, top + 20, 56, 20).build());
        chargeButton = addRenderableWidget(Button.builder(Component.translatable("screen.void_reclaimer.core.charge",
                        Math.round(CoreCharging.CHARGE_COST.metallic()), Math.round(CoreCharging.CHARGE_COST.silicate())),
                b -> PacketDistributor.sendToServer(CoreActionPayload.charge(pos)))
                .bounds(left + 10, top + HEIGHT - 28, 130, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH - 70, top + HEIGHT - 28, 60, 20).build());
    }

    private void rename() {
        BackupCoreBlockEntity core = core();
        String name = BackupCoreBlockEntity.cleanName(nameBox.getValue());
        if (core != null && !name.isEmpty() && !name.equals(core.name())) {
            PacketDistributor.sendToServer(CoreActionPayload.rename(pos, name));
        }
    }

    @Override
    public void tick() {
        BackupCoreBlockEntity core = core();
        if (core == null || minecraft.player == null || minecraft.player.distanceToSqr(pos.getCenter()) > 64.0) {
            onClose();
            return;
        }
        chargeButton.active = core.charges() < CoreCharging.MAX_CHARGES
                && CoreCharging.CHARGE_COST.canAfford(minecraft.player.getData(ModAttachments.DUST));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            rename();
            nameBox.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BackupCoreBlockEntity core = core();
        if (core == null) {
            return;
        }
        graphics.drawString(font, title, left + 10, top + 8, ACCENT, false);
        int y = top + 48;
        graphics.drawString(font, Component.translatable("screen.void_reclaimer.core.charges"), left + 10, y, TEXT, false);
        for (int i = 0; i < CoreCharging.MAX_CHARGES; i++) {
            int x = left + 70 + i * 14;
            graphics.fill(x, y, x + 10, y + 8, i < core.charges() ? 0xFF7FD4FF : 0xFF2A3440);
        }
        y += 16;
        graphics.drawString(font, Component.translatable("screen.void_reclaimer.core.sunlight"), left + 10, y, DIM, false);
        int barX = left + 70;
        graphics.fill(barX, y + 2, barX + 100, y + 6, 0xFF2A3440);
        int filled = Mth.clamp(Math.round(core.sunFraction() * 100), 0, 100);
        if (filled > 0) {
            graphics.fill(barX, y + 2, barX + filled, y + 6, 0xFFFFE08A);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, BORDER);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
