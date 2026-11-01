package com.voidreclaimer.client.screen;

import com.voidreclaimer.link.ShellRow;
import com.voidreclaimer.network.PickShellPayload;
import com.voidreclaimer.network.RecoverShellPayload;
import com.voidreclaimer.network.ShellListPayload;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** The Uplink Seat's list of shells: one button each, greyed out when it cannot be reached. */
public final class ShellPickerScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW_HEIGHT = 22;
    private static final int ROWS_ON_A_PAGE = 8;
    private static final int TOP_MARGIN = 24;
    private static final int BOTTOM_MARGIN = 28;
    private static final int PANEL = 0xE0101820;
    private static final int BORDER = 0xFF3A6A80;
    private static final int TEXT = 0xCFE8FF;
    private static final double SEAT_DISTANCE_SQR = 64.0;

    private final BlockPos seat;
    private final List<ShellRow> rows;
    private int page;
    /** The shell whose recovery is waiting for a second click. */
    private UUID confirming;
    private int left;
    private int top;
    private int panelHeight;

    public ShellPickerScreen(ShellListPayload payload) {
        super(Component.translatable("screen.void_reclaimer.uplink.title"));
        seat = payload.seat();
        rows = List.copyOf(payload.rows());
    }

    @Override
    protected void init() {
        clearWidgets();
        int count = Math.min(ROWS_ON_A_PAGE, rows.size() - page * ROWS_ON_A_PAGE);
        panelHeight = TOP_MARGIN + count * ROW_HEIGHT + BOTTOM_MARGIN;
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        for (int i = 0; i < count; i++) {
            ShellRow row = rows.get(page * ROWS_ON_A_PAGE + i);
            Button button = Button.builder(label(row), b -> pick(row)).bounds(left + 10, top + TOP_MARGIN + i * ROW_HEIGHT, WIDTH - 20, 20).build();
            button.active = row.ready() || row.recoverable();
            addRenderableWidget(button);
        }
        int bottom = top + panelHeight - BOTTOM_MARGIN + 4;
        if (rows.size() > ROWS_ON_A_PAGE) {
            addRenderableWidget(Button.builder(Component.translatable("screen.void_reclaimer.uplink.next"), b -> nextPage())
                    .bounds(left + 10, bottom, 60, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + WIDTH - 70, bottom, 60, 20).build());
    }

    private Component label(ShellRow row) {
        Component where = row.sameDimension()
                ? Component.translatable("screen.void_reclaimer.uplink.here", row.number(), Math.round(row.distance()))
                : Component.translatable("screen.void_reclaimer.uplink.elsewhere", row.number(), row.dimension());
        if (row.shell().equals(confirming)) {
            return Component.translatable("screen.void_reclaimer.uplink.confirm_recover");
        }
        Component how = row.ready()
                ? Component.translatable("screen.void_reclaimer.uplink.signal", Math.round(row.strength() * 100f))
                : Component.translatable("screen.void_reclaimer.uplink." + row.status());
        return Component.empty().append(where).append(": ").append(how);
    }

    private void pick(ShellRow row) {
        if (row.recoverable() && !row.shell().equals(confirming)) {
            confirming = row.shell();
            init();
            return;
        }
        PacketDistributor.sendToServer(row.recoverable() ? new RecoverShellPayload(seat, row.shell()) : new PickShellPayload(seat, row.shell()));
        onClose();
    }

    private void nextPage() {
        page = (page + 1) * ROWS_ON_A_PAGE < rows.size() ? page + 1 : 0;
        init();
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(seat.getCenter()) > SEAT_DISTANCE_SQR) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + panelHeight, PANEL);
        graphics.renderOutline(left, top, WIDTH, panelHeight, BORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + 10, top + 8, TEXT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
