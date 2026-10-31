package com.voidreclaimer.client.debug;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.debug.GuiLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * GUI layout debug, toggled with F6 on any screen: every button and click region outlined with its size, anything that overlaps in
 * red, and the mouse's GUI position. The same list goes to {@code debug/gui.txt} in the game folder each time a screen opens or the
 * toggle turns on, so a layout can be read rather than judged from a picture.
 */
@EventBusSubscriber(modid = VoidReclaimer.MOD_ID, value = Dist.CLIENT)
public final class GuiDebug {
    private static final int OUTLINE = 0xFF50E070;
    private static final int OVERLAP = 0xFFFF3030;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int BACK = 0xC0000000;

    /** A screen with click regions that are not widgets, such as the Body tab's tree. */
    public interface Regions {
        List<GuiLayout.Rect> debugRegions();
    }

    private static boolean on;

    private GuiDebug() {}

    private static List<GuiLayout.Rect> rects(Screen screen) {
        List<GuiLayout.Rect> rects = new ArrayList<>();
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible) {
                rects.add(new GuiLayout.Rect(widget.getMessage().getString(), widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
            }
        }
        if (screen instanceof Regions regions) {
            rects.addAll(regions.debugRegions());
        }
        return rects;
    }

    private static void dump(Screen screen) {
        List<GuiLayout.Rect> rects = rects(screen);
        List<String> lines = new ArrayList<>();
        lines.add(screen.getClass().getSimpleName() + " in a " + screen.width + " x " + screen.height + " GUI");
        rects.forEach(r -> lines.add(String.format(Locale.ROOT, "  %4d %4d  %3d x %3d  %s", r.x(), r.y(), r.width(), r.height(), r.name())));
        List<GuiLayout.Overlap> overlaps = GuiLayout.overlaps(rects);
        lines.add(overlaps.isEmpty() ? "No overlaps" : overlaps.size() + " overlaps:");
        overlaps.forEach(o -> lines.add("  " + o.first().name() + " and " + o.second().name()));
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("debug").resolve("gui.txt");
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            VoidReclaimer.LOGGER.warn("Could not write {}: {}", path, e.toString());
        }
    }

    @SubscribeEvent
    static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() == GLFW.GLFW_KEY_F6) {
            on = !on;
            if (on) {
                dump(event.getScreen());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (on) {
            dump(event.getScreen());
        }
    }

    @SubscribeEvent
    static void onRender(ScreenEvent.Render.Post event) {
        if (!on) {
            return;
        }
        Screen screen = event.getScreen();
        GuiGraphics graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        List<GuiLayout.Rect> rects = rects(screen);
        List<GuiLayout.Overlap> overlaps = GuiLayout.overlaps(rects);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);
        for (GuiLayout.Rect rect : rects) {
            boolean clash = overlaps.stream().anyMatch(o -> o.first().equals(rect) || o.second().equals(rect));
            graphics.renderOutline(rect.x(), rect.y(), rect.width(), rect.height(), clash ? OVERLAP : OUTLINE);
        }
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        StringBuilder line = new StringBuilder(screen.getClass().getSimpleName() + "  " + screen.width + " x " + screen.height + "  mouse " + mouseX + " " + mouseY);
        rects.stream().filter(r -> r.contains(mouseX, mouseY)).forEach(r -> line.append("  [").append(r.name()).append(' ').append(r.width()).append('x').append(r.height()).append(']'));
        if (!overlaps.isEmpty()) {
            line.append("  ").append(overlaps.size()).append(" OVERLAPS");
        }
        String text = line.toString();
        graphics.fill(0, 0, font.width(text) + 4, 11, BACK);
        graphics.drawString(font, text, 2, 2, overlaps.isEmpty() ? TEXT : OVERLAP, false);
        graphics.pose().popPose();
    }
}
