package com.voidreclaimer.client.screen;

import com.voidreclaimer.api.VoidReclaimerRegistries;
import com.voidreclaimer.api.body.Casing;
import com.voidreclaimer.api.body.ModuleKind;
import com.voidreclaimer.api.body.SectionStats;
import com.voidreclaimer.api.body.Socket;
import com.voidreclaimer.api.body.Trail;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodyStats;
import com.voidreclaimer.body.BodyView;
import com.voidreclaimer.body.Cosmetics;
import com.voidreclaimer.body.Parts;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.network.BodyActionPayload;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.replicator.ReplicatorData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Replicator's Body tab. Left: area filters, the body's sockets as a tree (indented by joint), and how many are
 * empty. Right: with nothing selected, the body's stats and its looks (casing, trail, accent tint); with a socket
 * selected, its section, the mounts it offers and the matching items from the inventory (click one to install it).
 * Every change goes to the server, which validates it.
 *
 * <p>Clickable areas are recorded while drawing, so clicks always match what is on screen.
 */
final class BodyPanel {
    private static final int SLOT = 16;
    private static final int STEP = 17;
    static final int TREE_WIDTH = 150;
    private static final int TREE_ROW = 11;
    private static final int TREE_ROWS = 10;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int DIM = 0xFF7A8794;
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int WARN = 0xFFFF6060;
    private static final int[] ACCENTS = {0x7FD4FF, 0xF2C14E, 0xFFB347, 0x7CFC8A, 0x5EEAD4, 0xC792EA, 0xFF6B6B, 0xFFFFFF};

    private final Font font;
    private final int x;
    private final int y;
    private final int width;
    /** Area filter; null shows every socket. */
    @Nullable
    private String area;
    @Nullable
    private String selectedSocket;
    @Nullable
    private BodyData.Mount selectedMount;
    private int scroll;
    private final List<Region> regions = new ArrayList<>();

    private record Region(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** A tree row: a socket and its depth in the joint tree. */
    private record Row(Socket socket, int depth) {}

    BodyPanel(Font font, int x, int y, int width) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
    }

    private int rightX() {
        return x + TREE_WIDTH + 8;
    }

    private int rightWidth() {
        return width - TREE_WIDTH - 8;
    }

    // ---- rendering ----------------------------------------------------------------------------------------

    void render(GuiGraphics graphics, LocalPlayer player, int mouseX, int mouseY) {
        regions.clear();
        BodyView view = Bodies.view(player);
        if (selectedSocket != null && view.chassis().socket(selectedSocket) == null) {
            selectedSocket = null;
            selectedMount = null;
        }
        Hover hover = new Hover();
        renderTree(graphics, view, mouseX, mouseY);
        if (selectedSocket == null) {
            renderStats(graphics, player, view.stats());
            renderCosmetics(graphics, player, view.data().cosmetics(), mouseX, mouseY);
        } else {
            renderSocket(graphics, player, view, mouseX, mouseY, hover);
        }
        if (!hover.stack.isEmpty()) {
            List<Component> lines = new ArrayList<>(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), hover.stack));
            if (hover.line != null) {
                lines.add(hover.line);
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private static final class Hover {
        ItemStack stack = ItemStack.EMPTY;
        @Nullable
        Component line;
    }

    private void renderTree(GuiGraphics graphics, BodyView view, int mouseX, int mouseY) {
        // Area filters.
        int fx = x;
        int fy = y;
        Set<String> areas = new LinkedHashSet<>();
        view.chassis().sockets().forEach(s -> areas.add(s.area()));
        List<String> filters = new ArrayList<>();
        filters.add(null);
        filters.addAll(areas);
        for (String filter : filters) {
            Component label = filter == null ? Component.translatable("body.void_reclaimer.area.all") : areaName(filter);
            int w = font.width(label) + 6;
            if (fx + w > x + TREE_WIDTH) {
                fx = x;
                fy += 12;
            }
            boolean active = java.util.Objects.equals(filter, area);
            chip(graphics, fx, fy, w, label, active, mouseX, mouseY);
            regions.add(new Region(fx, fy, w, 11, () -> {
                area = filter;
                scroll = 0;
            }));
            fx += w + 2;
        }
        int top = fy + 14;
        List<Row> rows = rows(view);
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - TREE_ROWS));
        for (int i = scroll; i < Math.min(rows.size(), scroll + TREE_ROWS); i++) {
            Row row = rows.get(i);
            int ry = top + (i - scroll) * TREE_ROW;
            String socketId = row.socket().id();
            boolean selected = socketId.equals(selectedSocket);
            boolean hover = inside(mouseX, mouseY, x, ry, TREE_WIDTH, TREE_ROW);
            if (selected || hover) {
                graphics.fill(x, ry, x + TREE_WIDTH, ry + TREE_ROW, selected ? 0xFF2E4A5E : 0xFF232D38);
            }
            int indent = x + 2 + 8 * Math.min(row.depth(), 4);
            boolean filled = !view.data().section(socketId).isEmpty();
            graphics.fill(indent, ry + 3, indent + 5, ry + 8, filled ? ACCENT : 0xFF3A4A5A);
            if (!filled) {
                graphics.fill(indent + 1, ry + 4, indent + 4, ry + 7, 0xFF141A22);
            }
            Component name = socketName(socketId);
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), x + TREE_WIDTH - indent - 10), indent + 8, ry + 2,
                    filled ? TEXT : DIM, false);
            regions.add(new Region(x, ry, TREE_WIDTH, TREE_ROW, () -> {
                selectedSocket = socketId.equals(selectedSocket) ? null : socketId;
                selectedMount = null;
            }));
        }
        if (rows.size() > TREE_ROWS) {
            graphics.drawString(font, (scroll + 1) + "-" + Math.min(rows.size(), scroll + TREE_ROWS) + "/" + rows.size(),
                    x + TREE_WIDTH - 40, top + TREE_ROWS * TREE_ROW + 2, DIM, false);
        }
        int missing = view.missing().size();
        graphics.drawString(font, missing == 0 ? Component.translatable("body.void_reclaimer.complete")
                        : Component.translatable("body.void_reclaimer.missing", missing),
                x + 2, top + TREE_ROWS * TREE_ROW + 2, missing == 0 ? DIM : 0xFFE0B050, false);
    }

    /** Sockets in joint-tree order (children under their parent), filtered by area. */
    private List<Row> rows(BodyView view) {
        List<Row> rows = new ArrayList<>();
        List<Socket> sockets = view.chassis().sockets();
        for (Socket socket : sockets) {
            if (socket.parent().isEmpty()) {
                addRows(rows, sockets, socket, 0);
            }
        }
        if (area != null) {
            rows.removeIf(r -> !r.socket().area().equals(area));
            rows.replaceAll(r -> new Row(r.socket(), 0));
        }
        return rows;
    }

    private static void addRows(List<Row> rows, List<Socket> sockets, Socket socket, int depth) {
        rows.add(new Row(socket, depth));
        for (Socket child : sockets) {
            if (child.parent().isPresent() && child.parent().get().equals(socket.id())) {
                addRows(rows, sockets, child, depth + 1);
            }
        }
    }

    private void renderStats(GuiGraphics graphics, LocalPlayer player, BodyStats stats) {
        int tx = rightX() + 2;
        int ty = y + 2;
        graphics.drawString(font, Component.translatable("body.void_reclaimer.stats"), tx, ty, ACCENT, false);
        ty += 12;
        Component size = Component.translatable("body." + stats.size().location().getNamespace() + ".size." + stats.size().location().getPath());
        line(graphics, tx, ty, Component.translatable("body.void_reclaimer.stat.size", size, fmt(stats.scale())), TEXT);
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
            ty += 10;
            line(graphics, tx, ty, Component.translatable("body.void_reclaimer.overloaded"), WARN);
        }
    }

    private void renderSocket(GuiGraphics graphics, LocalPlayer player, BodyView view, int mouseX, int mouseY, Hover hover) {
        String socketId = selectedSocket;
        int tx = rightX() + 2;
        int ty = y + 2;
        graphics.drawString(font, socketName(socketId), tx, ty, ACCENT, false);
        ty += 11;
        ItemStack section = view.data().section(socketId);
        if (section.isEmpty()) {
            graphics.drawString(font, Component.translatable("body.void_reclaimer.empty_socket"), tx, ty, DIM, false);
        } else {
            graphics.renderItem(section, tx, ty - 2);
            if (inside(mouseX, mouseY, tx, ty - 2, SLOT, SLOT)) {
                hover.stack = section;
            }
            graphics.drawString(font, font.plainSubstrByWidth(section.getHoverName().getString(), rightWidth() - 60), tx + 18, ty + 2,
                    TEXT, false);
            if (BodyRules.canRemoveSection(view, socketId) == BodyRules.Check.OK) {
                Component remove = Component.translatable("body.void_reclaimer.remove");
                int rx = rightX() + rightWidth() - font.width(remove) - 2;
                boolean over = inside(mouseX, mouseY, rx, ty + 1, font.width(remove), 10);
                graphics.drawString(font, remove, rx, ty + 2, over ? WARN : DIM, false);
                regions.add(new Region(rx, ty + 1, font.width(remove), 10,
                        () -> PacketDistributor.sendToServer(BodyActionPayload.removeSection(socketId))));
            }
        }
        ty += 18;

        // Mounts on the section.
        List<BodyData.Mount> mounts = view.mounts(socketId);
        BodyData.Mount active = view.activeToolMount();
        if (!mounts.isEmpty()) {
            int mx = tx;
            for (BodyData.Mount mount : mounts) {
                boolean selected = mount.equals(selectedMount);
                boolean over = inside(mouseX, mouseY, mx, ty, SLOT, SLOT);
                graphics.fill(mx, ty, mx + SLOT, ty + SLOT, selected ? 0xFF2E4A5E : over ? 0xFF2A3644 : 0xFF1E2630);
                graphics.renderOutline(mx, ty, SLOT, SLOT, mount.equals(active) ? ACCENT : 0xFF3A4A5A);
                ItemStack module = view.data().module(mount);
                if (module.isEmpty()) {
                    graphics.drawCenteredString(font, kindLetter(mount.kind()), mx + 8, ty + 4, 0xFF4A5A6A);
                } else {
                    graphics.renderItem(module, mx, ty);
                }
                if (over) {
                    hover.stack = module;
                    hover.line = mount.kind().displayName().copy().withColor(DIM);
                    if (module.isEmpty()) {
                        hover.stack = ItemStack.EMPTY;
                    }
                }
                regions.add(new Region(mx, ty, SLOT, SLOT, () -> selectedMount = mount.equals(selectedMount) ? null : mount));
                mx += STEP;
                if (mx + SLOT > rightX() + rightWidth()) {
                    mx = tx;
                    ty += STEP;
                }
            }
            ty += STEP + 2;
        }
        if (selectedMount != null && !mounts.contains(selectedMount)) {
            selectedMount = null;
        }
        if (selectedMount != null && !view.data().module(selectedMount).isEmpty()
                && BodyRules.canRemoveModule(view, selectedMount) == BodyRules.Check.OK) {
            BodyData.Mount mount = selectedMount;
            Component remove = Component.translatable("body.void_reclaimer.remove_module");
            boolean over = inside(mouseX, mouseY, tx, ty, font.width(remove), 10);
            graphics.drawString(font, remove, tx, ty, over ? WARN : DIM, false);
            regions.add(new Region(tx, ty, font.width(remove), 10, () -> PacketDistributor.sendToServer(BodyActionPayload.removeModule(mount))));
            ty += 11;
        }

        // What can go there from the inventory.
        graphics.drawString(font, Component.translatable(selectedMount == null ? "body.void_reclaimer.install_section"
                : "body.void_reclaimer.install_module", selectedMount == null ? "" : selectedMount.kind().displayName()), tx, ty, DIM, false);
        ty += 11;
        List<Integer> candidates = selectedMount == null ? sectionCandidates(player, view, socketId) : moduleCandidates(player, selectedMount.kind());
        if (candidates.isEmpty()) {
            for (var seq : font.split(Component.translatable("body.void_reclaimer.no_parts"), rightWidth() - 4)) {
                graphics.drawString(font, seq, tx, ty, DIM, false);
                ty += 10;
            }
            return;
        }
        int perRow = Math.max(1, (rightWidth() - 2) / STEP);
        for (int i = 0; i < candidates.size(); i++) {
            int sx = tx + (i % perRow) * STEP;
            int sy = ty + (i / perRow) * STEP;
            int slot = candidates.get(i);
            ItemStack stack = player.getInventory().items.get(slot);
            BodyData.Mount mount = selectedMount;
            BodyRules.Check check = mount == null ? BodyRules.canInstallSection(view, socketId, stack)
                    : BodyRules.canInstallModule(view, mount, stack);
            boolean over = inside(mouseX, mouseY, sx, sy, SLOT, SLOT);
            graphics.fill(sx, sy, sx + SLOT, sy + SLOT, over ? 0xFF2A3644 : 0xFF1E2630);
            graphics.renderItem(stack, sx, sy);
            if (check != BodyRules.Check.OK) {
                graphics.fill(sx, sy, sx + SLOT, sy + SLOT, 0xA0101418);
            }
            if (over) {
                hover.stack = stack;
                hover.line = check == BodyRules.Check.OK
                        ? Component.translatable("body.void_reclaimer.click_install").withColor(ACCENT)
                        : Component.translatable(BodyRules.messageKey(check)).withColor(WARN);
            }
            regions.add(new Region(sx, sy, SLOT, SLOT, () -> PacketDistributor.sendToServer(mount == null
                    ? BodyActionPayload.installSection(socketId, slot) : BodyActionPayload.installModule(mount, slot))));
        }
    }

    private void renderCosmetics(GuiGraphics graphics, LocalPlayer player, Cosmetics cosmetics, int mouseX, int mouseY) {
        int cx = rightX() + 2;
        int cy = y + 118;
        RegistryAccess access = player.registryAccess();
        ReplicatorData replicator = player.getData(ModAttachments.REPLICATOR);
        Component casing = Component.translatable("body.void_reclaimer.casing", Cosmetics.name(access, VoidReclaimerRegistries.CASING, cosmetics.casing()));
        Component trail = Component.translatable("body.void_reclaimer.trail", Cosmetics.name(access, VoidReclaimerRegistries.TRAIL, cosmetics.trail()));
        int casingWidth = font.width(casing) + 6;
        chip(graphics, cx, cy, casingWidth, casing, false, mouseX, mouseY);
        regions.add(new Region(cx, cy, casingWidth, 11, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(
                cosmetics.withCasing(Cosmetics.next(access, VoidReclaimerRegistries.CASING, Casing::order, cosmetics.casing(),
                        replicator::isUnlocked))))));
        int trailX = cx + casingWidth + 4;
        int trailWidth = font.width(trail) + 6;
        chip(graphics, trailX, cy, trailWidth, trail, false, mouseX, mouseY);
        regions.add(new Region(trailX, cy, trailWidth, 11, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(
                cosmetics.withTrail(Cosmetics.next(access, VoidReclaimerRegistries.TRAIL, Trail::order, cosmetics.trail(),
                        replicator::isUnlocked))))));
        int sy = cy + 14;
        graphics.drawString(font, Component.translatable("body.void_reclaimer.accent"), cx, sy + 1, DIM, false);
        int sx = cx + 40;
        for (int color : ACCENTS) {
            graphics.fill(sx, sy, sx + 10, sy + 10, 0xFF000000 | color);
            if (cosmetics.accent() == color) {
                graphics.renderOutline(sx - 1, sy - 1, 12, 12, 0xFFFFFFFF);
            }
            regions.add(new Region(sx, sy, 10, 10, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(cosmetics.withAccent(color)))));
            sx += 12;
        }
    }

    private void chip(GuiGraphics graphics, int bx, int by, int bw, Component label, boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, bx, by, bw, 11);
        graphics.fill(bx, by, bx + bw, by + 11, active ? 0xFF2E4A5E : hover ? 0xFF2A3644 : 0xFF1E2630);
        graphics.drawString(font, label, bx + 3, by + 2, active ? ACCENT : TEXT, false);
    }

    private void line(GuiGraphics graphics, int tx, int ty, Component text, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), rightWidth() - 4), tx, ty, color, false);
    }

    // ---- input --------------------------------------------------------------------------------------------

    boolean mouseClicked(LocalPlayer player, double mouseX, double mouseY) {
        for (Region region : List.copyOf(regions)) {
            if (region.contains(mouseX, mouseY)) {
                region.action().run();
                return true;
            }
        }
        return false;
    }

    boolean mouseScrolled(LocalPlayer player, double scrollY) {
        scroll -= (int) Math.signum(scrollY);
        return true;
    }

    // ---- helpers ------------------------------------------------------------------------------------------

    /** Inventory slots holding sections that could go in {@code socketId}. */
    private static List<Integer> sectionCandidates(LocalPlayer player, BodyView view, String socketId) {
        Socket socket = view.chassis().socket(socketId);
        boolean core = socketId.equals(view.chassis().coreSocket());
        List<Integer> slots = new ArrayList<>();
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            SectionStats section = Parts.section(items.get(i));
            if (section != null && socket != null && (core ? section.isCore() : socket.accepts(section.socketType()))) {
                slots.add(i);
            }
        }
        return slots;
    }

    /** Inventory slots holding modules of {@code kind}. */
    private static List<Integer> moduleCandidates(LocalPlayer player, ModuleKind kind) {
        List<Integer> slots = new ArrayList<>();
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (Parts.isModule(items.get(i), kind)) {
                slots.add(i);
            }
        }
        return slots;
    }

    /** {@code body.void_reclaimer.socket.<id>}, or the id made readable. */
    static Component socketName(String socketId) {
        String key = "body.void_reclaimer.socket." + socketId;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(readable(socketId));
    }

    private static Component areaName(String area) {
        String key = "body.void_reclaimer.area." + area;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(readable(area));
    }

    private static String readable(String id) {
        String text = id.replace('_', ' ');
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String kindLetter(ModuleKind kind) {
        return kind.getSerializedName().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String fmt(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
