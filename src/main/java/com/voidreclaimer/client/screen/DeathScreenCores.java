package com.voidreclaimer.client.screen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.core.BackupCoreList;
import com.voidreclaimer.link.ShellRow;
import com.voidreclaimer.network.RespawnChoicePayload;
import com.voidreclaimer.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import com.voidreclaimer.core.CoreCharging;
import com.voidreclaimer.core.RespawnCards;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Adds a "Restore at" panel to the death screen: a card for the free deep space spawn, one for every Backup Core with a charge (name,
 * charge pips, distance from where you died) and one for every Remote Shell, plus a short line from the AI under the score. The choice
 * is sent to the server, which uses it when the vanilla Respawn button is pressed. Cards that don't fit the screen are left out.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class DeathScreenCores {
    private static final int X = 10;
    /** Below the dust meter, which stays drawn behind the death screen. */
    private static final int TOP = 90;
    private static final int CARD_WIDTH = 168;
    private static final int CARD_HEIGHT = 28;
    private static final int GAP = 4;
    private static final int TEXT = 0xFFE6F2FF;
    private static final int DIM = 0xFF8899AA;
    private static final int ACCENT = 0xFF7FD4FF;

    private static Choice selected = Choice.SPAWN;
    private static final List<Card> cards = new ArrayList<>();
    /** The shells the server said can be woken in at this death. */
    private static List<ShellRow> shells = List.of();

    private DeathScreenCores() {}

    /** Installed as {@link com.voidreclaimer.network.DeathShellsPayload#clientHandler}. */
    public static void onShells(com.voidreclaimer.network.DeathShellsPayload payload) {
        shells = payload.rows();
    }

    /** Where to go: the spawn, with neither, a Backup Core or a shell. */
    private record Choice(Optional<GlobalPos> core, Optional<UUID> shell) {
        static final Choice SPAWN = new Choice(Optional.empty(), Optional.empty());
    }

    /** One choice. */
    private static final class Card extends AbstractWidget {
        private final Choice choice;
        private final Component name;
        private final Component detail;
        private final Runnable onChoose;

        Card(int x, int y, Choice choice, Component name, Component detail, Runnable onChoose) {
            super(x, y, CARD_WIDTH, CARD_HEIGHT, name);
            this.choice = choice;
            this.name = name;
            this.detail = detail;
            this.onChoose = onChoose;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean chosen = choice.equals(selected);
            int back = chosen ? 0xF0233B4D : isHoveredOrFocused() ? 0xF01C2833 : 0xE0121820;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, back);
            graphics.renderOutline(getX(), getY(), width, height, chosen ? ACCENT : 0xFF3A4A5A);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, name, getX() + 6, getY() + 5, chosen ? TEXT : 0xFFCFD8E0, false);
            graphics.drawString(font, detail, getX() + 6, getY() + 16, DIM, false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onChoose.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen)) {
            return;
        }
        cards.clear();
        selected = Choice.SPAWN;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        List<BackupCoreList.Entry> cores = mc.player.getData(ModAttachments.BACKUP_CORES).cores().stream()
                .filter(entry -> entry.charges() > 0).toList();
        if (cores.isEmpty() && shells.isEmpty()) {
            return;
        }
        int y = TOP;
        add(event, Choice.SPAWN, Component.translatable("screen.void_reclaimer.respawn.spawn"),
                Component.translatable("screen.void_reclaimer.respawn.spawn.detail"), y);
        for (BackupCoreList.Entry entry : cores) {
            y += CARD_HEIGHT + GAP;
            Component detail = Component.literal(RespawnCards.pips(entry.charges(), CoreCharging.MAX_CHARGES) + "  ");
            if (entry.pos().dimension().equals(mc.player.level().dimension())) {
                var p = entry.pos().pos();
                detail = detail.copy().append(Component.translatable("screen.void_reclaimer.respawn.distance",
                        RespawnCards.blocks(p.getX() + 0.5 - mc.player.getX(), p.getY() + 0.5 - mc.player.getY(), p.getZ() + 0.5 - mc.player.getZ())));
            }
            add(event, new Choice(Optional.of(entry.pos()), Optional.empty()), Component.literal(entry.name()), detail, y);
        }
        for (ShellRow row : shells) {
            y += CARD_HEIGHT + GAP;
            Component detail = row.sameDimension() ? Component.translatable("screen.void_reclaimer.respawn.distance", Math.round(row.distance()))
                    : Component.translatable("screen.void_reclaimer.respawn.shell.away");
            add(event, new Choice(Optional.empty(), Optional.of(row.shell())), Component.translatable("screen.void_reclaimer.respawn.shell", row.number()), detail, y);
        }
        select(Choice.SPAWN);
    }

    private static void add(ScreenEvent.Init.Post event, Choice choice, Component name, Component detail, int y) {
        if (y + CARD_HEIGHT > event.getScreen().height - GAP) {
            return;
        }
        Card card = new Card(X, y, choice, name, detail, () -> select(choice));
        event.addListener(card);
        cards.add(card);
    }

    private static void select(Choice choice) {
        selected = choice;
        if (Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(new RespawnChoicePayload(choice.core(), choice.shell()));
        }
    }

    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DeathScreen)) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        GuiGraphics graphics = event.getGuiGraphics();
        // A short line from the AI under the score, whether or not a core can be chosen.
        graphics.drawCenteredString(font, Component.translatable("screen.void_reclaimer.respawn.ai_line"), event.getScreen().width / 2, 116, DIM);
        if (!cards.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.void_reclaimer.respawn.title"), X, TOP - 12, ACCENT);
        }
    }
}
