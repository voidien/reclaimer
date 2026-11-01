package com.voidreclaimer.earth;

import com.voidreclaimer.registry.ModDataComponents;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Planetary Grid: held, it shows the planet's 4 x 4 sectors. The sector map is an attachment on the Earth level and only
 * reaches clients on Earth, so the item carries its own copy in the {@code planet_sectors} component, refreshed once a
 * second while it is in an inventory: it reads the same from deep space, with no packet of its own.
 */
public class PlanetaryGridItem extends Item {
    private static final int PERIOD = 20;

    public PlanetaryGridItem(Properties properties) {
        super(properties.stacksTo(1).component(ModDataComponents.PLANET_SECTORS.get(), 0));
    }

    /** The map {@code stack} carries; none restored on a stack without one. */
    public static SectorMap map(ItemStack stack) {
        return new SectorMap(stack.getOrDefault(ModDataComponents.PLANET_SECTORS.get(), 0));
    }

    /** Copies the planet's map onto {@code stack} when it differs. */
    public static void refresh(ItemStack stack, MinecraftServer server) {
        int restored = EarthRestoration.map(server).restored();
        if (stack.getOrDefault(ModDataComponents.PLANET_SECTORS.get(), 0) != restored) {
            stack.set(ModDataComponents.PLANET_SECTORS.get(), restored);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity.tickCount % PERIOD == 0) {
            refresh(stack, level.getServer());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("hud.void_reclaimer.planetary_grid", map(stack).count()));
    }
}
