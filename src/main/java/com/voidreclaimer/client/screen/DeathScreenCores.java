package com.voidreclaimer.client.screen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreList;
import com.voidreclaimer.network.RespawnChoicePayload;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds a "Respawn at" list to the death screen: the free deep space spawn plus every Backup Core with a charge.
 * The choice is sent to the server, which uses it when the vanilla Respawn button is pressed.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class DeathScreenCores {
    private static final int X = 10;
    /** Below the dust meter, which stays drawn behind the death screen. */
    private static final int TOP = 76;
    private static final int BUTTON_WIDTH = 160;
    private static final int ROW = 22;
    private static final int TEXT = 0xCFE8FF;

    private static Optional<GlobalPos> selected = Optional.empty();
    private static final List<Choice> choices = new ArrayList<>();

    private DeathScreenCores() {}

    private record Choice(Button button, Optional<GlobalPos> pos, Component label) {}

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen)) {
            return;
        }
        choices.clear();
        selected = Optional.empty();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        List<BackupCoreList.Entry> cores = mc.player.getData(ModAttachments.BACKUP_CORES).cores().stream()
                .filter(entry -> entry.charges() > 0).toList();
        if (cores.isEmpty()) {
            return;
        }
        int y = TOP;
        add(event, Optional.empty(), Component.translatable("screen.void_reclaimer.respawn.spawn"), y);
        for (BackupCoreList.Entry entry : cores) {
            y += ROW;
            add(event, Optional.of(entry.pos()), Component.translatable("screen.void_reclaimer.respawn.core", entry.name(), entry.charges()), y);
        }
        select(Optional.empty());
    }

    private static void add(ScreenEvent.Init.Post event, Optional<GlobalPos> pos, Component label, int y) {
        Button button = Button.builder(label, b -> select(pos)).bounds(X, y, BUTTON_WIDTH, 20).build();
        event.addListener(button);
        choices.add(new Choice(button, pos, label));
    }

    private static void select(Optional<GlobalPos> pos) {
        selected = pos;
        for (Choice choice : choices) {
            boolean chosen = choice.pos().equals(pos);
            choice.button().setMessage(chosen ? Component.literal("> ").append(choice.label()).append(" <") : choice.label());
        }
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new RespawnChoicePayload(pos));
        }
    }

    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof DeathScreen && !choices.isEmpty()) {
            event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                    Component.translatable("screen.void_reclaimer.respawn.title"), X, TOP - 12, TEXT);
        }
    }
}
