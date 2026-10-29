package com.voidreclaimer.body.part;

import com.voidreclaimer.api.body.ModuleStats;
import com.voidreclaimer.body.Parts;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A body module (core, thruster, storage, sensor, tool, cosmetic). Does nothing in the hand: mount it on a section in
 * the Body tab of the Replicator. Its stats live in the {@code module} component, shown in the tooltip along with an
 * optional description ({@code item.void_reclaimer.<name>.tooltip}).
 */
public class ModuleItem extends Item {
    public ModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SectionItem.description(this, tooltip);
        ModuleStats stats = Parts.module(stack);
        if (stats == null) {
            return;
        }
        tooltip.add(Component.translatable("tooltip.void_reclaimer.module", stats.kind().displayName(), stats.tier(), fmt(stats.mass()))
                .withStyle(ChatFormatting.DARK_AQUA));
        stats.core().ifPresent(c -> tooltip.add(Component.translatable("tooltip.void_reclaimer.module.core", fmt(c.energyCapacity()),
                fmt(c.solarRate())).withStyle(ChatFormatting.GRAY)));
        stats.thruster().ifPresent(t -> tooltip.add(Component.translatable("tooltip.void_reclaimer.module.thruster",
                String.format(Locale.ROOT, "%.3f", t.thrust()), fmt(t.topSpeed())).withStyle(ChatFormatting.GRAY)));
        stats.storage().ifPresent(s -> tooltip.add(Component.translatable("tooltip.void_reclaimer.module.storage", s.rows(),
                fmt(s.dustCapacity())).withStyle(ChatFormatting.GRAY)));
        stats.minSize().ifPresent(size -> tooltip.add(Component.translatable("tooltip.void_reclaimer.module.min_size",
                Component.translatable("body." + size.location().getNamespace() + ".size." + size.location().getPath()))
                .withStyle(ChatFormatting.GOLD)));
    }

    static String fmt(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
