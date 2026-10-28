package com.voidreclaimer.client.screen;

import com.voidreclaimer.replicator.Milestone;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** "New log entry" toast shown when a milestone is reached. */
public final class MilestoneToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_MS = 6000L;

    private final Milestone milestone;

    public MilestoneToast(Milestone milestone) {
        this.milestone = milestone;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        graphics.drawString(toasts.getMinecraft().font, Component.translatable("toast.void_reclaimer.log"), 8, 7, 0xFF7FD4FF, false);
        graphics.drawString(toasts.getMinecraft().font, milestone.logTitle(), 8, 18, 0xFFFFFFFF, false);
        return timeSinceLastVisible >= DISPLAY_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
