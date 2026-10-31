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
import com.voidreclaimer.body.Wear;
import com.voidreclaimer.replicator.DustCost;
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
    static final int TREE_ROWS = 10;
    private static final int MIN_TREE_ROWS = 3;
    /** Room under the tree for its summary and page lines. */
    private static final int FOOTER = 24;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int DIM = 0xFF7A8794;
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int WARN = 0xFFFF6060;
    private static final int[] ACCENTS = {0x7FD4FF, 0xF2C14E, 0xFFB347, 0x7CFC8A, 0x5EEAD4, 0xC792EA, 0xFF6B6B, 0xFFFFFF};

    private final Font font;
    private final int x;
    private final int y;
    private final Layout layout;
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

    /**
     * Where the two columns go: the tree of {@code treeWidth} and {@code treeRows} at the panel's x, the details at
     * {@code rightX}, {@code rightWidth} wide. The assembly view leaves its model between them.
     */
    record Layout(int treeWidth, int treeRows, int rightX, int rightWidth, int bottom) {
        /** The Body tab: tree, then the details right beside it. */
        static Layout beside(int x, int width) {
            return new Layout(TREE_WIDTH, TREE_ROWS, x + TREE_WIDTH + 8, width - TREE_WIDTH - 8, Integer.MAX_VALUE);
        }
    }

    BodyPanel(Font font, int x, int y, int width) {
        this(font, x, y, Layout.beside(x, width));
    }

    BodyPanel(Font font, int x, int y, Layout layout) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.layout = layout;
    }

    private int rightX() {
        return layout.rightX();
    }

    private int rightWidth() {
        return layout.rightWidth();
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
            renderCosmetics(graphics, player, view.data().cosmetics(), view.stats().overloaded(), mouseX, mouseY);
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
            if (fx + w > x + layout.treeWidth()) {
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
        // A body with many areas has more filter rows, so the tree gets fewer: its footer must stay on screen.
        int visible = layout.bottom() == Integer.MAX_VALUE ? layout.treeRows()
                : Math.min(layout.treeRows(), Math.max(MIN_TREE_ROWS, (layout.bottom() - top - FOOTER) / TREE_ROW));
        List<Row> rows = rows(view);
        scroll = Mth.clamp(scroll, 0, Math.max(0, rows.size() - visible));
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
            Row row = rows.get(i);
            int ry = top + (i - scroll) * TREE_ROW;
            String socketId = row.socket().id();
            boolean selected = socketId.equals(selectedSocket);
            boolean hover = inside(mouseX, mouseY, x, ry, layout.treeWidth(), TREE_ROW);
            if (selected || hover) {
                graphics.fill(x, ry, x + layout.treeWidth(), ry + TREE_ROW, selected ? 0xFF2E4A5E : 0xFF232D38);
            }
            int indent = x + 2 + 8 * Math.min(row.depth(), 4);
            boolean filled = !view.data().section(socketId).isEmpty();
            graphics.fill(indent, ry + 3, indent + 5, ry + 8, filled ? ACCENT : 0xFF3A4A5A);
            if (!filled) {
                graphics.fill(indent + 1, ry + 4, indent + 4, ry + 7, 0xFF141A22);
            }
            Component name = socketName(socketId);
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), x + layout.treeWidth() - indent - 10), indent + 8, ry + 2,
                    filled ? TEXT : DIM, false);
            regions.add(new Region(x, ry, layout.treeWidth(), TREE_ROW, () -> {
                selectedSocket = socketId.equals(selectedSocket) ? null : socketId;
                selectedMount = null;
            }));
        }
        if (rows.size() > visible) {
            // Under the summary line in a tree too narrow to hold both.
            boolean below = layout.treeWidth() < TREE_WIDTH;
            graphics.drawString(font, (scroll + 1) + "-" + Math.min(rows.size(), scroll + visible) + "/" + rows.size(),
                    below ? x + 2 : x + layout.treeWidth() - 40, top + visible * TREE_ROW + (below ? 12 : 2), DIM, false);
        }
        int missing = view.missing().size();
        graphics.drawString(font, missing == 0 ? Component.translatable("body.void_reclaimer.complete")
                        : Component.translatable("body.void_reclaimer.missing", missing),
                x + 2, top + visible * TREE_ROW + 2, missing == 0 ? DIM : 0xFFE0B050, false);
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
        ty += 13;
        if (!section.isEmpty()) {
            ty = renderCondition(graphics, player, socketId, section, tx, ty, mouseX, mouseY);
            ty = renderPaint(graphics, player, view.data().cosmetics(), socketId, tx, ty, mouseX, mouseY);
        }
        ty += 3;

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

    /** "Condition 63%  [Repair: M3 S2]" for a worn section; nothing for a new one. Returns the next line's y. */
    private int renderCondition(GuiGraphics graphics, LocalPlayer player, String socketId, ItemStack section, int tx, int ty,
                                int mouseX, int mouseY) {
        float condition = Wear.condition(section);
        if (condition >= 1f) {
            return ty;
        }
        int color = condition >= Wear.FULL_EFFECT ? 0xFFE0B050 : WARN;
        Component label = Component.translatable("body.void_reclaimer.condition", Math.round(condition * 100));
        graphics.drawString(font, label, tx, ty, color, false);
        DustCost cost = Wear.repairCost(player.level().getRecipeManager(), section);
        boolean affordable = cost.canAfford(player.getData(ModAttachments.DUST));
        Component repair = Component.translatable("body.void_reclaimer.repair", costText(player, cost));
        int rx = tx + font.width(label) + 6;
        boolean over = inside(mouseX, mouseY, rx, ty - 1, font.width(repair), 10);
        graphics.drawString(font, repair, rx, ty, !affordable ? 0xFF5A6570 : over ? ACCENT : TEXT, false);
        regions.add(new Region(rx, ty - 1, font.width(repair), 10, () -> PacketDistributor.sendToServer(BodyActionPayload.repair(socketId))));
        return ty + 11;
    }

    private static String costText(LocalPlayer player, DustCost cost) {
        StringBuilder text = new StringBuilder();
        for (var kind : com.voidreclaimer.dust.DustKinds.ordered(player.registryAccess(), cost.amounts().keySet())) {
            String name = com.voidreclaimer.dust.DustKinds.name(player.registryAccess(), kind).getString();
            text.append(text.isEmpty() ? "" : " ").append(name.isEmpty() ? "?" : name.substring(0, 1)).append(Mth.ceil(cost.get(kind)));
        }
        return text.isEmpty() ? "-" : text.toString();
    }

    /** The section's own casing (cycles through Default and the available casings) and accent. Returns the next y. */
    private int renderPaint(GuiGraphics graphics, LocalPlayer player, Cosmetics cosmetics, String socketId, int tx, int ty,
                            int mouseX, int mouseY) {
        RegistryAccess access = player.registryAccess();
        ReplicatorData replicator = player.getData(ModAttachments.REPLICATOR);
        Cosmetics.SectionPaint paint = cosmetics.paint(socketId);
        Component casingName = paint.casing().isPresent()
                ? Cosmetics.name(access, VoidReclaimerRegistries.CASING, paint.casing().get())
                : Component.translatable("body.void_reclaimer.paint.default");
        Component casing = Component.translatable("body.void_reclaimer.paint", casingName);
        int w = font.width(casing) + 6;
        chip(graphics, tx, ty, w, casing, false, mouseX, mouseY);
        regions.add(new Region(tx, ty, w, 11, () -> {
            // Default -> each available casing in order -> Default.
            List<com.voidreclaimer.registry.OrderedRegistry.Entry<Casing>> casings = Cosmetics.casings(access).stream()
                    .filter(e -> Cosmetics.isAvailable(e.value(), replicator::isUnlocked)).toList();
            int index = paint.casing().map(key -> {
                for (int i = 0; i < casings.size(); i++) {
                    if (casings.get(i).key().equals(key)) {
                        return i;
                    }
                }
                return -1;
            }).orElse(-1);
            java.util.Optional<net.minecraft.resources.ResourceKey<Casing>> next = index + 1 < casings.size()
                    ? java.util.Optional.of(casings.get(index + 1).key()) : java.util.Optional.empty();
            PacketDistributor.sendToServer(BodyActionPayload.cosmetics(cosmetics.withPaint(socketId,
                    new Cosmetics.SectionPaint(next, paint.accent()))));
        }));
        ty += 13;
        int sx = tx;
        // "Default" swatch, then the palette.
        graphics.renderOutline(sx, ty, 9, 9, paint.accent().isEmpty() ? 0xFFFFFFFF : 0xFF3A4A5A);
        graphics.drawString(font, "-", sx + 2, ty, DIM, false);
        regions.add(new Region(sx, ty, 9, 9, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(
                cosmetics.withPaint(socketId, new Cosmetics.SectionPaint(paint.casing(), java.util.Optional.empty()))))));
        sx += 11;
        for (int color : ACCENTS) {
            graphics.fill(sx, ty, sx + 9, ty + 9, 0xFF000000 | color);
            if (paint.accent().isPresent() && paint.accent().get() == color) {
                graphics.renderOutline(sx - 1, ty - 1, 11, 11, 0xFFFFFFFF);
            }
            regions.add(new Region(sx, ty, 9, 9, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(
                    cosmetics.withPaint(socketId, new Cosmetics.SectionPaint(paint.casing(), java.util.Optional.of(color)))))));
            sx += 11;
        }
        return ty + 12;
    }

    /** Save / Apply / Queue missing for the first blueprint in the inventory, if any. */
    private void renderBlueprint(GuiGraphics graphics, LocalPlayer player, int bx, int by, int mouseX, int mouseY) {
        var items = player.getInventory().items;
        int slot = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(com.voidreclaimer.registry.ModItems.BODY_BLUEPRINT.get())) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        int blueprintSlot = slot;
        boolean saved = items.get(slot).has(com.voidreclaimer.registry.ModDataComponents.BLUEPRINT.get());
        graphics.renderItem(items.get(slot), bx, by - 3);
        int x0 = bx + 18;
        for (BodyActionPayload.Action action : List.of(BodyActionPayload.Action.BLUEPRINT_SAVE, BodyActionPayload.Action.BLUEPRINT_APPLY,
                BodyActionPayload.Action.BLUEPRINT_QUEUE)) {
            if (action != BodyActionPayload.Action.BLUEPRINT_SAVE && !saved) {
                continue;
            }
            Component label = Component.translatable("body.void_reclaimer.blueprint." + action.name().substring(10).toLowerCase(Locale.ROOT));
            int w = font.width(label) + 6;
            if (x0 + w > rightX() + rightWidth()) {
                // Long translations wrap under the first chip.
                x0 = bx + 18;
                by += 13;
            }
            int chipY = by;
            chip(graphics, x0, chipY, w, label, false, mouseX, mouseY);
            regions.add(new Region(x0, chipY, w, 11, () -> PacketDistributor.sendToServer(BodyActionPayload.blueprint(action, blueprintSlot))));
            x0 += w + 3;
        }
    }

    private void renderCosmetics(GuiGraphics graphics, LocalPlayer player, Cosmetics cosmetics, boolean overloaded, int mouseX, int mouseY) {
        int cx = rightX() + 2;
        // Under the stats: the overload warning is one line more.
        int cy = y + (overloaded ? 106 : 94);
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
        int trailY = cy;
        int trailWidth = font.width(trail) + 6;
        if (trailX + trailWidth > rightX() + rightWidth()) {
            // Long names: the trail chip wraps to its own line.
            trailX = cx;
            trailY = cy + 13;
        }
        chip(graphics, trailX, trailY, trailWidth, trail, false, mouseX, mouseY);
        regions.add(new Region(trailX, trailY, trailWidth, 11, () -> PacketDistributor.sendToServer(BodyActionPayload.cosmetics(
                cosmetics.withTrail(Cosmetics.next(access, VoidReclaimerRegistries.TRAIL, Trail::order, cosmetics.trail(),
                        replicator::isUnlocked))))));
        int sy = trailY + 14;
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
        renderBlueprint(graphics, player, cx, sy + 16, mouseX, mouseY);
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

    /** Where this panel's clicks go right now, for the GUI layout debug. */
    List<com.voidreclaimer.debug.GuiLayout.Rect> debugRegions() {
        List<com.voidreclaimer.debug.GuiLayout.Rect> rects = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            rects.add(new com.voidreclaimer.debug.GuiLayout.Rect("body " + i, region.x(), region.y(), region.w(), region.h()));
        }
        return rects;
    }

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

    @Nullable
    String selectedSocket() {
        return selectedSocket;
    }

    @Nullable
    BodyData.Mount selectedMount() {
        return selectedMount;
    }

    /** The area filter, or null for every socket. */
    @Nullable
    String area() {
        return area;
    }

    /** Selects a socket, as clicking its row does, and clears a filter that would hide it. */
    void select(String socket, @Nullable BodyData.Mount mount) {
        selectedSocket = socket;
        selectedMount = mount;
        area = null;
    }

    /** Back to the body's stats. */
    void clearSelection() {
        selectedSocket = null;
        selectedMount = null;
    }

    /** The tree's row height, for screens that size the tree to fit. */
    static int treeRowHeight() {
        return TREE_ROW;
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
