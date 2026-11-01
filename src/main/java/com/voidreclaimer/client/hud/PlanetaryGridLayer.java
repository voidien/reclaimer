package com.voidreclaimer.client.hud;

import com.voidreclaimer.earth.PlanetaryGridItem;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.earth.SectorMap;
import com.voidreclaimer.earth.Sectors;
import com.voidreclaimer.registry.ModDataComponents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Planetary Grid's map, shown while one is in either hand: restored sectors green, the rest ash, and on Earth the
 * player's own sector outlined. Anything carrying the {@code planet_sectors} component shows it.
 */
public final class PlanetaryGridLayer {
    private static final int BACKDROP = 0xB0101418;
    private static final int RUINED = 0xFF5A4A3A;
    private static final int RESTORED = 0xFF4FA34A;
    private static final int HERE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFD8E8D0;
    private static final int PADDING = 3;

    private PlanetaryGridLayer() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.options.hideGui || player == null) {
            return;
        }
        ItemStack held = gridIn(player.getMainHandItem(), player.getOffhandItem());
        if (held == null) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int left = PlanetaryGridLayout.left(width);
        int top = PlanetaryGridLayout.top(height);
        int size = PlanetaryGridLayout.SIZE;
        int textY = top + size + PADDING + 2;
        graphics.fill(left - PADDING, top - PADDING, left + size + PADDING, textY + mc.font.lineHeight + 1, BACKDROP);
        SectorMap map = PlanetaryGridItem.map(held);
        for (int sector = 0; sector < Sectors.COUNT; sector++) {
            int x = PlanetaryGridLayout.cellX(sector, width);
            int y = PlanetaryGridLayout.cellY(sector, height);
            graphics.fill(x, y, x + PlanetaryGridLayout.CELL, y + PlanetaryGridLayout.CELL, map.isRestored(sector) ? RESTORED : RUINED);
        }
        int here = RuinedEarth.is(player.level()) ? Sectors.at(player.getBlockX(), player.getBlockZ()) : Sectors.NONE;
        if (here != Sectors.NONE) {
            graphics.renderOutline(PlanetaryGridLayout.cellX(here, width), PlanetaryGridLayout.cellY(here, height),
                    PlanetaryGridLayout.CELL, PlanetaryGridLayout.CELL, HERE);
        }
        Component count = Component.translatable("hud.void_reclaimer.planetary_grid", map.count());
        graphics.drawCenteredString(mc.font, count, left + size / 2, textY, TEXT);
    }

    private static ItemStack gridIn(ItemStack main, ItemStack off) {
        if (main.has(ModDataComponents.PLANET_SECTORS.get())) {
            return main;
        }
        return off.has(ModDataComponents.PLANET_SECTORS.get()) ? off : null;
    }
}
