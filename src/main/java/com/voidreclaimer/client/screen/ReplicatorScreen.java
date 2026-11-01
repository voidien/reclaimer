package com.voidreclaimer.client.screen;

import com.voidreclaimer.client.hud.DustIcons;
import com.voidreclaimer.client.input.ModKeyMappings;
import com.voidreclaimer.dust.DustData;
import com.voidreclaimer.dust.DustKinds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import com.voidreclaimer.energy.Energy;
import com.voidreclaimer.network.ReplicatorActionPayload;
import com.voidreclaimer.registry.ModAttachments;
import com.voidreclaimer.registry.ModRecipeTypes;
import com.voidreclaimer.api.milestone.Milestone;
import com.voidreclaimer.api.replicator.ReplicatorCategory;
import com.voidreclaimer.registry.OrderedRegistry;
import com.voidreclaimer.replicator.ReplicatorCategories;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.resources.ResourceKey;
import com.voidreclaimer.replicator.ReplicatorData;
import com.voidreclaimer.replicator.ReplicatorJob;
import com.voidreclaimer.replicator.ReplicatorRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The AI's built-in Replicator. Left: recipes of the selected tab (locked ones show as "???" with a milestone hint).
 * Right: the job queue with progress and cancel buttons, and the output buffer. Two more tabs show the AI's log and
 * its body ({@link BodyPanel}).
 * Drawn entirely with {@link GuiGraphics} fills and text (no textures). Doesn't pause the game, so jobs keep
 * running while it is open.
 */
public final class ReplicatorScreen extends Screen implements com.voidreclaimer.client.debug.GuiDebug.Regions {
    private static final int WIDTH = 320;
    private final SidePreview preview = new SidePreview();
    /** At most 240 so the panel fits a 720p window at GUI scale 3 (the owner's usual test size). */
    private static final int HEIGHT = 236;
    private static final int LIST_WIDTH = 190;
    private static final int ROW = 22;
    /** The description box under the recipe list. */
    private static final int DETAIL_TOP = 38 + 5 * 22 + 4;
    private static final int DETAIL_LINES = 5;
    /** A recipe that takes items keeps the bottom of the detail box for them. */
    private static final int INPUT_DETAIL_LINES = 3;
    private static final int INPUT_ROW_TOP = 33;
    private static final int INPUT_WIDTH = 44;
    private static final int COST_GAP = 5;
    private static final int TIGHT_COST_GAP = 2;
    private static final int VISIBLE_ROWS = 5;
    private static final int PANEL = 0xE0101418;
    private static final int PANEL_LIGHT = 0xFF1E2630;
    private static final int ACCENT = 0xFF7FD4FF;
    private static final int TEXT = 0xFFDDE6EE;
    private static final int DIM = 0xFF7A8794;

    /** Tabs: the registered recipe categories, then the log, then the body. Set in {@link #init()}. */
    private List<OrderedRegistry.Entry<ReplicatorCategory>> categories = List.of();
    private int categoryTabs;
    private int logsTab;
    private int bodyTab;
    private int tab;
    private BodyPanel bodyPanel;
    private int scroll;
    @Nullable
    private RecipeHolder<ReplicatorRecipe> selected;
    private int selectedLog;
    private Start start = Start.RECIPES;
    /** Queue position being dragged (never 0, the running job), or -1. */
    private int dragFrom = -1;
    private int left;
    private int top;
    private Button replicateButton;
    private Button collectButton;
    private Button assemblyButton;

    @Override
    public List<com.voidreclaimer.debug.GuiLayout.Rect> debugRegions() {
        return tab == bodyTab ? bodyPanel.debugRegions() : List.of();
    }

    /** Which tab the screen opens on: the first recipe tab, the Logs tab or the Body tab. */
    public enum Start { RECIPES, LOGS, BODY }

    public ReplicatorScreen() {
        this(Start.RECIPES);
    }

    public ReplicatorScreen(Start start) {
        super(Component.translatable("replicator.void_reclaimer.title"));
        this.start = start;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        categories = ReplicatorCategories.all(minecraft.level.registryAccess());
        categoryTabs = categories.size();
        logsTab = categoryTabs;
        bodyTab = categoryTabs + 1;
        // Six tabs fit at full width; datapacks may add more, which then share the row.
        int tabWidth = Math.min(50, (WIDTH - 12) / (bodyTab + 1) - 2);
        int tabX = left + 6;
        for (int i = 0; i <= bodyTab; i++) {
            int index = i;
            Component label = i < categoryTabs ? ReplicatorCategories.title(categories.get(i))
                    : Component.translatable(i == logsTab ? "replicator.void_reclaimer.tab.logs" : "replicator.void_reclaimer.tab.body");
            addRenderableWidget(Button.builder(label, b -> selectTab(index)).bounds(tabX, top + 18, tabWidth, 16).build());
            tabX += tabWidth + 2;
        }
        addRenderableWidget(Button.builder(Component.translatable("menu.void_reclaimer.inventory"),
                        b -> PacketDistributor.sendToServer(com.voidreclaimer.network.OpenInventoryPayload.INSTANCE))
                .bounds(left + WIDTH - 60, top + 2, 56, 14).build());
        if (start == Start.LOGS) {
            tab = logsTab;
        } else if (start == Start.BODY) {
            tab = bodyTab;
        }
        start = Start.RECIPES;
        bodyPanel = new BodyPanel(font, left + 6, top + 38, WIDTH - 12);
        assemblyButton = addRenderableWidget(Button.builder(Component.translatable("replicator.void_reclaimer.assembly"),
                b -> minecraft.setScreen(new AssemblyScreen())).bounds(left + WIDTH - 112, top + 2, 50, 14).build());
        replicateButton = addRenderableWidget(Button.builder(Component.translatable("replicator.void_reclaimer.replicate"), b -> replicate())
                .bounds(left + 6, top + HEIGHT - 24, 90, 18).build());
        collectButton = addRenderableWidget(Button.builder(Component.translatable("replicator.void_reclaimer.collect"),
                        b -> PacketDistributor.sendToServer(ReplicatorActionPayload.collect()))
                .bounds(left + LIST_WIDTH + 14, top + HEIGHT - 24, WIDTH - LIST_WIDTH - 20, 18).build());
    }

    // ---- for JEI (see compat.jei): the screen's bounds and the item under the mouse --------------------------

    /** An item drawn on the screen and where, so a recipe viewer can look it up. */
    public record ItemHit(ItemStack stack, int x, int y, int width, int height) {}

    public int guiLeft() {
        return left;
    }

    public int guiTop() {
        return top;
    }

    public int guiWidth() {
        return WIDTH;
    }

    public int guiHeight() {
        return HEIGHT;
    }

    /** The recipe result, queued job or output item under the mouse, or null. Locked recipes show no item. */
    @Nullable
    public ItemHit itemAt(double mouseX, double mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || tab == bodyTab) {
            return null;
        }
        ReplicatorData data = player.getData(ModAttachments.REPLICATOR);
        if (tab < categoryTabs) {
            List<RecipeHolder<ReplicatorRecipe>> recipes = recipesForTab(data);
            for (int i = scroll; i < Math.min(recipes.size(), scroll + VISIBLE_ROWS); i++) {
                ReplicatorRecipe recipe = recipes.get(i).value();
                int itemX = left + 6 + 3;
                int itemY = top + 38 + (i - scroll) * ROW + 3;
                if (data.isUnlocked(recipe.milestone()) && inside(mouseX, mouseY, itemX, itemY, 16, 16)) {
                    return new ItemHit(recipe.result(), itemX, itemY, 16, 16);
                }
            }
        }
        int queueX = left + LIST_WIDTH + 18;
        if (selected != null && tab < categoryTabs && data.isUnlocked(selected.value().milestone())) {
            int inputY = top + DETAIL_TOP + INPUT_ROW_TOP;
            List<net.neoforged.neoforge.common.crafting.SizedIngredient> inputs = selected.value().inputs();
            for (int i = 0; i < inputs.size(); i++) {
                int inputX = left + 10 + i * INPUT_WIDTH;
                ItemStack[] shown = inputs.get(i).getItems();
                if (shown.length > 0 && inside(mouseX, mouseY, inputX, inputY, 16, 16)) {
                    return new ItemHit(shown[0], inputX, inputY, 16, 16);
                }
            }
        }
        for (int i = 0; i < data.jobs().size(); i++) {
            int itemY = top + 54 + i * 18;
            if (inside(mouseX, mouseY, queueX, itemY, 16, 16)) {
                return new ItemHit(data.jobs().get(i).result(), queueX, itemY, 16, 16);
            }
        }
        int bufferY = top + HEIGHT - 28 - 46;
        for (int i = 0; i < Math.min(data.buffer().size(), ReplicatorData.BUFFER_SLOTS); i++) {
            int slotX = queueX + (i % 5) * 18;
            int slotY = bufferY + 10 + (i / 5) * 18;
            if (inside(mouseX, mouseY, slotX, slotY, 16, 16)) {
                return new ItemHit(data.buffer().get(i), slotX, slotY, 16, 16);
            }
        }
        return null;
    }

    private void selectTab(int index) {
        tab = index;
        scroll = 0;
        selected = null;
    }

    private void replicate() {
        if (selected != null) {
            PacketDistributor.sendToServer(ReplicatorActionPayload.queue(selected.id()));
        }
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            onClose();
            return;
        }
        ReplicatorData data = player.getData(ModAttachments.REPLICATOR);
        boolean recipeTab = tab < categoryTabs;
        replicateButton.visible = recipeTab;
        collectButton.visible = tab != bodyTab;
        assemblyButton.visible = tab == bodyTab;
        replicateButton.active = recipeTab && selected != null && data.isUnlocked(selected.value().milestone())
                && !data.queueFull() && selected.value().cost().canAfford(player.getData(ModAttachments.DUST))
                && com.voidreclaimer.replicator.RecipeInputs.plan(player.getInventory().items, selected.value().inputs()) != null;
        collectButton.active = !data.buffer().isEmpty();
    }

    // ---- rendering ----------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ReplicatorData data = player.getData(ModAttachments.REPLICATOR);
        graphics.drawString(font, title, left + 6, top + 6, ACCENT, false);
        if (!Energy.canPowerSystems(player)) {
            graphics.drawString(font, Component.translatable("replicator.void_reclaimer.paused"), left + 120, top + 6, 0xFFFF6060, false);
        }
        if (tab == bodyTab) {
            bodyPanel.render(graphics, player, mouseX, mouseY);
            preview.render(graphics, player, left, top, top + HEIGHT);
            return;
        }
        if (tab < categoryTabs) {
            renderRecipes(graphics, player, data, mouseX, mouseY);
        } else {
            renderLogs(graphics, data);
        }
        renderQueue(graphics, data, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF3A4A5A);
        if (tab == bodyTab) {
            graphics.fill(left + 6 + BodyPanel.TREE_WIDTH + 4, top + 38, left + WIDTH - 6, top + HEIGHT - 6, PANEL_LIGHT);
            return;
        }
        graphics.fill(left + 6, top + 38, left + LIST_WIDTH + 6, top + 38 + VISIBLE_ROWS * ROW, PANEL_LIGHT);
        graphics.fill(left + 6, top + DETAIL_TOP, left + LIST_WIDTH + 6, top + HEIGHT - 28, PANEL_LIGHT);
        graphics.fill(left + LIST_WIDTH + 14, top + 38, left + WIDTH - 6, top + HEIGHT - 28, PANEL_LIGHT);
    }

    private void renderRecipes(GuiGraphics graphics, LocalPlayer player, ReplicatorData data, int mouseX, int mouseY) {
        List<RecipeHolder<ReplicatorRecipe>> recipes = recipesForTab(data);
        DustData dust = player.getData(ModAttachments.DUST);
        int x = left + 6;
        int y = top + 38;
        if (recipes.isEmpty()) {
            graphics.drawString(font, Component.translatable("replicator.void_reclaimer.empty_tab"), x + 6, y + 6, DIM, false);
        }
        List<Component> costHover = null;
        Component rowHover = null;
        for (int i = scroll; i < Math.min(recipes.size(), scroll + VISIBLE_ROWS); i++) {
            RecipeHolder<ReplicatorRecipe> holder = recipes.get(i);
            ReplicatorRecipe recipe = holder.value();
            int rowY = y + (i - scroll) * ROW;
            boolean unlocked = data.isUnlocked(recipe.milestone());
            if (holder.equals(selected)) {
                graphics.fill(x, rowY, x + LIST_WIDTH, rowY + ROW, 0xFF2E4A5E);
            } else if (inside(mouseX, mouseY, x, rowY, LIST_WIDTH, ROW)) {
                graphics.fill(x, rowY, x + LIST_WIDTH, rowY + ROW, 0xFF26323E);
            }
            if (unlocked) {
                if (inside(mouseX, mouseY, x, rowY, LIST_WIDTH, ROW)) {
                    rowHover = description(holder);
                }
                graphics.renderItem(recipe.result(), x + 3, rowY + 3);
                graphics.renderItemDecorations(font, recipe.result(), x + 3, rowY + 3);
                Component name = recipe.results().size() > 1
                        ? recipe.displayName().copy().append(Component.literal(" +" + (recipe.results().size() - 1)).withColor(DIM))
                        : recipe.displayName();
                graphics.drawString(font, name, x + 24, rowY + 2, TEXT, false);
                List<Component> hover = drawCost(graphics, recipe, dust, x + 24, rowY + 12, mouseX, mouseY);
                if (hover != null) {
                    costHover = hover;
                }
            } else {
                graphics.drawString(font, "???", x + 11 - font.width("???") / 2, rowY + 7, DIM, false);
                // Two lines at most, where a name and its cost go; a longer hint is read in full in the box under the list.
                List<net.minecraft.util.FormattedCharSequence> hint = font.split(Milestones.hint(minecraft.level.registryAccess(), recipe.milestone()), LIST_WIDTH - 28);
                for (int line = 0; line < Math.min(hint.size(), 2); line++) {
                    graphics.drawString(font, hint.get(line), x + 24, (hint.size() > 1 ? rowY + 2 : rowY + 7) + line * 10, DIM, false);
                }
            }
        }
        renderDetail(graphics);
        if (costHover != null) {
            graphics.renderComponentTooltip(font, costHover, mouseX, mouseY);
        } else if (rowHover != null) {
            graphics.renderTooltip(font, font.split(rowHover, 180), mouseX, mouseY);
        }
        if (recipes.size() > VISIBLE_ROWS) {
            graphics.drawString(font, (scroll + 1) + "-" + Math.min(recipes.size(), scroll + VISIBLE_ROWS) + "/" + recipes.size(),
                    left + 104, top + HEIGHT - 19, DIM, false);
        }
    }

    /** A recipe's description: its own text, else the built-in line for it, else null. */
    @Nullable
    private Component description(RecipeHolder<ReplicatorRecipe> holder) {
        if (holder.value().description().isPresent()) {
            return holder.value().description().get();
        }
        String key = ReplicatorRecipe.descriptionKey(holder.id());
        return net.minecraft.client.resources.language.I18n.exists(key) ? Component.translatable(key) : null;
    }

    /** The selected recipe's name and description in the box under the list. */
    private void renderDetail(GuiGraphics graphics) {
        int x = left + 10;
        int y = top + DETAIL_TOP + 3;
        if (selected == null) {
            graphics.drawString(font, Component.translatable("replicator.void_reclaimer.select_hint"), x, y, DIM, false);
            return;
        }
        ReplicatorData data = minecraft.player.getData(ModAttachments.REPLICATOR);
        if (!data.isUnlocked(selected.value().milestone())) {
            List<net.minecraft.util.FormattedCharSequence> hint = font.split(Milestones.hint(minecraft.level.registryAccess(), selected.value().milestone()), LIST_WIDTH - 12);
            for (int i = 0; i < Math.min(hint.size(), DETAIL_LINES); i++) {
                graphics.drawString(font, hint.get(i), x, y + i * 9, DIM, false);
            }
            return;
        }
        List<net.neoforged.neoforge.common.crafting.SizedIngredient> inputs = selected.value().inputs();
        Component text = description(selected);
        if (text != null) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, LIST_WIDTH - 12);
            int room = inputs.isEmpty() ? DETAIL_LINES : INPUT_DETAIL_LINES;
            for (int i = 0; i < Math.min(lines.size(), room); i++) {
                graphics.drawString(font, lines.get(i), x, y + i * 9, TEXT, false);
            }
        }
        // What the job takes from the inventory, with how many are held: red when short.
        int inputY = top + DETAIL_TOP + INPUT_ROW_TOP;
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack[] shown = inputs.get(i).getItems();
            if (shown.length == 0) {
                continue;
            }
            int inputX = left + 10 + i * INPUT_WIDTH;
            int needed = inputs.get(i).count();
            int held = com.voidreclaimer.replicator.RecipeInputs.held(minecraft.player.getInventory().items, inputs.get(i));
            graphics.renderItem(shown[0], inputX, inputY);
            graphics.drawString(font, held + "/" + needed, inputX + 18, inputY + 4, held >= needed ? TEXT : 0xFF5050, false);
        }
    }

    /**
     * Draws a recipe's price as an icon and an amount per dust kind, red when not affordable, then the time ("[icon]10 [icon]15 · 20s").
     * Returns the tooltip (name, held and needed) for the icon under the mouse, or null.
     */
    @Nullable
    private List<Component> drawCost(GuiGraphics graphics, ReplicatorRecipe recipe, DustData dust, int x, int y, int mouseX, int mouseY) {
        var access = minecraft.level.registryAccess();
        List<Component> hover = null;
        int cx = x;
        // A big price tightens up to stay in its row.
        int wide = font.width(costTime(recipe, true));
        for (ResourceLocation kind : recipe.cost().amounts().keySet()) {
            wide += DustIcons.SIZE + 1 + font.width(Integer.toString(Mth.ceil(recipe.cost().get(kind)))) + COST_GAP;
        }
        boolean tight = wide > LIST_WIDTH - 28;
        for (ResourceLocation kind : DustKinds.ordered(access, recipe.cost().amounts().keySet())) {
            float amount = recipe.cost().get(kind);
            boolean affordable = dust.get(kind) + 1.0E-4f >= amount;
            String number = Integer.toString(Mth.ceil(amount));
            int width = DustIcons.SIZE + 1 + font.width(number);
            DustIcons.draw(graphics, access, kind, cx, y, 1f);
            graphics.drawString(font, number, cx + DustIcons.SIZE + 1, y + 1, affordable ? TEXT : 0xFF5050, false);
            if (inside(mouseX, mouseY, cx, y - 1, width, DustIcons.SIZE + 2)) {
                hover = List.of(Component.translatable("replicator.void_reclaimer.cost_tooltip", DustKinds.name(access, kind),
                        Mth.floor(dust.get(kind)), Mth.ceil(amount)));
            }
            cx += width + (tight ? TIGHT_COST_GAP : COST_GAP);
        }
        graphics.drawString(font, Component.literal(costTime(recipe, !tight)).withColor(0x8899AA),
                cx, y + 1, TEXT, false);
        return hover;
    }

    private static String costTime(ReplicatorRecipe recipe, boolean dot) {
        return (dot ? "· " : "") + Mth.ceil(recipe.ticks() / 20f) + "s";
    }

    private void renderLogs(GuiGraphics graphics, ReplicatorData data) {
        int x = left + 6;
        int y = top + 38;
        List<ResourceKey<Milestone>> logs = data.unlocked();
        var access = minecraft.level.registryAccess();
        if (logs.isEmpty()) {
            graphics.drawString(font, Component.translatable("replicator.void_reclaimer.no_logs"), x + 6, y + 6, DIM, false);
            return;
        }
        selectedLog = Mth.clamp(selectedLog, 0, logs.size() - 1);
        for (int i = 0; i < Math.min(logs.size(), VISIBLE_ROWS); i++) {
            int rowY = y + i * 12;
            graphics.drawString(font, Milestones.logTitle(access, logs.get(i)), x + 4, rowY + 3, i == selectedLog ? ACCENT : TEXT, false);
        }
        ResourceKey<Milestone> log = logs.get(selectedLog);
        int textY = y + Math.min(logs.size(), VISIBLE_ROWS) * 12 + 8;
        for (FormattedCharSequence line : font.split(Milestones.logText(access, log), LIST_WIDTH - 8)) {
            graphics.drawString(font, line, x + 4, textY, 0xFFB8C4CE, false);
            textY += 10;
        }
    }

    private void renderQueue(GuiGraphics graphics, ReplicatorData data, int mouseX, int mouseY) {
        int x = left + LIST_WIDTH + 18;
        int y = top + 42;
        int width = WIDTH - LIST_WIDTH - 28;
        graphics.drawString(font, Component.translatable("replicator.void_reclaimer.queue", data.jobs().size(), ReplicatorData.MAX_JOBS), x, y, ACCENT, false);
        y += 12;
        if (dragFrom >= data.jobs().size()) {
            dragFrom = -1;
        }
        int dropAt = dragFrom >= 0 ? dropIndex(mouseY, data.jobs().size()) : -1;
        for (int i = 0; i < data.jobs().size(); i++) {
            ReplicatorJob job = data.jobs().get(i);
            if (i == dragFrom) {
                graphics.fill(x - 2, y - 1, x + width, y + 16, 0x66000000);
            }
            if (dropAt == i && dropAt != dragFrom) {
                int lineY = dropAt > dragFrom ? y + 17 : y - 1;
                graphics.fill(x - 2, lineY, x + width, lineY + 1, ACCENT);
            }
            graphics.renderItem(job.result(), x, y);
            graphics.renderItemDecorations(font, job.result(), x, y);
            int barX = x + 20;
            int barWidth = width - 32;
            graphics.fill(barX, y + 6, barX + barWidth, y + 10, 0xFF0C1014);
            graphics.fill(barX, y + 6, barX + Math.round(barWidth * job.fraction()), y + 10, i == 0 ? ACCENT : 0xFF3F6A80);
            int cancelX = x + width - 10;
            boolean hover = inside(mouseX, mouseY, cancelX, y + 2, 10, 12);
            graphics.drawString(font, "x", cancelX + 2, y + 4, hover ? 0xFFFF6060 : DIM, false);
            y += 18;
        }
        if (dragFrom >= 0 && dragFrom < data.jobs().size()) {
            graphics.renderItem(data.jobs().get(dragFrom).result(), mouseX - 8, mouseY - 8);
        } else if (data.jobs().size() > 2) {
            graphics.drawString(font, Component.translatable("replicator.void_reclaimer.drag_hint"), x + 62, top + 42, DIM, false);
        }
        int bufferY = top + HEIGHT - 28 - 46;
        graphics.drawString(font, Component.translatable("replicator.void_reclaimer.output"), x, bufferY, ACCENT, false);
        List<ItemStack> buffer = data.buffer();
        // A cancel with a full inventory can go past the slots. Collect still takes them all.
        if (buffer.size() > ReplicatorData.BUFFER_SLOTS) {
            int hidden = buffer.size() - ReplicatorData.BUFFER_SLOTS;
            graphics.drawString(font, "+" + hidden, x + font.width(Component.translatable("replicator.void_reclaimer.output")) + 6, bufferY, TEXT, false);
        }
        for (int i = 0; i < ReplicatorData.BUFFER_SLOTS; i++) {
            int slotX = x + (i % 5) * 18;
            int slotY = bufferY + 10 + (i / 5) * 18;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF0C1014);
            if (i < buffer.size()) {
                graphics.renderItem(buffer.get(i), slotX, slotY);
                graphics.renderItemDecorations(font, buffer.get(i), slotX, slotY);
            }
        }
    }

    // ---- input --------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (tab == bodyTab) {
            return preview.press(left, top, top + HEIGHT, mouseX, mouseY) || bodyPanel.mouseClicked(player, mouseX, mouseY);
        }
        ReplicatorData data = player.getData(ModAttachments.REPLICATOR);
        int x = left + 6;
        int y = top + 38;
        if (tab < categoryTabs) {
            List<RecipeHolder<ReplicatorRecipe>> recipes = recipesForTab(data);
            for (int i = scroll; i < Math.min(recipes.size(), scroll + VISIBLE_ROWS); i++) {
                if (inside(mouseX, mouseY, x, y + (i - scroll) * ROW, LIST_WIDTH, ROW)) {
                    selected = recipes.get(i);
                    return true;
                }
            }
        } else {
            for (int i = 0; i < Math.min(data.unlocked().size(), VISIBLE_ROWS); i++) {
                if (inside(mouseX, mouseY, x, y + i * 12, LIST_WIDTH, 12)) {
                    selectedLog = i;
                    return true;
                }
            }
        }
        int queueX = left + LIST_WIDTH + 18;
        int width = WIDTH - LIST_WIDTH - 28;
        int rowY = top + 54;
        for (int i = 0; i < data.jobs().size(); i++) {
            if (inside(mouseX, mouseY, queueX + width - 10, rowY + 2, 10, 12)) {
                PacketDistributor.sendToServer(ReplicatorActionPayload.cancel(i));
                return true;
            }
            rowY += 18;
        }
        // Waiting jobs can be dragged to a new place; the running job (0) stays put.
        rowY = top + 54;
        for (int i = 0; i < data.jobs().size(); i++) {
            if (i >= 1 && inside(mouseX, mouseY, queueX, rowY, width - 12, 16)) {
                dragFrom = i;
                return true;
            }
            rowY += 18;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return preview.drag(dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        preview.release();
        LocalPlayer player = Minecraft.getInstance().player;
        if (dragFrom >= 0 && player != null) {
            int size = player.getData(ModAttachments.REPLICATOR).jobs().size();
            int target = dropIndex(mouseY, size);
            if (target != dragFrom) {
                PacketDistributor.sendToServer(ReplicatorActionPayload.move(dragFrom, target));
            }
            dragFrom = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The queue place under a mouse height, never in front of the running job. */
    private int dropIndex(double mouseY, int size) {
        return Mth.clamp((int) Math.floor((mouseY - (top + 54)) / 18.0), 1, Math.max(1, size - 1));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && tab == bodyTab) {
            return preview.scroll(left, top, top + HEIGHT, mouseX, mouseY, scrollY) || bodyPanel.mouseScrolled(player, scrollY);
        }
        if (player != null && tab < categoryTabs) {
            int max = Math.max(0, recipesForTab(player.getData(ModAttachments.REPLICATOR)).size() - VISIBLE_ROWS);
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.REPLICATOR.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Recipes in the current tab: unlocked ones first (cheapest first), then locked ones. */
    private List<RecipeHolder<ReplicatorRecipe>> recipesForTab(ReplicatorData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || tab >= categoryTabs) {
            return List.of();
        }
        ResourceKey<ReplicatorCategory> category = categories.get(tab).key();
        List<RecipeHolder<ReplicatorRecipe>> list = new ArrayList<>(mc.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.REPLICATOR.get()));
        list.removeIf(h -> !h.value().category().equals(category));
        list.sort(Comparator.<RecipeHolder<ReplicatorRecipe>, Boolean>comparing(h -> !data.isUnlocked(h.value().milestone()))
                .thenComparing(h -> h.value().ticks())
                .thenComparing(h -> h.id().toString()));
        return list;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
