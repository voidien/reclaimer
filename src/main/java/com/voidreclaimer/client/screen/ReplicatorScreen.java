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
public final class ReplicatorScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 244;
    private static final int LIST_WIDTH = 190;
    private static final int ROW = 22;
    /** The description box under the recipe list. */
    private static final int DETAIL_TOP = 38 + 6 * 22 + 4;
    private static final int DETAIL_LINES = 4;
    private static final int VISIBLE_ROWS = 6;
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
    private int left;
    private int top;
    private Button replicateButton;
    private Button collectButton;

    public ReplicatorScreen() {
        super(Component.translatable("replicator.void_reclaimer.title"));
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
        bodyPanel = new BodyPanel(font, left + 6, top + 38, WIDTH - 12);
        replicateButton = addRenderableWidget(Button.builder(Component.translatable("replicator.void_reclaimer.replicate"), b -> replicate())
                .bounds(left + 6, top + HEIGHT - 24, 90, 18).build());
        collectButton = addRenderableWidget(Button.builder(Component.translatable("replicator.void_reclaimer.collect"),
                        b -> PacketDistributor.sendToServer(ReplicatorActionPayload.collect()))
                .bounds(left + LIST_WIDTH + 14, top + HEIGHT - 24, WIDTH - LIST_WIDTH - 20, 18).build());
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
        replicateButton.active = recipeTab && selected != null && data.isUnlocked(selected.value().milestone())
                && !data.queueFull() && selected.value().cost().canAfford(player.getData(ModAttachments.DUST));
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
                graphics.drawString(font, Milestones.hint(minecraft.level.registryAccess(), recipe.milestone()), x + 24, rowY + 7, DIM, false);
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
            graphics.drawString(font, Milestones.hint(minecraft.level.registryAccess(), selected.value().milestone()), x, y, DIM, false);
            return;
        }
        Component text = description(selected);
        if (text == null) {
            return;
        }
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, LIST_WIDTH - 12);
        for (int i = 0; i < Math.min(lines.size(), DETAIL_LINES); i++) {
            graphics.drawString(font, lines.get(i), x, y + i * 9, TEXT, false);
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
            cx += width + 5;
        }
        graphics.drawString(font, Component.literal(String.format(java.util.Locale.ROOT, "· %ds", Mth.ceil(recipe.ticks() / 20f))).withColor(0x8899AA),
                cx, y + 1, TEXT, false);
        return hover;
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
        for (int i = 0; i < data.jobs().size(); i++) {
            ReplicatorJob job = data.jobs().get(i);
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
        int bufferY = top + HEIGHT - 28 - 46;
        graphics.drawString(font, Component.translatable("replicator.void_reclaimer.output"), x, bufferY, ACCENT, false);
        List<ItemStack> buffer = data.buffer();
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
            return bodyPanel.mouseClicked(player, mouseX, mouseY);
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
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && tab == bodyTab) {
            return bodyPanel.mouseScrolled(player, scrollY);
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
